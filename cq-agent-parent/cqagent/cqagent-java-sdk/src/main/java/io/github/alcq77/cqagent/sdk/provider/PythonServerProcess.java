package io.github.alcq77.cqagent.sdk.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages a Python model server subprocess (vllm / llama.cpp-server / transformers TGI).
 * <p>
 * Lifecycle: {@link #start()} -> {@link #waitForReady()} -> use HTTP API -> {@link #stop()}.
 * The server is expected to expose an OpenAI-compatible {@code /v1/chat/completions} endpoint.
 */
public class PythonServerProcess implements AutoCloseable {

    private static final int DEFAULT_PORT = 8000;
    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 60;
    private static final int HEALTH_CHECK_INTERVAL_MS = 500;

    private final String command;
    private final List<String> args;
    private final int port;
    private final int startupTimeoutSeconds;
    private final Map<String, String> env;

    private Process process;
    private Thread outputDrainer;

    /**
     * @param command            Python executable or script (e.g. "python", "vllm", "llama-server")
     * @param args               Arguments to pass (e.g. "--model", "Qwen/Qwen2-7B", "--port", "8000")
     * @param port               Port the server will listen on
     * @param startupTimeoutSeconds Max seconds to wait for the server to become ready
     * @param env                Extra environment variables (may be null)
     */
    public PythonServerProcess(String command, List<String> args, int port, int startupTimeoutSeconds,
                               Map<String, String> env) {
        this.command = command;
        this.args = args != null ? List.copyOf(args) : List.of();
        this.port = port > 0 ? port : DEFAULT_PORT;
        this.startupTimeoutSeconds = startupTimeoutSeconds > 0 ? startupTimeoutSeconds : DEFAULT_STARTUP_TIMEOUT_SECONDS;
        this.env = env;
    }

    /**
     * Starts the Python subprocess.
     *
     * @throws IOException if the process cannot be started
     */
    public void start() throws IOException {
        if (process != null && process.isAlive()) {
            return;
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(command);
        cmd.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.inheritIO();
        if (env != null && !env.isEmpty()) {
            pb.environment().putAll(env);
        }
        process = pb.start();
        outputDrainer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // drain stdout/stderr to prevent buffer deadlock
                }
            } catch (IOException ignored) {
                // process closed
            }
        }, "python-bridge-drainer");
        outputDrainer.setDaemon(true);
        outputDrainer.start();
    }

    /**
     * Blocks until the server responds to a health check or timeout is reached.
     *
     * @throws IllegalStateException if the process dies before becoming ready
     * @throws RuntimeException      if the server does not become ready within the timeout
     */
    public void waitForReady() {
        if (process == null) {
            throw new IllegalStateException("Process not started. Call start() first.");
        }
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(startupTimeoutSeconds);
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException(
                    "Python process exited with code " + process.exitValue() + " before becoming ready");
            }
            if (isServerReady()) {
                return;
            }
            sleep(HEALTH_CHECK_INTERVAL_MS);
        }
        throw new RuntimeException(
            "Python server did not become ready within " + startupTimeoutSeconds + "s on port " + port);
    }

    /**
     * Returns {@code true} if the server responds to a health check.
     */
    public boolean isServerReady() {
        try {
            URL url = new URL("http://127.0.0.1:" + port + "/v1/models");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns the base URL of the running server (e.g. "http://127.0.0.1:8000/v1").
     */
    public String getBaseUrl() {
        return "http://127.0.0.1:" + port + "/v1";
    }

    /**
     * Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns whether the subprocess is still alive.
     */
    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    /**
     * Stops the subprocess gracefully, then forcibly if needed.
     */
    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        stop();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Resolves server configuration from endpoint headers.
     * <p>
     * Supported headers:
     * <ul>
     *   <li>{@code bridge.command} — Python executable (default: "python")</li>
     *   <li>{@code bridge.args} — Space-separated arguments</li>
     *   <li>{@code bridge.port} — Port number (default: 8000)</li>
     *   <li>{@code bridge.startup-timeout-seconds} — Startup timeout (default: 60)</li>
     * </ul>
     */
    static PythonServerProcess fromHeaders(Map<String, String> headers, int defaultPort) {
        Map<String, String> h = headers != null ? headers : Map.of();
        String command = h.getOrDefault("bridge.command", "python");
        String argsStr = h.getOrDefault("bridge.args", "");
        List<String> args = argsStr.isBlank() ? List.of() : List.of(argsStr.split("\\s+"));
        int port = parseInt(h.get("bridge.port"), defaultPort > 0 ? defaultPort : DEFAULT_PORT);
        int timeout = parseInt(h.get("bridge.startup-timeout-seconds"), DEFAULT_STARTUP_TIMEOUT_SECONDS);
        return new PythonServerProcess(command, args, port, timeout, null);
    }

    private static int parseInt(String s, int fallback) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}