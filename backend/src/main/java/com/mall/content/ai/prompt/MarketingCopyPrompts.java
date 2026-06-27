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
        return buildMarketingCopyPrompt(platform, productName, shortTitle, sellingPointsJson, detailContent, variantCount, null);
    }

    public static String buildMarketingCopyPrompt(
            MarketingPlatform platform,
            String productName,
            String shortTitle,
            String sellingPointsJson,
            String detailContent,
            int variantCount,
            String scenario
    ) {
        String scenarioGuide = "";
        if (scenario != null && !scenario.isBlank()) {
            scenarioGuide = switch (scenario.trim()) {
                case "618" -> """
                        营销场景：618大促
                        - 强调"全年最低价""错过等一年"等紧迫感话术
                        - 突出平台满减、跨店优惠等促销机制
                        - 使用倒计时、限量等稀缺性暗示
                        """;
                case "double11" -> """
                        营销场景：双11狂欢
                        - 双11全球狂欢节氛围，强调"年度必买清单"
                        - 突出预售权益、定金膨胀、前N名免单
                        - 用"疯抢""爆款"等点燃购物欲
                        """;
                case "new_launch" -> """
                        营销场景：新品首发
                        - 突出"独家首发""限量首发"的新鲜感
                        - 强调创新点、升级亮点、首发专属福利
                        - 用"抢先体验""第一时间拥有"制造期待
                        """;
                case "clearance" -> """
                        营销场景：清仓特卖
                        - 强调"清仓价""历史最低""卖完下架"
                        - 突出性价比，弱化包装，强化实惠感
                        - 催促下单："库存告急""手慢无"
                        """;
                default -> "";
            };
        }

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
            case OTHER -> """
                    平台风格：通用
                    - 专业的电商商品介绍风格
                    - 突出商品核心卖点与优势
                    - 语言流畅自然，适合多平台分发
                    - 结构清晰：引入→卖点→总结
                    """;
        };

        return String.join("\n",
                "你是资深电商带货文案策划。根据商品信息生成营销文案。",
                "仅返回严格 JSON，不要 markdown。",
                scenarioGuide.isEmpty() ? "" : scenarioGuide,
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
        ).replace("\n\n\n", "\n\n"); // 去掉空行
    }
}
