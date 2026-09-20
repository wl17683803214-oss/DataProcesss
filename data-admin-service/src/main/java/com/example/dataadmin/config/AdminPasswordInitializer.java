package com.example.dataadmin.config;

import com.example.dataadmin.entity.SysUser;
import com.example.dataadmin.mapper.SysUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** 首次启动时把 SQL 中的初始化密码立即转换成 BCrypt 密文。 */
@Component
public class AdminPasswordInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminPasswordInitializer(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 仅处理带有 {init} 标识的初始密码，正常密文不会重复修改。 */
    @Override
    public void run(ApplicationArguments arguments) {
        SysUser administrator = userMapper.findByUsername("admin");
        if (administrator == null || administrator.getPassword() == null) {
            return;
        }
        if (!administrator.getPassword().startsWith("{init}")) {
            return;
        }

        String rawPassword = administrator.getPassword().substring(6);
        String encodedPassword = passwordEncoder.encode(rawPassword);
        userMapper.resetPassword(administrator.getId(), encodedPassword);
    }
}
