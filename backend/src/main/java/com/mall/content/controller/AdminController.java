package com.mall.content.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.content.common.ApiResponse;
import com.mall.content.domain.entity.SysUser;
import com.mall.content.domain.entity.SystemAnnouncement;
import com.mall.content.service.AdminService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        Page<SysUser> result = adminService.listUsers(page, size, keyword, status);
        return ApiResponse.ok(Map.of(
                "records", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize(),
                "maxUsers", 50,
                "currentCount", adminService.currentUserCount()
        ));
    }

    @PostMapping("/users/{id}/approve")
    public ApiResponse<SysUser> approveUser(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok(adminService.approveUser(id, auth));
    }

    @PostMapping("/users/{id}/disable")
    public ApiResponse<SysUser> disableUser(@PathVariable Long id) {
        return ApiResponse.ok(adminService.disableUser(id));
    }

    @PostMapping("/users/{id}/enable")
    public ApiResponse<SysUser> enableUser(@PathVariable Long id) {
        return ApiResponse.ok(adminService.enableUser(id));
    }

    // ==================== 配额管理 ====================

    @PostMapping("/users/{id}/quota")
    public ApiResponse<SysUser> setQuota(@PathVariable Long id, @RequestBody QuotaRequest req, Authentication auth) {
        return ApiResponse.ok(adminService.setQuota(id, req.getQuotaTotal(), auth));
    }

    @PostMapping("/users/{id}/quota/add")
    public ApiResponse<SysUser> addQuota(@PathVariable Long id, @RequestBody QuotaRequest req, Authentication auth) {
        return ApiResponse.ok(adminService.addQuota(id, req.getAmount(), auth));
    }

    // ==================== 权限管理 ====================

    @PostMapping("/users/{id}/permissions")
    public ApiResponse<SysUser> setPermissions(@PathVariable Long id, @RequestBody PermissionsRequest req) {
        return ApiResponse.ok(adminService.setPermissions(id, req.getPermissions()));
    }

    // ==================== 系统公告 ====================

    @GetMapping("/announcement")
    public ApiResponse<SystemAnnouncement> getAnnouncement() {
        return ApiResponse.ok(adminService.getActiveAnnouncement());
    }

    @PostMapping("/announcement")
    public ApiResponse<SystemAnnouncement> setAnnouncement(@RequestBody AnnouncementRequest req, Authentication auth) {
        return ApiResponse.ok(adminService.setAnnouncement(req.getContent(), auth));
    }

    // ==================== 用户详情 ====================

    @GetMapping("/users/{id}/detail")
    public ApiResponse<Map<String, Object>> userDetail(@PathVariable Long id) {
        var user = adminService.getUserById(id);
        var logs = adminService.getQuotaLogs(id);
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("phone", user.getPhone() != null ? user.getPhone() : "");
        info.put("email", user.getEmail() != null ? user.getEmail() : "");
        info.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        info.put("role", user.getRole());
        info.put("status", user.getStatus());
        info.put("quotaTotal", user.getQuotaTotal());
        info.put("quotaUsed", user.getQuotaUsed());
        info.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        info.put("quotaLogs", logs);
        return ApiResponse.ok(info);
    }

    // ==================== 回收站 ====================

    @GetMapping("/recycle-bin/products")
    public ApiResponse<Map<String, Object>> recycleBinProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(adminService.listDeletedProducts(page, size));
    }

    @PostMapping("/recycle-bin/products/{id}/restore")
    public ApiResponse<?> restoreProduct(@PathVariable Long id) {
        adminService.restoreProduct(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/recycle-bin/products/{id}")
    public ApiResponse<?> purgeProduct(@PathVariable Long id) {
        adminService.purgeProduct(id);
        return ApiResponse.ok(null);
    }

    // ==================== DTO ====================

    @Data
    public static class QuotaRequest {
        private Integer quotaTotal;
        private Integer amount;
    }

    @Data
    public static class PermissionsRequest {
        private List<String> permissions;
    }

    @Data
    public static class AnnouncementRequest {
        @NotBlank
        private String content;
    }
}
