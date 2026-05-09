package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.spi.tool.ProductTool;

public class ProductPluginDispatcherTool implements ProductTool {

    private final ProductPluginDirectoryToolRegistry registry;

    public ProductPluginDispatcherTool(ProductPluginDirectoryToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "plugin_dispatcher";
    }

    @Override
    public boolean supports(String userInput) {
        return registry.currentTools().stream().anyMatch(t -> t.supports(userInput));
    }

    @Override
    public String execute(String userInput) {
        return registry.currentTools().stream()
                .filter(t -> t.supports(userInput))
                .findFirst()
                .map(t -> t.execute(userInput))
                .orElse("");
    }
}
