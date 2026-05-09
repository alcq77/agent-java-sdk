package io.github.alcq77.cqagent.core.model;

import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内嵌版模型路由器：支持静态路由 + 主备 + 加权 + 健康感知。
 */
public class ProductModelRouter {

    /**
     * 端点可达性探测器，用于健康感知路由。
     */
    public interface EndpointHealthChecker {
        boolean reachable(ProductEndpointConfig endpoint);
    }

    private final EndpointHealthChecker healthChecker;
    private final Map<String, AtomicInteger> cursor = new ConcurrentHashMap<>();

    public ProductModelRouter(EndpointHealthChecker healthChecker) {
        this.healthChecker = healthChecker;
    }

    /**
     * 解析最终逻辑模型：
     * - 先按 taskType/tags 命中 modelDispatchPolicies；
     * - 未命中时回退到请求逻辑模型。
     */
    public String resolveLogicalModel(String requestedLogicalModel,
                                      Map<String, ModelDispatchPolicy> policies,
                                      String taskType,
                                      List<String> tags) {
        if (policies == null || policies.isEmpty()) {
            return requestedLogicalModel;
        }
        Set<String> tagSet = tags == null ? Set.of() : new HashSet<>(tags);
        for (ModelDispatchPolicy policy : policies.values()) {
            if (policy == null || policy.getTargetLogicalModel() == null || policy.getTargetLogicalModel().isBlank()) {
                continue;
            }
            if (!policy.getTaskTypes().isEmpty() && (taskType == null || !policy.getTaskTypes().contains(taskType))) {
                continue;
            }
            if (!policy.getRequiredTags().isEmpty() && !tagSet.containsAll(policy.getRequiredTags())) {
                continue;
            }
            return policy.getTargetLogicalModel().trim();
        }
        return requestedLogicalModel;
    }

    public List<ProductEndpointConfig> resolveCandidates(String logicalModel,
                                                         Map<String, ProductEndpointConfig> endpointById,
                                                         Map<String, String> routing,
                                                         Map<String, RoutePolicy> policies) {
        // 候选顺序决定了重试与熔断触发顺序，需保持稳定可解释。
        List<String> endpointIds = new ArrayList<>();
        RoutePolicy policy = policies.get(logicalModel);
        if (policy != null) {
            endpointIds.addAll(policyOrder(logicalModel, policy));
        }
        String staticRoute = routing.get(logicalModel);
        if (staticRoute != null && !staticRoute.isBlank()) {
            endpointIds.add(staticRoute);
        }
        if (endpointIds.isEmpty() && endpointById.containsKey(logicalModel)) {
            endpointIds.add(logicalModel);
        }
        List<ProductEndpointConfig> out = new ArrayList<>();
        boolean healthAware = policy == null || policy.isHealthAware();
        for (String id : new LinkedHashSet<>(endpointIds)) {
            ProductEndpointConfig endpoint = endpointById.get(id);
            if (endpoint == null) {
                continue;
            }
            if (!healthAware || healthChecker.reachable(endpoint)) {
                out.add(endpoint);
            }
        }
        return out;
    }

    private List<String> policyOrder(String logicalModel, RoutePolicy policy) {
        // policy 顺序：primary -> weighted(当前命中优先) -> secondary。
        List<String> out = new ArrayList<>();
        if (policy.getPrimaryEndpoint() != null && !policy.getPrimaryEndpoint().isBlank()) {
            out.add(policy.getPrimaryEndpoint().trim());
        }
        if (policy.getWeightedEndpoints() != null && !policy.getWeightedEndpoints().isEmpty()) {
            List<String> weighted = new ArrayList<>();
            for (Map.Entry<String, Integer> e : policy.getWeightedEndpoints().entrySet()) {
                int weight = Math.max(0, e.getValue() == null ? 0 : e.getValue());
                for (int i = 0; i < weight; i++) {
                    weighted.add(e.getKey());
                }
            }
            if (!weighted.isEmpty()) {
                int idx = Math.floorMod(cursor.computeIfAbsent(logicalModel, k -> new AtomicInteger(0))
                        .getAndIncrement(), weighted.size());
                String chosen = weighted.get(idx);
                out.add(chosen);
                for (String v : weighted) {
                    if (!v.equals(chosen)) {
                        out.add(v);
                    }
                }
            }
        }
        out.addAll(policy.getSecondaryEndpoints());
        return out;
    }
}
