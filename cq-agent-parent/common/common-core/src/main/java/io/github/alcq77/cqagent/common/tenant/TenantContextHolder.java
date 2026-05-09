package io.github.alcq77.cqagent.common.tenant;

import org.slf4j.MDC;

/**
 * 当前请求/任务的租户 ID（ThreadLocal），与文档中行级多租户约定一致。
 * <p>应在请求入口设置、出口 {@link #clear()}，避免线程池复用泄漏。</p>
 */
public final class TenantContextHolder {

    public static final String MDC_TENANT_KEY = "tenantId";

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
        if (tenantId != null) {
            MDC.put(MDC_TENANT_KEY, String.valueOf(tenantId));
        } else {
            MDC.remove(MDC_TENANT_KEY);
        }
    }

    public static Long getTenantIdOrNull() {
        return TENANT_ID.get();
    }

    public static long requireTenantId() {
        Long id = TENANT_ID.get();
        if (id == null) {
            throw new IllegalStateException("当前上下文未设置 tenantId");
        }
        return id;
    }

    public static void clear() {
        TENANT_ID.remove();
        MDC.remove(MDC_TENANT_KEY);
    }
}
