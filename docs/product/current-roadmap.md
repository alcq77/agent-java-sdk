# Current Roadmap

Updated: 2026-04-28

## Completed

- Package names were shortened from `io.github.alcq77.cqagent.product.*` to `io.github.alcq77.cqagent.*`.
- Source directories now match package declarations for `core`, `sdk`, `spi`, `starter`, `plugin`, and `testkit`.
- Maven coordinates were unified to `io.github.alcq77.cqagent` and example dependencies were updated.
- Main docs, examples, CI workflow, and public-facing comments were updated to the new naming.
- `openai_compat` provider remains available.
- `openai` provider was added on top of LangChain4j native OpenAI support.
- `anthropic` provider was added on top of LangChain4j native Anthropic support.
- `ollama` provider was added for locally hosted/self-running models.
- `dashscope` provider was added for Qwen/DashScope API access.
- Provider capability descriptors were introduced for chat, streaming, tool calling, multimodal, structured output, and self-hosted metadata.
- Main module tests pass.
- `examples/java-sdk-demo` compiles.
- `examples/spring-starter-demo` compiles.

## In Progress

- Provider matrix expansion has started with `openai`, `openai_compat`, `anthropic`, `ollama`, and `dashscope`.
- Product naming is mostly unified; IDE metadata may still contain old `cqgent` workspace history.

## Not Started

- Gemini / DeepSeek / Azure OpenAI / Bedrock providers.
- Python-native model bridge:
  - `transformers`
  - `vllm`
  - `llama.cpp`
  - gRPC or subprocess bridge
- Multi-agent orchestration:
  - planner
  - coordinator
  - sub-agent execution
  - workflow/graph runtime
- Production observability:
  - metrics export
  - tracing
  - audit events
- Capability-aware routing and policy selection.

## Known Issues

- `connectTimeout` is present in `ProductEndpointConfig`, but LangChain4j `0.36.2` provider builders expose only a single request `timeout`.
- Existing deployments that depended on old `cqgent` Redis/JDBC defaults or actuator ids need configuration overrides during migration.
- Some files still contain legacy encoding-garbled comments and descriptions, even though they no longer block build/test.

## Next Recommended Order

1. Add Gemini / DeepSeek / Azure OpenAI / Bedrock providers.
2. Design the Python-native model bridge.
3. Start multi-agent coordination as a separate runtime layer instead of mixing it into single-agent runtime code.
4. Add capability-aware routing and policy selection.
