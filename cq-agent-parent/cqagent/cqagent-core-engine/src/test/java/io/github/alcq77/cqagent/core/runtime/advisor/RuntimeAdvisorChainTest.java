package io.github.alcq77.cqagent.core.runtime.advisor;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeAdvisorChainTest {

    @Test
    void shouldApplyBeforeInOrderAndAfterInReverseOrder() {
        RuntimeAdvisorChain chain = new RuntimeAdvisorChain(List.of(
            new PrefixAdvisor("A", 10),
            new PrefixAdvisor("B", 20)
        ));
        AgentChatRequest request = AgentChatRequest.builder().message("q").build();
        AgentChatResponse response = AgentChatResponse.builder().reply("answer").build();

        AgentChatRequest advisedRequest = chain.before(request);
        AgentChatResponse advisedResponse = chain.after(advisedRequest, response);

        assertEquals("[A][B]answer", advisedResponse.getReply());
        assertEquals("B:A:q", advisedRequest.getMessage());
    }

    private static final class PrefixAdvisor implements AgentRuntimeAdvisor {
        private final String name;
        private final int order;

        private PrefixAdvisor(String name, int order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public AgentChatRequest before(AgentChatRequest request) {
            return AgentChatRequest.builder()
                .sessionId(request.getSessionId())
                .message((request.getMessage() == null ? "" : name + ":" + request.getMessage()))
                .systemPrompt(request.getSystemPrompt())
                .traceId(request.getTraceId())
                .promptTemplateId(request.getPromptTemplateId())
                .promptVariables(request.getPromptVariables())
                .taskType(request.getTaskType())
                .tags(request.getTags())
                .build();
        }

        @Override
        public AgentChatResponse after(AgentChatRequest request, AgentChatResponse response) {
            response.setReply("[" + name + "]" + response.getReply());
            return response;
        }
    }
}
