package com.mall.content.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.content.domain.entity.SysUser;
import com.mall.content.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 5 个固定管理员账号（不可注册，系统预置）。
     */
    private static final Object[][] ADMIN_ACCOUNTS = {
            {"linjun",       "test123456", "林俊"},
            {"huangjiarui",  "test123456", "黄嘉瑞"},
            {"zhouwang",     "test123456", "周旺"},
            {"luoyi",        "test123456", "罗艺"},
            {"jiangtianhao", "test123456", "江天豪"},
    };

    @Override
    public void run(String... args) {
        for (Object[] account : ADMIN_ACCOUNTS) {
            String username = (String) account[0];
            String password = (String) account[1];
            String nickname = (String) account[2];

            SysUser exists = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
            );
            if (exists == null) {
                SysUser admin = new SysUser();
                admin.setUsername(username);
                admin.setNickname(nickname);
                admin.setRole("ADMIN");
                admin.setStatus("ACTIVE");
                admin.setQuotaTotal(-1); // 管理员不限制
                admin.setQuotaUsed(0);
                admin.setPasswordHash(passwordEncoder.encode(password));
                sysUserMapper.insert(admin);
            }
        }
    }
}
