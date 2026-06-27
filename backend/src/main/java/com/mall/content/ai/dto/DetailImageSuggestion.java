package com.mall.content.ai.dto;

import lombok.Data;

/**
 * AI 一键填写详情图表单的建议结果。
 */
@Data
public class DetailImageSuggestion {
    private String sectionTitle;     // 模块标题，如"核心参数对比"
    private String sectionCopy;      // 文案要点，图中需展示的关键信息
    private String visualDirection;  // 画面描述，含氛围、场景、光影、构图等
}
