package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.starter.ProductStarterProperties;
import io.github.alcq77.cqagent.starter.WorkspaceSkillsRegistry;
import io.github.alcq77.cqagent.starter.WorkspaceSkillsTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceSkillsToolPriorityTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldChooseHigherPrioritySkillWhenKeywordsBothMatch() throws Exception {
        Path skillsDir = tempDir.resolve("skills");
        Files.createDirectories(skillsDir);
        Files.writeString(skillsDir.resolve("billing-basic.md"),
            "priority: 1\nkeywords: billing,invoice\nBASIC_SKILL\n", StandardCharsets.UTF_8);
        Files.writeString(skillsDir.resolve("billing-pro.md"),
            "priority: 10\nkeywords: billing,invoice\nPRO_SKILL\n", StandardCharsets.UTF_8);

        ProductStarterProperties properties = new ProductStarterProperties();
        properties.getSkills().setEnabled(true);
        properties.getSkills().setDirectory(skillsDir.toString());

        WorkspaceSkillsRegistry registry = new WorkspaceSkillsRegistry(properties);
        registry.afterPropertiesSet();
        try {
            WorkspaceSkillsTool tool = new WorkspaceSkillsTool(registry);
            String output = tool.execute("please help with billing invoice");
            assertTrue(output.contains("PRO_SKILL"));
        } finally {
            registry.destroy();
        }
    }
}
