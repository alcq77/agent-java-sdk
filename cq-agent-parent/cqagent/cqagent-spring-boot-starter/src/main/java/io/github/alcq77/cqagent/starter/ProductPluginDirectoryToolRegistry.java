package io.github.alcq77.cqagent.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alcq77.cqagent.spi.tool.ProductTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProductPluginDirectoryToolRegistry implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ProductPluginDirectoryToolRegistry.class);

    private final ProductStarterProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile List<ProductTool> cachedTools = List.of();
    private final List<URLClassLoader> activeClassLoaders = new CopyOnWriteArrayList<>();
    private Thread watchThread;
    private WatchService watchService;

    public ProductPluginDirectoryToolRegistry(ProductStarterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public void afterPropertiesSet() {
        reloadSafely();
        if (!properties.getPlugin().isEnabled()) {
            return;
        }
        try {
            Path dir = Paths.get(properties.getPlugin().getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            watchService = dir.getFileSystem().newWatchService();
            dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            running.set(true);
            watchThread = Thread.ofPlatform().name("product-plugin-watch").daemon(true).start(() -> watchLoop(dir));
        } catch (Exception e) {
            log.warn("plugin watch disabled, message={}", e.getMessage());
        }
    }

    public List<ProductTool> currentTools() {
        return cachedTools;
    }

    private void watchLoop(Path dir) {
        while (running.get()) {
            try {
                WatchKey key = watchService.take();
                boolean changed = false;
                for (WatchEvent<?> ignored : key.pollEvents()) {
                    changed = true;
                }
                key.reset();
                if (changed) {
                    reloadSafely();
                    log.info("plugin directory reloaded path={}", dir);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("plugin watch loop error: {}", e.getMessage());
            }
        }
    }

    private synchronized void reloadSafely() {
        if (!properties.getPlugin().isEnabled()) {
            cachedTools = List.of();
            closeClassLoaders(activeClassLoaders);
            activeClassLoaders.clear();
            return;
        }
        List<URLClassLoader> nextLoaders = new ArrayList<>();
        try {
            List<ProductTool> nextTools = loadTools(nextLoaders);
            List<URLClassLoader> previous = new ArrayList<>(activeClassLoaders);
            activeClassLoaders.clear();
            activeClassLoaders.addAll(nextLoaders);
            cachedTools = nextTools;
            closeClassLoaders(previous);
            log.info("plugin tools loaded count={}", nextTools.size());
        } catch (Exception e) {
            closeClassLoaders(nextLoaders);
            log.warn("plugin reload failed, keep previous snapshot, message={}", e.getMessage());
        }
    }

    private List<ProductTool> loadTools(List<URLClassLoader> outLoaders) throws Exception {
        Path dir = Paths.get(properties.getPlugin().getDirectory()).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        ProductStarterProperties.Plugin cfg = properties.getPlugin();
        Optional<PluginDirectoryManifest> manifestOpt = loadManifest(dir);
        if (cfg.isVerifySha256() && cfg.isRequireManifest() && manifestOpt.isEmpty()) {
            throw new IllegalStateException("plugin manifest required but missing: " + dir.resolve(cfg.getManifestFile()));
        }
        if (cfg.isVerifySha256() && manifestOpt.isEmpty()) {
            log.warn("verify-sha256 enabled but manifest missing at {}, loading jars without digest check",
                    dir.resolve(cfg.getManifestFile()));
        }
        Map<String, String> artifacts = manifestOpt.map(PluginDirectoryManifest::getArtifacts).orElse(Map.of());

        List<Path> jars = Files.list(dir)
                .filter(p -> p.getFileName().toString().endsWith(".jar"))
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        List<ProductTool> tools = new ArrayList<>();
        for (Path jar : jars) {
            String name = jar.getFileName().toString();
            if (!acceptJar(jar, name, artifacts)) {
                continue;
            }
            URLClassLoader cl = new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader());
            outLoaders.add(cl);
            ServiceLoader.load(ProductTool.class, cl).forEach(tools::add);
        }
        return tools;
    }

    private Optional<PluginDirectoryManifest> loadManifest(Path pluginDir) {
        try {
            Path mf = pluginDir.resolve(properties.getPlugin().getManifestFile());
            if (!Files.exists(mf)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(mf.toFile(), PluginDirectoryManifest.class));
        } catch (Exception ex) {
            log.warn("failed to parse plugin manifest: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 校验清单与摘要；返回 false 表示跳过该 jar。
     */
    private boolean acceptJar(Path jarPath, String fileName, Map<String, String> artifacts) throws Exception {
        ProductStarterProperties.Plugin cfg = properties.getPlugin();
        if (!cfg.isVerifySha256()) {
            return true;
        }
        if (artifacts.isEmpty()) {
            return true;
        }
        String expected = artifacts.get(fileName);
        if (expected == null) {
            if (cfg.isStrictManifest()) {
                log.warn("skip plugin jar not listed in manifest: {}", fileName);
                return false;
            }
            log.debug("plugin jar not in manifest, loading without digest check: {}", fileName);
            return true;
        }
        String actual = sha256Hex(jarPath);
        if (!expected.equalsIgnoreCase(actual)) {
            log.error("plugin jar sha256 mismatch, skip: {} expected={} actual={}", fileName, expected, actual);
            return false;
        }
        log.debug("plugin jar digest ok: {}", fileName);
        return true;
    }

    private static String sha256Hex(Path path) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        try (InputStream in = Files.newInputStream(path)) {
            int r;
            while ((r = in.read(buf)) >= 0) {
                md.update(buf, 0, r);
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    @Override
    public void destroy() {
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
        closeClassLoaders(activeClassLoaders);
        activeClassLoaders.clear();
    }

    private static void closeClassLoaders(List<URLClassLoader> classLoaders) {
        for (URLClassLoader cl : classLoaders) {
            try {
                cl.close();
            } catch (Exception ignored) {
            }
        }
    }
}
