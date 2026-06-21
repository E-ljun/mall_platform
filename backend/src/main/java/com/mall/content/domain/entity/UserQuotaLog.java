package com.mall.content.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_quota_log")
public class UserQuotaLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long changeBy;
    private Integer delta;
    private String reason;
    private LocalDateTime createdAt;
}
