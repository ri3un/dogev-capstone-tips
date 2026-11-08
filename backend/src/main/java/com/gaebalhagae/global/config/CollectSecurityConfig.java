package com.gaebalhagae.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * collect 프로필(노트북 수집용)에서만 /api/collect/** 를 로그인 없이 엽니다.
 * 다른 요청은 SecurityConfig 의 기본 규칙을 그대로 따릅니다.
 */
@Configuration
@Profile("collect")
public class CollectSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain collectFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/collect/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
