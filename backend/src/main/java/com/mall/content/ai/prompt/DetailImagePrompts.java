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
            boolean hasRefImages
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的电商设计师。请根据以下商品信息生成一张电商详情页模块图。\n");
        if (hasRefImages) {
            sb.append("【重要】已附上商品实拍图作为参考，生成的图片中的商品必须与参考图一致，"
                    + "包括颜色、形状、材质、包装等关键特征，绝不能生成其他商品！\n");
        }
        sb.append("比例：").append(aspectRatio).append("\n");
        sb.append("商品名称：").append(productName).append("\n");
        sb.append("模块标题：").append(sectionTitle).append("\n");
        sb.append("文案要点：").append(sectionCopy).append("\n");
        sb.append("画面要求：").append(visualDirection).append("\n");
        sb.append("要求：高端电商风格，适合移动端详情页；图内中文清晰无乱码；"
                + "遵守物理常识；不是网页截图。");
        return sb.toString();
    }
}
