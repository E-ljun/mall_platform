package com.mall.content.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_generation_log")
public class AiGenerationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private String taskType;
    private String platform;
    private String model;
    private String status;
    private String inputSummary;
    private String errorMessage;
    private Integer durationMs;
    private LocalDateTime createdAt;
}
