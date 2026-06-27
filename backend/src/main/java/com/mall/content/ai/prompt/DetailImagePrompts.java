package com.mall.content.ai.prompt;

/**
 * 详情页分段图片生成 Prompt，迁移自原项目 lib/ai/prompts/generation.ts 核心约束。
 */
public final class DetailImagePrompts {

    private DetailImagePrompts() {
    }

    public static String buildSectionImagePrompt(
            String productName,
            String sectionTitle,
            String sectionCopy,
            String visualDirection,
            String aspectRatio,
            boolean hasRefImages,
            String imageTextDensity
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的电商设计师。请根据以下商品信息生成一张电商详情页模块图。\n");
        if (hasRefImages) {
            sb.append("【重要】已附上商品实拍图作为参考，生成的图片中的商品必须与参考图一致，"
                    + "包括颜色、形状、材质、包装等关键特征，绝不能生成其他商品！\n");
        }

        // 根据文字密度动态调整文案策略
        String density = imageTextDensity != null ? imageTextDensity.trim().toLowerCase() : "moderate";
        sb.append("【文案密度要求】");
        switch (density) {
            case "minimal" -> sb.append("本商品以视觉为主，仅需在图片上放置1-2句核心slogan或品牌标语。"
                    + "文字精炼、字号优雅，不抢主视觉。画面质感、光影、构图是重点。\n");
            case "moderate" -> sb.append("本商品需要适度文案说明，"
                    + "在图片合适区域放置3-4条简短卖点。文字简洁排版，作为画面的辅助信息。\n");
            case "rich" -> sb.append("本商品需要在图片上展示较多文字信息。"
                    + "请使用【大标题 → 小字标注】的层级结构，确保中文文字清晰可读、不扭曲、不变形。"
                    + "文字区域与商品区域明确分区，避免文字压在复杂纹理上。"
                    + "如需多行文字，使用清晰的对齐和间距，不超过4行正文。\n");
            default -> sb.append("根据商品特点自行判断文案密度，文字清晰可读。\n");
        }

        sb.append("比例：").append(aspectRatio).append("\n");
        sb.append("商品名称：").append(productName).append("\n");
        sb.append("模块标题：").append(sectionTitle).append("\n");
        sb.append("文案要点：").append(sectionCopy).append("\n");
        sb.append("画面要求：").append(visualDirection).append("\n");
        sb.append("要求：高端电商风格，适合移动端详情页；图内中文清晰无乱码，字体端正不扭曲；"
                + "遵守物理常识；不是网页截图；产品主体突出。");
        return sb.toString();
    }

    /**
     * AI 分析商品图片，推荐 sectionTitle / sectionCopy / visualDirection。
     * sectionTitle 必须紧贴具体商品特征，不能是泛泛的模块类型名。
     */
    public static String buildSuggestionPrompt(String productName, String productDesc) {
        // 商品名称是权威定义，优先级高于图片推测
        String nameLine = (productName != null && !productName.isBlank())
                ? productName
                : "（请根据图片自行判断商品类型）";
        String descLine = (productDesc != null && !productDesc.isBlank())
                ? productDesc
                : "（无额外描述）";

        return """
你是一位资深的电商视觉设计专家。请根据商品图片和商品信息，为生成一张电商详情页模块图提供最佳参数建议。

**【重要】下面提供的「商品名称」是该物品的权威定义。**
即使图片看起来像其他东西，也必须以商品名称为准来构思所有内容。
例如：商品名称写的是"洗发水"，就必须按洗发水来设计，绝不能当成花瓶或其他物品。

**你需要返回一个 JSON 对象，包含以下三个字段：**

1. `sectionTitle`（模块标题）：
   - 根据商品图片和商品信息，提炼一个与**这个具体商品**紧密相关的标题
   - 要紧扣商品的材质、工艺、功效或使用体验
   - 4-8个字为佳，像一句精炼的广告语，让顾客一眼看懂这个模块在讲什么
   - 正确示例（手工皂）："天然花植浮雕"、"冷制手工精萃"、"一皂双用设计"
   - 正确示例（台灯）："双关节灵活调节"、"护眼柔光技术"
   - 正确示例（饮料）："低糖电解质配方"、"真实西柚风味"
   - 错误示例："核心卖点展示"、"产品参数对比"、"功能演示"——这些太泛，看不出是什么商品

2. `sectionCopy`（文案要点）：
   - 列出图中应该展示的 3-5 条关键文案
   - 每条文案简练有力，10字以内为佳，用中文逗号分隔

3. `visualDirection`（画面描述）：
   这是最重要的字段，需要详细描述。请从以下维度构思，写一段 80-150 字的完整画面描述：

   **氛围感：** 整体调性是什么？（科技感 / 温馨 / 高端奢华 / 清新自然 / 活力运动 / 极简主义）
   **场景背景：** 商品放在什么环境里？
   **光影：** 什么样的光线？
   **构图：** 画面如何布局？
   **配色倾向：** 主色调建议
   **文字排版：** 文字放在画面的哪个区域？用什么样的排版风格？

   根据商品类型，给出最合适的电商级视觉方案。

**【商品信息——权威定义，优先级高于图片推测】**
商品名称："""
                + nameLine + "\n商品描述：" + descLine
                + "\n\n请直接返回 JSON（不要 markdown 代码块），格式："
                + "\n{\"sectionTitle\":\"...\", \"sectionCopy\":\"...\", \"visualDirection\":\"...\"}";
    }
}
