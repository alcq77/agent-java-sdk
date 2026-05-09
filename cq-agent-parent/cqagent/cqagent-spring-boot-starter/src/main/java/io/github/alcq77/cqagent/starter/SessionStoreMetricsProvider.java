package io.github.alcq77.cqagent.starter;

import java.util.Map;

/**
 * 会话存储指标提供器。
 */
public interface SessionStoreMetricsProvider {

    Map<String, Object> metrics();
}
