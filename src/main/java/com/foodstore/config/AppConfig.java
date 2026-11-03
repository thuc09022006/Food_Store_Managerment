package com.foodstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Cấu hình dùng chung. Chỉ owner Foundation sửa file này. */
@Configuration
public class AppConfig {

    /** Băm mật khẩu bằng BCrypt (chỉ dùng spring-security-crypto, không cần Spring Security đầy đủ). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
