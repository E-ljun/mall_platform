package com.mall.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.content.domain.entity.SysUser;
import com.mall.content.domain.entity.SystemAnnouncement;
import com.mall.content.domain.entity.UserQuotaLog;
import com.mall.content.mapper.SysUserMapper;
import com.mall.content.mapper.SystemAnnouncementMapper;
import com.mall.content.mapper.UserQuotaLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int MAX_USERS = 50;

    private final SysUserMapper sysUserMapper;
    private final UserQuotaLogMapper quotaLogMapper;
    private final SystemAnnouncementMapper announcementMapper;

    // ==================== 用户管理 ====================

    public Page<SysUser> listUsers(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreatedAt);
        return sysUserMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public SysUser approveUser(Long userId, Authentication auth) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (!"PENDING".equals(user.getStatus())) throw new IllegalArgumentException("该用户非待审核状态");

        user.setStatus("ACTIVE");
        user.setApprovedBy((Long) auth.getPrincipal());
        user.setApprovedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return user;
    }

    @Transactional
    public SysUser disableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if ("ADMIN".equals(user.getRole())) throw new IllegalArgumentException("不能禁用管理员账号");
        user.setStatus("DISABLED");
        sysUserMapper.updateById(user);
        return user;
    }

    @Transactional
    public SysUser enableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        user.setStatus("ACTIVE");
        sysUserMapper.updateById(user);
        return user;
    }

    // ==================== 配额管理 ====================

    @Transactional
    public SysUser setQuota(Long userId, int quotaTotal, Authentication auth) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        Long adminId = (Long) auth.getPrincipal();
        int delta = quotaTotal - (user.getQuotaTotal() != null ? user.getQuotaTotal() : 0);

        user.setQuotaTotal(quotaTotal);
        sysUserMapper.updateById(user);

        UserQuotaLog log = new UserQuotaLog();
        log.setUserId(userId);
        log.setChangeBy(adminId);
        log.setDelta(delta);
        log.setReason("管理员设置配额");
        quotaLogMapper.insert(log);

        return user;
    }

    @Transactional
    public SysUser addQuota(Long userId, int amount, Authentication auth) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        Long adminId = (Long) auth.getPrincipal();
        user.setQuotaTotal((user.getQuotaTotal() != null ? user.getQuotaTotal() : 0) + amount);
        sysUserMapper.updateById(user);

        UserQuotaLog log = new UserQuotaLog();
        log.setUserId(userId);
        log.setChangeBy(adminId);
        log.setDelta(amount);
        log.setReason("管理员追加配额");
        quotaLogMapper.insert(log);

        return user;
    }

    // ==================== 权限管理 ====================

    @Transactional
    public SysUser setPermissions(Long userId, List<String> permissions) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        final String json;
        if (permissions == null || permissions.isEmpty()) {
            // 空数组 → 存储为 "[]" 表示无任何权限（与 null 区分，null=全部可用）
            json = "[]";
        } else {
            try {
                json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(permissions);
            } catch (Exception e) {
                throw new IllegalStateException("权限序列化失败", e);
            }
        }
        sysUserMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getPermissions, json));
        user.setPermissions(json);
        return user;
    }

    public boolean canUseModule(SysUser user, String module) {
        if ("ADMIN".equals(user.getRole())) return true;
        if (!"ACTIVE".equals(user.getStatus())) return false;
        // 检查配额
        if (user.getQuotaTotal() != null && user.getQuotaTotal() != -1
                && user.getQuotaUsed() != null && user.getQuotaUsed() >= user.getQuotaTotal()) {
            return false;
        }
        // null = 全部可用（新用户默认）
        if (user.getPermissions() == null) return true;
        String permStr = user.getPermissions().trim();
        // 空数组 "[]" = 无任何权限
        if (permStr.equals("[]")) return false;
        // JSON 数组格式：["description","copy"]
        if (permStr.startsWith("[")) {
            try {
                List<String> perms = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(permStr, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                return perms.contains(module);
            } catch (Exception e) {
                return false;
            }
        }
        // 旧格式：逗号分隔
        for (String perm : permStr.split(",")) {
            if (perm.trim().equals(module)) return true;
        }
        return false;
    }

    @Transactional
    public void consumeQuota(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) return;
        if (user.getQuotaTotal() == null || user.getQuotaTotal() == -1) return; // 不限
        user.setQuotaUsed((user.getQuotaUsed() != null ? user.getQuotaUsed() : 0) + 1);
        sysUserMapper.updateById(user);
    }

    // ==================== 注册限制 ====================

    public boolean isRegistrationFull() {
        long count = sysUserMapper.selectCount(null);
        return count >= MAX_USERS;
    }

    public long currentUserCount() {
        return sysUserMapper.selectCount(null);
    }

    // ==================== 系统公告 ====================

    public SystemAnnouncement getActiveAnnouncement() {
        List<SystemAnnouncement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<SystemAnnouncement>()
                        .eq(SystemAnnouncement::getIsActive, 1)
                        .orderByDesc(SystemAnnouncement::getCreatedAt)
                        .last("LIMIT 1")
        );
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    public SystemAnnouncement setAnnouncement(String content, Authentication auth) {
        // 先把旧公告失效
        List<SystemAnnouncement> activeList = announcementMapper.selectList(
                new LambdaQueryWrapper<SystemAnnouncement>().eq(SystemAnnouncement::getIsActive, 1)
        );
        for (SystemAnnouncement a : activeList) {
            a.setIsActive(0);
            announcementMapper.updateById(a);
        }

        SystemAnnouncement ann = new SystemAnnouncement();
        ann.setContent(content);
        ann.setCreatedBy((Long) auth.getPrincipal());
        ann.setIsActive(1);
        announcementMapper.insert(ann);
        return ann;
    }

    public SysUser getUserById(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        return user;
    }

    public List<UserQuotaLog> getQuotaLogs(Long userId) {
        return quotaLogMapper.selectList(
                new LambdaQueryWrapper<UserQuotaLog>()
                        .eq(UserQuotaLog::getUserId, userId)
                        .orderByDesc(UserQuotaLog::getCreatedAt)
        );
    }
}
