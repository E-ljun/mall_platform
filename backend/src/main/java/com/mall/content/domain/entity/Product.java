package com.mall.content.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private BigDecimal price;
    private Long categoryId;
    private Integer stock;
    private String status;
    private String mainImageUrl;
    private String shortTitle;
    private String sellingPoints;
    private String detailContent;
    private String keywords;
    private String aiAnalysisRaw;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
