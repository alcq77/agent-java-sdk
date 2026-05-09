package io.github.alcq77.cqagent.starter;

/**
 * 会话存储健康探针。
 * <p>
 * 仅用于 Starter 健康检查扩展，避免把健康探测能力耦合到 SPI。
 */
public interface SessionStoreHealthProbe {

    boolean healthy();

    String detail();
}
