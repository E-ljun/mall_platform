package com.mall.content.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDescriptionResult {

    private String shortTitle;
    private List<String> sellingPoints = new ArrayList<>();
    private String detailContent;
    private String category;
    private String material;
    private String color;
    private List<String> styleTags = new ArrayList<>();
    private List<String> targetAudience = new ArrayList<>();
    private List<String> usageScenarios = new ArrayList<>();
    private List<String> complianceWarnings = new ArrayList<>();
}
