package com.gaebalhagae.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.response.ErrorCode;
import com.gaebalhagae.global.security.CustomOAuth2UserService;
import com.gaebalhagae.global.security.JwtAuthFilter;
import com.gaebalhagae.global.security.JwtProvider;
import com.gaebalhagae.global.security.OAuth2SuccessHandler;
import com.gaebalhagae.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 카카오·네이버 OAuth2 로그인 → 서버 JWT 발급, 이후 요청은 Bearer 토큰으로 인증합니다. (결정 3)
 * 기본은 로그인 필요, /api/admin/** 은 ADMIN 만.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.front-url}")
    private String frontUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/oauth2/**", "/login/oauth2/**", "/uploads/**", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2Login(o -> o
                        .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((req, res, ex) -> res.sendRedirect(frontUrl + "/login?error=oauth")))
                .addFilterBefore(new JwtAuthFilter(jwtProvider, memberRepository::existsByIdAndDeletedAtIsNull), UsernamePasswordAuthenticationFilter.class)
                // 401·403도 필터 단계에서 공통 응답 형식으로 내려줍니다.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> writeError(res, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> writeError(res, ErrorCode.FORBIDDEN)));
        return http.build();
    }

    /** 개발 중 Vite(5173)에서 부를 수 있게 프론트 주소만 허용. 배포는 Nginx 같은 주소라 CORS 불필요 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code));
    }
}
