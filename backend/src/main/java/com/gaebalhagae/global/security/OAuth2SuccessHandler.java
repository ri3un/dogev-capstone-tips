package com.gaebalhagae.global.security;

import com.gaebalhagae.member.domain.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/** SNS 로그인 성공 → FRONT_URL/oauth/callback#token=JWT 로 보냅니다(#뒤는 서버 로그에 남지 않음) */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final String frontUrl;

    public OAuth2SuccessHandler(JwtProvider jwtProvider, @Value("${app.front-url}") String frontUrl) {
        this.jwtProvider = jwtProvider;
        this.frontUrl = frontUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        Long memberId = ((Number) user.getAttribute(CustomOAuth2UserService.MEMBER_ID)).longValue();
        Role role = Role.valueOf(user.getAttribute(CustomOAuth2UserService.ROLE));
        response.sendRedirect(frontUrl + "/oauth/callback#token=" + jwtProvider.issue(memberId, role));
    }
}
