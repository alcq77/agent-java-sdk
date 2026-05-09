package io.github.alcq77.cqagent.common.util;

import java.util.UUID;

/**
 * ID 生成（轻量替代雪花等，后续可换为 Snowflake 实现同一工具入口）。
 */
public final class UuidUtils {

    private UuidUtils() {
    }

    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    public static String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
