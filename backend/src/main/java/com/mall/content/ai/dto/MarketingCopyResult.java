package com.mall.content.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketingCopyResult {

    private String platform;
    private List<Variant> variants = new ArrayList<>();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Variant {
        private Integer variantNo;
        private String title;
        private String content;
        private List<String> hashtags = new ArrayList<>();
    }
}
