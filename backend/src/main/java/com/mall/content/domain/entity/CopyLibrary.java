package com.mall.content.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("copy_library")
public class CopyLibrary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String platform;
    private String groupName;
    private String productName;
    private String productImage;
    private String content;
    private String tags;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
