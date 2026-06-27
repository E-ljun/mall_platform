package com.mall.content.controller;

import com.mall.content.common.ApiResponse;
import com.mall.content.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final AiGenerationLogMapper aiLogMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper sysUserMapper;
    private final MarketingCopyMapper copyMapper;

    @GetMapping("/admin/stats/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 近 7 天 AI 调用趋势
        List<Map<String, Object>> aiTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long total = aiLogMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.content.domain.entity.AiGenerationLog>()
                            .between(com.mall.content.domain.entity.AiGenerationLog::getCreatedAt, start, end)
            );
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("count", total);
            aiTrend.add(day);
        }
        result.put("aiTrend", aiTrend);

        // 近 7 天商品新增趋势
        List<Map<String, Object>> productTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long total = productMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.content.domain.entity.Product>()
                            .between(com.mall.content.domain.entity.Product::getCreatedAt, start, end)
            );
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("count", total);
            productTrend.add(day);
        }
        result.put("productTrend", productTrend);

        // 用户配额消耗排行（前 10）
        List<com.mall.content.domain.entity.SysUser> users = sysUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.content.domain.entity.SysUser>()
                        .orderByDesc(com.mall.content.domain.entity.SysUser::getQuotaUsed)
                        .last("LIMIT 10")
        );
        List<Map<String, Object>> quotaRank = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", u.getUsername());
            m.put("quotaTotal", u.getQuotaTotal());
            m.put("quotaUsed", u.getQuotaUsed() != null ? u.getQuotaUsed() : 0);
            return m;
        }).toList();
        result.put("quotaRank", quotaRank);

        // 文案平台分布
        List<Map<String, Object>> platformDist = copyMapper.selectList(null).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        c -> c.getPlatform() != null ? c.getPlatform() : "OTHER",
                        java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("platform", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).toList();
        result.put("platformDist", platformDist);

        return ApiResponse.ok(result);
    }
}
