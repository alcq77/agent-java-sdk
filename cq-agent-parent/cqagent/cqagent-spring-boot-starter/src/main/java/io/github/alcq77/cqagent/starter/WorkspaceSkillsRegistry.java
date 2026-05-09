package io.github.alcq77.cqagent.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Workspace 技能目录注册表。
 * <p>
 * 职责边界：
 * - 负责扫描并解析 skills 文件；
 * - 维护内存快照并监听目录变化；
 * - 不负责实际工具调用，由 {@link WorkspaceSkillsTool} 消费快照。
 */
public class WorkspaceSkillsRegistry implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSkillsRegistry.class);

    private final ProductStarterProperties properties;
    private volatile Map<String, SkillEntry> skillSnapshot = Map.of();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread watchThread;
    private WatchService watchService;

    public WorkspaceSkillsRegistry(ProductStarterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        // 启动阶段先做一次快照加载，确保服务可立即读取技能。
        reloadSnapshot();
        if (!properties.getSkills().isEnabled()) {
            return;
        }
        try {
            Path root = Paths.get(properties.getSkills().getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(root);
            watchService = root.getFileSystem().newWatchService();
            register(root);
            registerChildren(root);
            running.set(true);
            watchThread = Thread.ofPlatform().name("product-skills-watch").daemon(true).start(() -> watchLoop(root));
        } catch (Exception e) {
            log.warn("skills watch disabled, message={}", e.getMessage());
        }
    }

    public Map<String, SkillEntry> currentSkills() {
        return skillSnapshot;
    }

    private void watchLoop(Path root) {
        while (running.get()) {
            try {
                WatchKey key = watchService.take();
                boolean changed = false;
                for (WatchEvent<?> ignored : key.pollEvents()) {
                    changed = true;
                }
                key.reset();
                if (changed) {
                    registerChildren(root);
                    reloadSnapshot();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("skills watch loop error: {}", e.getMessage());
            }
        }
    }

    private void reloadSnapshot() {
        if (!properties.getSkills().isEnabled()) {
            skillSnapshot = Map.of();
            return;
        }
        Path root = Paths.get(properties.getSkills().getDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            Map<String, SkillEntry> next = new LinkedHashMap<>();
            Files.list(root)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".md"))
                    .forEach(p -> putSkillFile(next, stripExt(p), p));
            Files.list(root)
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        Path skillMd = dir.resolve("SKILL.md");
                        if (Files.isRegularFile(skillMd)) {
                            putSkillFile(next, dir.getFileName().toString(), skillMd);
                        }
                    });
            skillSnapshot = next;
            log.info("skills loaded count={}", next.size());
        } catch (Exception e) {
            log.warn("skills reload failed, keep previous snapshot, message={}", e.getMessage());
        }
    }

    private static void putSkillFile(Map<String, SkillEntry> map, String key, Path file) {
        try {
            String content = Files.readString(file);
            map.put(key, parseSkill(key, content));
        } catch (Exception ignored) {
        }
    }

    private static SkillEntry parseSkill(String key, String content) {
        int priority = 0;
        List<String> keywords = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("priority:")) {
                String value = trimmed.substring("priority:".length()).trim();
                try {
                    priority = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
            } else if (trimmed.startsWith("keywords:")) {
                String value = trimmed.substring("keywords:".length()).trim();
                for (String k : value.split(",")) {
                    String t = k.trim();
                    if (!t.isEmpty()) {
                        keywords.add(t.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return new SkillEntry(key, content, priority, keywords);
    }

    private void register(Path dir) {
        try {
            dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (Exception ignored) {
        }
    }

    private void registerChildren(Path root) {
        try {
            Files.list(root).filter(Files::isDirectory).forEach(this::register);
        } catch (Exception ignored) {
        }
    }

    private static String stripExt(Path file) {
        String name = file.getFileName().toString();
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    @Override
    public void destroy() {
        // 优雅关闭 watch 线程与 watch service，避免资源泄漏。
        running.set(false);
        if (watchThread != null) {
            watchThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (Exception ignored) {
            }
        }
    }

    public record SkillEntry(String name, String content, int priority, List<String> keywords) {
    }
}
