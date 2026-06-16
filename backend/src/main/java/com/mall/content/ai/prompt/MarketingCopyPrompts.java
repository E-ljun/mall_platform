package com.mall.content.ai.prompt;

import com.mall.content.domain.enums.MarketingPlatform;

/**
 * 多平台营销文案 Prompt。
 * 小红书风格参考原项目 xiaohongshu-service.ts；淘宝/抖音为验收新增。
 */
public final class MarketingCopyPrompts {

    private MarketingCopyPrompts() {
    }

    public static String buildMarketingCopyPrompt(
            MarketingPlatform platform,
            String productName,
            String shortTitle,
            String sellingPointsJson,
            String detailContent,
            int variantCount
    ) {
        String styleGuide = switch (platform) {
            case XIAOHONGSHU -> """
                    平台风格：小红书
                    - 活泼、种草语气，适度使用 emoji（每段 2-5 个）
                    - 分段短句，易扫读
                    - 必须包含 5-10 个中文话题标签（不带空格）
                    - 避免硬广口吻，强调真实体验感
                    """;
            case TAOBAO -> """
                    平台风格：淘宝
                    - 突出性价比、促销感、限时优惠氛围
                    - 卖点清晰分点，强调品质与售后
                    - 语气专业可信，适合详情页/直通车文案
                    - 可含价格锚点话术，但不得虚假降价
                    """;
            case DOUYIN -> """
                    平台风格：抖音
                    - 口语化、短平快、适合口播
                    - 前 3 秒必须有吸睛钩子
                    - 包含明确行动指令（点击/下单/领券）
                    - 句子短，节奏强，可带语气词
                    """;
        };

        return String.join("\n",
                "你是资深电商带货文案策划。根据商品信息生成营销文案。",
                "仅返回严格 JSON，不要 markdown。",
                styleGuide,
                "",
                "商品名称：" + productName,
                "精炼标题：" + shortTitle,
                "核心卖点：" + sellingPointsJson,
                "详情描述：" + detailContent,
                "",
                "请生成 " + variantCount + " 个风格明显不同但内容真实的备选版本。",
                """
                返回 JSON：
                {
                  "platform": "%s",
                  "variants": [
                    {
                      "variantNo": 1,
                      "title": "文案标题",
                      "content": "正文",
                      "hashtags": ["仅小红书需要，其他平台可空数组"]
                    }
                  ]
                }
                """.formatted(platform.name())
        );
    }
}
