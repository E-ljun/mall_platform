package com.mall.content.domain.enums;

import java.util.Map;

public enum MarketingPlatform {
    XIAOHONGSHU,
    TAOBAO,
    DOUYIN;

    private static final Map<MarketingPlatform, String> LABELS = Map.of(
            XIAOHONGSHU, "小红书",
            TAOBAO, "淘宝",
            DOUYIN, "抖音"
    );

    public String label() {
        return LABELS.get(this);
    }
}
