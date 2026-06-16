package com.mall.content.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_image")
public class ProductImage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String filePath;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private Integer sortOrder;
    private Integer isMain;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
