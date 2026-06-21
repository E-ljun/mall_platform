package com.mall.content.controller;

import com.mall.content.common.ApiResponse;
import com.mall.content.domain.entity.SysUser;
import com.mall.content.mapper.SysUserMapper;
import com.mall.content.security.JwtTokenProvider;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String oldPassword;
        @NotBlank
        private String newPassword;
    }

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final com.mall.content.service.ProductService productService;
    private final com.mall.content.mapper.ProductMapper productMapper;
    private final com.mall.content.mapper.ProductImageMapper productImageMapper;
    private final com.mall.content.service.StorageService storageService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername())
        );
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.fail("AUTH_FAILED", "用户名或密码错误");
        }
        if ("PENDING".equals(user.getStatus())) {
            return ApiResponse.fail("AUTH_FAILED", "账号尚未通过审核，请等待管理员审核");
        }
        if ("DISABLED".equals(user.getStatus())) {
            return ApiResponse.fail("AUTH_FAILED", "账号已被禁用，请联系管理员");
        }
        // 管理员入口校验：adminLogin=true 只允许管理员
        if (request.getAdminLogin() != null && request.getAdminLogin() && !"ADMIN".equals(user.getRole())) {
            return ApiResponse.fail("AUTH_FAILED", "普通用户请使用用户入口登录");
        }
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getRole());
        return ApiResponse.ok(Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "nickname", user.getNickname(),
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "quotaTotal", user.getQuotaTotal() != null ? user.getQuotaTotal() : 0,
                "quotaUsed", user.getQuotaUsed() != null ? user.getQuotaUsed() : 0
        ));
    }

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        // 检查注册上限
        long userCount = sysUserMapper.selectCount(null);
        if (userCount >= 50) {
            return ApiResponse.fail("FULL", "系统用户已达上限(50人)，暂时无法注册");
        }
        SysUser exists = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername())
        );
        if (exists != null) {
            return ApiResponse.fail("USER_EXISTS", "用户名已存在");
        }
        if (request.getPassword().length() < 6) {
            return ApiResponse.fail("WEAK_PASSWORD", "密码至少6位");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole("USER");
        user.setStatus("PENDING"); // 默认待审核
        user.setQuotaTotal(20);
        user.setQuotaUsed(0);
        sysUserMapper.insert(user);
        return ApiResponse.ok(Map.of(
                "userId", user.getId(),
                "message", "注册成功！请等待管理员审核通过后登录"
        ));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ApiResponse.fail("AUTH_FAILED", "当前密码错误");
        }
        if (request.getNewPassword().length() < 6) {
            return ApiResponse.fail("WEAK_PASSWORD", "新密码至少6位");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail("NOT_FOUND", "用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            return ApiResponse.fail("FORBIDDEN", "管理员账号不可注销");
        }
        // 级联删除用户的所有商品（包括图片）
        var products = productMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.content.domain.entity.Product>()
                        .eq(com.mall.content.domain.entity.Product::getUserId, userId)
        );
        for (var product : products) {
            var images = productImageMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.content.domain.entity.ProductImage>()
                            .eq(com.mall.content.domain.entity.ProductImage::getProductId, product.getId())
            );
            for (var image : images) {
                storageService.deleteFile(image.getFilePath());
            }
            storageService.deleteProductDirectory(product.getId());
            productImageMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.mall.content.domain.entity.ProductImage>()
                    .eq(com.mall.content.domain.entity.ProductImage::getProductId, product.getId()));
            productMapper.deleteById(product.getId());
        }
        sysUserMapper.deleteById(userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/profile")
    public ApiResponse<java.util.Map<String, Object>> profile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return ApiResponse.fail("NOT_FOUND", "用户不存在");
        }
        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
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
        return ApiResponse.ok(info);
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateProfile(@RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) return ApiResponse.fail("NOT_FOUND", "用户不存在");
        if (body.containsKey("nickname")) user.setNickname(body.get("nickname"));
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        if (body.containsKey("email")) user.setEmail(body.get("email"));
        sysUserMapper.updateById(user);
        return ApiResponse.ok(null);
    }

    @PostMapping("/avatar")
    public ApiResponse<String> uploadAvatar(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            Authentication auth
    ) throws Exception {
        if (file.isEmpty()) {
            return ApiResponse.fail("BAD_REQUEST", "文件为空");
        }
        String mime = file.getContentType();
        if (mime == null || (!mime.equals("image/jpeg") && !mime.equals("image/png") && !mime.equals("image/webp"))) {
            return ApiResponse.fail("BAD_REQUEST", "仅支持 JPG/PNG/WebP 图片");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return ApiResponse.fail("BAD_REQUEST", "头像不能超过 2MB");
        }
        Long userId = (Long) auth.getPrincipal();
        SysUser user = sysUserMapper.selectById(userId);
        String savedPath = storageService.saveGeneratedImage(userId, file.getBytes(),
                "avatar_" + userId + "_" + System.currentTimeMillis() + ".png");
        String publicUrl = storageService.toPublicUrl(savedPath);
        user.setAvatar(publicUrl);
        sysUserMapper.updateById(user);
        return ApiResponse.ok(publicUrl);
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        private Boolean adminLogin; // true=管理员入口，false/null=普通用户入口
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
        @NotBlank
        private String nickname;
    }
}
