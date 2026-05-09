package demo;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OnboardingController {

    @GetMapping(value = "/onboarding", produces = MediaType.TEXT_HTML_VALUE)
    public String onboarding() {
        return """
                <html>
                <head><title>Starter Demo Onboarding</title></head>
                <body style="font-family: Arial, sans-serif; padding: 24px;">
                  <h2>Starter Demo Onboarding</h2>
                  <p>1) 编辑 <code>application.yml</code> 配置模型端点与路由。</p>
                  <p>2) 启动后访问 <code>/demo/chat?q=你好</code> 验证 LangChain4j 基座调用链。</p>
                  <p>3) 可将插件 Jar 放到 <code>workspace/plugins</code> 目录触发自动发现。</p>
                  <p>4) 可将技能 Markdown 放到 <code>workspace/skills</code> 目录供技能工具使用。</p>
                </body>
                </html>
                """;
    }
}
