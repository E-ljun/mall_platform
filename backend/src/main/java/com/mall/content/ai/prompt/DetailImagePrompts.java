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
            String aspectRatio
    ) {
        return String.join("\n",
                "生成一张电商商品详情页模块图，比例 " + aspectRatio + "。",
                "商品：" + productName,
                "模块标题：" + sectionTitle,
                "文案要点：" + sectionCopy,
                "画面要求：" + visualDirection,
                "图内中文标题清晰，不要乱码，不要挤压到边缘。",
                "遵守物理常识：线缆不得消失进桌面、气流方向合理、物体有支撑。",
                "整体风格适合移动端详情页，不是网页截图。"
        );
    }
}
