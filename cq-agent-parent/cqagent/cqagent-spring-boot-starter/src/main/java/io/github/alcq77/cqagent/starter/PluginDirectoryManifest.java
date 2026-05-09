package io.github.alcq77.cqagent.starter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 插件目录清单（{@code plugins-manifest.json}）：文件名 → SHA-256（十六进制小写或大写均可比对）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginDirectoryManifest {

    /**
     * Jar 文件名 → 期望的 SHA-256 十六进制字符串。
     */
    private Map<String, String> artifacts = new LinkedHashMap<>();

    public Map<String, String> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Map<String, String> artifacts) {
        this.artifacts = artifacts != null ? artifacts : new LinkedHashMap<>();
    }
}
