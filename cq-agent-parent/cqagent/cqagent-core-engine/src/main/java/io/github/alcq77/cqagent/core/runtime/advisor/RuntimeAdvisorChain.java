package io.github.alcq77.cqagent.core.runtime.advisor;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Advisor 执行链，统一管理前置/后置/异常处理。
 */
public final class RuntimeAdvisorChain {

    private final List<AgentRuntimeAdvisor> advisors;

    public RuntimeAdvisorChain(List<AgentRuntimeAdvisor> advisors) {
        List<AgentRuntimeAdvisor> sorted = new ArrayList<>();
        if (advisors != null) {
            for (AgentRuntimeAdvisor advisor : advisors) {
                if (advisor != null && advisor.enabled()) {
                    sorted.add(advisor);
                }
            }
        }
        sorted.sort(Comparator.comparingInt(AgentRuntimeAdvisor::order));
        this.advisors = List.copyOf(sorted);
    }

    public AgentChatRequest before(AgentChatRequest request) {
        AgentChatRequest current = request;
        for (AgentRuntimeAdvisor advisor : advisors) {
            current = advisor.before(current);
        }
        return current;
    }

    public AgentChatResponse after(AgentChatRequest request, AgentChatResponse response) {
        AgentChatResponse current = response;
        for (int i = advisors.size() - 1; i >= 0; i--) {
            current = advisors.get(i).after(request, current);
        }
        return current;
    }

    public RuntimeException onError(AgentChatRequest request, RuntimeException error) {
        RuntimeException current = error;
        for (int i = advisors.size() - 1; i >= 0; i--) {
            current = advisors.get(i).onError(request, current);
        }
        return current;
    }
}
