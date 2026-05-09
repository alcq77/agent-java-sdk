package demo;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.sdk.AgentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DemoController {

    /**
     * Starter 自动装配后可直接注入 AgentClient。
     */
    private final AgentClient agentClient;

    @GetMapping("/demo/chat")
    public String chat(@RequestParam(defaultValue = "你好") String q) {
        // 最小调用面：只传 message，sessionId/traceId 由运行时管理。
        return agentClient.chat(AgentChatRequest.builder().message(q).build()).getReply();
    }
}
