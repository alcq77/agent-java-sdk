package io.github.alcq77.cqagent.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductModelRouterDispatchTest {

    @Test
    void shouldRouteByTaskTypeAndTags() {
        ProductModelRouter router = new ProductModelRouter(endpoint -> true);
        ModelDispatchPolicy qaPolicy = new ModelDispatchPolicy();
        qaPolicy.setTargetLogicalModel("qa-model");
        qaPolicy.setTaskTypes(List.of("qa"));
        qaPolicy.setRequiredTags(List.of("tier:vip"));

        String resolved = router.resolveLogicalModel(
            "default-model",
            Map.of("qaPolicy", qaPolicy),
            "qa",
            List.of("tier:vip", "lang:zh")
        );

        assertEquals("qa-model", resolved);
    }

    @Test
    void shouldFallbackToRequestedLogicalModelWhenNoPolicyMatched() {
        ProductModelRouter router = new ProductModelRouter(endpoint -> true);
        ModelDispatchPolicy codingPolicy = new ModelDispatchPolicy();
        codingPolicy.setTargetLogicalModel("coding-model");
        codingPolicy.setTaskTypes(List.of("coding"));

        String resolved = router.resolveLogicalModel(
            "default-model",
            Map.of("codingPolicy", codingPolicy),
            "qa",
            List.of("tier:normal")
        );

        assertEquals("default-model", resolved);
    }
}
