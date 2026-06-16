package com.mall.content.ai.prompt;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 从原项目 lib/ai/prompts/analysis.ts 迁移并扩展，输出符合验收标准的商品描述结构。
 */
public final class ProductDescriptionPrompts {

    private static final String OUTPUT_SHAPE = """
            {
              "shortTitle": "15字以内精炼标题",
              "sellingPoints": ["3-5条核心卖点短句"],
              "detailContent": "150-300字商品详情描述",
              "category": "分类",
              "material": "材质",
              "color": "颜色",
              "styleTags": ["风格标签"],
              "targetAudience": ["目标人群"],
              "usageScenarios": ["使用场景"],
              "complianceWarnings": ["若存在夸大宣传风险则列出，否则空数组"]
            }
            """;

    private ProductDescriptionPrompts() {
    }

    public static String buildProductDescriptionPrompt(
            String productName,
            String keywords,
            List<String> assetSummaries,
            List<String> imageBase64List
    ) {
        String assetBlock = assetSummaries.isEmpty()
                ? "无已上传图片。"
                : assetSummaries.stream().collect(Collectors.joining("\n"));

        return String.join("\n",
                "你是一名资深电商商品策划与合规审核专家。",
                "请结合商品图片、商品名称和补充关键词，输出严格 JSON，不要 markdown，不要额外说明。",
                "所有文案使用简体中文。",
                "shortTitle 必须在 15 个汉字以内。",
                "sellingPoints 必须 3-5 条，每条尽量短。",
                "detailContent 必须 150-300 字，与图片内容一致，不得编造图片中不存在的功能。",
                "遵守《广告法》与平台规范：禁止绝对化用语（最好/第一/100%）、禁止虚假医疗功效、禁止误导性价格承诺。",
                "若图片信息不足，请合理推断并在 complianceWarnings 中标注不确定性。",
                "",
                "商品名称：" + productName,
                "补充关键词：" + (keywords == null || keywords.isBlank() ? "无" : keywords),
                "图片资产：",
                assetBlock,
                imageBase64List.isEmpty() ? "（未附带图片 base64，请仅依据资产描述推断）" : "（已附带 " + imageBase64List.size() + " 张图片供多模态分析）",
                "",
                "返回 JSON 结构：",
                OUTPUT_SHAPE
        );
    }

    public static String buildRepairPrompt(String raw) {
        return String.join("\n",
                "请将以下畸形输出修复为严格 JSON，仅返回 JSON。",
                "目标结构：",
                OUTPUT_SHAPE,
                "",
                "待修复内容：",
                raw
        );
    }
}
