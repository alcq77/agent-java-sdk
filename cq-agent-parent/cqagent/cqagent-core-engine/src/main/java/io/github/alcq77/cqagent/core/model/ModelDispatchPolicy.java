package io.github.alcq77.cqagent.core.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 逻辑模型分发策略：根据 taskType/tags 选择目标逻辑模型。
 */
@Data
public class ModelDispatchPolicy {

    /**
     * 命中时切换到的逻辑模型。
     */
    private String targetLogicalModel;

    /**
     * 匹配的任务类型（为空表示不限制）。
     */
    private List<String> taskTypes = new ArrayList<>();

    /**
     * 必须包含的标签（为空表示不限制）。
     */
    private List<String> requiredTags = new ArrayList<>();
}
