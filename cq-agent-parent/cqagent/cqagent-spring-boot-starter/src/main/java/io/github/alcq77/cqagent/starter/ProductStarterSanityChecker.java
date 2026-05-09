package io.github.alcq77.cqagent.starter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 启动期配置自检，优先给出可读错误，减少运行期排障成本。
 */
public class ProductStarterSanityChecker implements ApplicationRunner {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*}}");

    private final ProductStarterProperties properties;

    public ProductStarterSanityChecker(ProductStarterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getLogicalModel() == null || properties.getLogicalModel().isBlank()) {
            throw new IllegalStateException("agent.product.logical-model must not be blank");
        }
        if (properties.getEndpoints().isEmpty()) {
            throw new IllegalStateException("agent.product.endpoints must not be empty");
        }
        String logicalModel = properties.getLogicalModel();
        boolean routed = properties.getRouting().containsKey(logicalModel)
                || properties.getRoutePolicies().containsKey(logicalModel)
                || properties.getEndpoints().containsKey(logicalModel);
        if (!routed) {
            throw new IllegalStateException(
                    "logical model '" + logicalModel + "' is not routable, please configure routing/route-policies/endpoints");
        }
        validatePromptTemplates();
    }

    private void validatePromptTemplates() {
        ProductStarterProperties.Prompts prompts = properties.getPrompts();
        if (prompts.getDefaultTemplateId() != null && !prompts.getDefaultTemplateId().isBlank()
                && !prompts.getTemplates().containsKey(prompts.getDefaultTemplateId())) {
            throw new IllegalStateException("agent.product.prompts.default-template-id not found in templates");
        }
        prompts.getTemplates().forEach((templateId, template) -> {
            if (templateId == null || templateId.isBlank()) {
                throw new IllegalStateException("prompt template id must not be blank");
            }
            if (isBlank(template.getSystemPrompt()) && isBlank(template.getUserMessage())) {
                throw new IllegalStateException("prompt template '" + templateId + "' must have systemPrompt or userMessage");
            }
            validatePlaceholders(templateId, "systemPrompt", template.getSystemPrompt());
            validatePlaceholders(templateId, "userMessage", template.getUserMessage());
        });
    }

    private static void validatePlaceholders(String templateId, String field, String content) {
        if (isBlank(content)) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("prompt template '" + templateId + "' has invalid placeholder in " + field);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
