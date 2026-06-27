package com.mall.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.content.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 原子扣减配额。单条 SQL，并发安全。
     * @return 影响行数：1=扣减成功，0=配额不足
     */
    @Update("UPDATE sys_user SET quota_used = COALESCE(quota_used, 0) + 1 " +
            "WHERE id = #{userId} AND (quota_total = -1 OR COALESCE(quota_used, 0) < quota_total)")
    int consumeQuotaAtomic(@Param("userId") Long userId);
}
