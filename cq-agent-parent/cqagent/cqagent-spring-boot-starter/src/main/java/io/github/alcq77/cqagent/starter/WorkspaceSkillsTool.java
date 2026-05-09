package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.spi.tool.ProductTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Locale;

public class WorkspaceSkillsTool implements ProductTool {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSkillsTool.class);

    private final WorkspaceSkillsRegistry skillsRegistry;

    public WorkspaceSkillsTool(WorkspaceSkillsRegistry skillsRegistry) {
        this.skillsRegistry = skillsRegistry;
    }

    @Override
    public String name() {
        return "workspace_skills";
    }

    @Override
    public boolean supports(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return false;
        }
        return bestMatch(userInput) != null;
    }

    @Override
    public String execute(String userInput) {
        WorkspaceSkillsRegistry.SkillEntry matched = bestMatch(userInput);
        if (matched != null && log.isDebugEnabled()) {
            log.debug("workspace skill matched name={} priority={}", matched.name(), matched.priority());
        }
        return matched == null ? "" : matched.content();
    }

    /**
     * 命中策略：先看关键字，再看技能名，最后按优先级降序选择最优技能。
     */
    private WorkspaceSkillsRegistry.SkillEntry bestMatch(String userInput) {
        String query = userInput == null ? "" : userInput.toLowerCase(Locale.ROOT);
        return skillsRegistry.currentSkills().values().stream()
                .filter(skill -> hitByKeyword(skill, query) || query.contains(skill.name().toLowerCase(Locale.ROOT)))
                .max(Comparator.comparingInt(WorkspaceSkillsRegistry.SkillEntry::priority))
                .orElse(null);
    }

    private static boolean hitByKeyword(WorkspaceSkillsRegistry.SkillEntry skill, String query) {
        for (String keyword : skill.keywords()) {
            if (query.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
