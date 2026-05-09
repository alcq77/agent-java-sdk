package io.github.alcq77.cqagent.starter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 启动时初始化工作目录与基础文件。
 */
public class ProductWorkspaceInitializer implements ApplicationRunner {

    private final ProductStarterProperties properties;

    public ProductWorkspaceInitializer(ProductStarterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path root = Paths.get(properties.getWorkspace()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("context"));
        Files.createDirectories(root.resolve("skills"));
        Files.createDirectories(root.resolve("plugins"));
        Files.createDirectories(root.resolve("tasks"));
        Files.createDirectories(root.resolve("sessions"));
        Files.createDirectories(root.resolve("skills").resolve("default"));
        writeIfAbsent(root.resolve("AGENT.md"), "# AGENT\n\nDefine your agent role and style here.\n");
        writeIfAbsent(root.resolve("INFO.md"), "# INFO\n\nPut runtime and business context here.\n");
        writeIfAbsent(root.resolve("skills").resolve("default").resolve("SKILL.md"),
                "# SKILL\n\nDescribe one reusable capability here.\n");
    }

    private static void writeIfAbsent(Path file, String content) throws Exception {
        if (!Files.exists(file)) {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
    }
}
