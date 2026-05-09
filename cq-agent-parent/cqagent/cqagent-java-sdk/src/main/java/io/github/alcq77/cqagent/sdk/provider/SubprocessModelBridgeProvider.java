package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Python-native model bridge provider.
 * <p>
 * Supports three backends via their OpenAI-compatible HTTP API:
 * <ul>
 *   <li><b>vllm</b> — {@code python -m vllm.entrypoints.openai.api_server --model ...}</li>
 *   <li><b>llama.cpp</b> — {@code llama-server --model ...}</li>
 *   <li><b>transformers</b> — any TGI or custom OpenAI-compatible server</li>
 * </ul>
 * <p>
 * Two operating modes:
 * <ol>
 *   <li><b>Managed subprocess</b> — if {@code bridge.command} is set in headers, this provider spawns
 *       a Python process, waits for it to become ready, then delegates via OpenAI-compatible HTTP.</li>
 *   <li><b>External server</b> — if no {@code bridge.command} is set, the provider connects to an
 *       existing server at {@code baseUrl} (e.g. a remote vllm instance).</li>
 * </ol>
 * <p>
 * Endpoint header keys:
 * <ul>
 *   <li>{@code bridge.command} — Python executable (e.g. "python", "vllm", "llama-server")</li>
 *   <li>{@code bridge.args} — Space-separated CLI arguments</li>
 *   <li>{@code bridge.port} — Port for the managed subprocess (default: 8000)</li>
 *   <li>{@code bridge.startup-timeout-seconds} — Max wait for subprocess readiness (default: 60)</li>
 * </ul>
 * <p>
 * When a managed subprocess is used, it is started lazily on the first {@code createChatLanguageModel} call
 * and shut down via {@link #close()} or JVM shutdown hook.
 */
public class SubprocessModelBridgeProvider implements ProductModelProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SubprocessModelBridgeProvider.class);

    /**
     * Tracks one subprocess per (baseUrl + port) to avoid duplicate spawns.
     */
    private final Map<String, PythonServerProcess> managedProcesses = new ConcurrentHashMap<>();

    @Override
    public String providerCode() {
        return ModelProviderCodes.PYTHON_BRIDGE;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming()
            .withToolCalling(false);
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        String resolvedUrl = resolveBaseUrl(endpoint);
        ensureServerRunning(endpoint, resolvedUrl);
        String modelName = resolveModelName(endpoint, logicalModel);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .baseUrl(resolvedUrl)
            .modelName(modelName)
            .timeout(endpoint.getReadTimeout());
        if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
            builder.apiKey(endpoint.getApiKey());
        }
        builder.customHeaders(safeHeaders(endpoint.getHeaders()));
        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        String resolvedUrl = resolveBaseUrl(endpoint);
        ensureServerRunning(endpoint, resolvedUrl);
        String modelName = resolveModelName(endpoint, logicalModel);
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
            .baseUrl(resolvedUrl)
            .modelName(modelName)
            .timeout(endpoint.getReadTimeout());
        if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
            builder.apiKey(endpoint.getApiKey());
        }
        builder.customHeaders(safeHeaders(endpoint.getHeaders()));
        return builder.build();
    }

    /**
     * If a managed subprocess is configured and not yet running, starts it and waits for readiness.
     */
    private void ensureServerRunning(ProductEndpointConfig endpoint, String resolvedUrl) {
        Map<String, String> headers = endpoint.getHeaders() != null ? endpoint.getHeaders() : Map.of();
        String command = headers.get("bridge.command");
        if (command == null || command.isBlank()) {
            return; // external server mode — nothing to start
        }
        String processKey = resolvedUrl;
        if (managedProcesses.containsKey(processKey)) {
            PythonServerProcess existing = managedProcesses.get(processKey);
            if (existing.isAlive()) {
                return;
            }
            // process died — remove and restart
            managedProcesses.remove(processKey);
        }
        PythonServerProcess proc = PythonServerProcess.fromHeaders(headers, parsePort(resolvedUrl));
        try {
            log.info("Starting Python model server: {} {}", command, headers.getOrDefault("bridge.args", ""));
            proc.start();
            proc.waitForReady();
            managedProcesses.put(processKey, proc);
            log.info("Python model server ready on {}", proc.getBaseUrl());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start Python model server: " + e.getMessage(), e);
        }
    }

    /**
     * Shuts down all managed subprocesses.
     */
    public void close() {
        managedProcesses.forEach((key, proc) -> {
            log.info("Stopping managed Python server on port {}", proc.getPort());
            proc.stop();
        });
        managedProcesses.clear();
    }

    private static String resolveBaseUrl(ProductEndpointConfig endpoint) {
        if (endpoint.getBaseUrl() == null || endpoint.getBaseUrl().isBlank()) {
            throw new IllegalArgumentException(
                "endpoint.baseUrl must not be blank for python_bridge provider. " +
                "Set it to the server URL (e.g. http://127.0.0.1:8000/v1) " +
                "or use bridge.command headers for managed subprocess.");
        }
        String url = endpoint.getBaseUrl().trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String resolveModelName(ProductEndpointConfig endpoint, String logicalModel) {
        return endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
    }

    private static Map<String, String> safeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        Map<String, String> filtered = new LinkedHashMap<>();
        headers.forEach((k, v) -> {
            if (!k.startsWith("bridge.")) {
                filtered.put(k, v);
            }
        });
        return filtered;
    }

    private static int parsePort(String url) {
        try {
            // http://127.0.0.1:8000/v1 -> extract 8000
            String host = url.replaceFirst("https?://", "");
            String portStr = host.replaceAll("[:/].*", "");
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            return 8000;
        }
    }
}