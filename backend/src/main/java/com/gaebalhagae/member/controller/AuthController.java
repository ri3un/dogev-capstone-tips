package com.gaebalhagae.member.controller;

import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.JwtProvider;
import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.domain.Provider;
import com.gaebalhagae.member.dto.DevLoginRequest;
import com.gaebalhagae.member.dto.LoginResponse;
import com.gaebalhagae.member.dto.MeResponse;
import com.gaebalhagae.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발용 로그인: SNS 키 없이 닉네임만으로 토큰을 받습니다. local·test 프로필에만 있고 prod 에는 없습니다.
 */
@RestController
@Profile({"local", "test"})
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final JwtProvider jwtProvider;

    @PostMapping("/api/auth/dev-login")
    @Transactional
    public ApiResponse<LoginResponse> devLogin(@Valid @RequestBody DevLoginRequest request) {
        Member member = memberService.login(Provider.DEV, "dev-" + request.nickname(), request.nickname());
        if (request.admin()) {
            member.makeAdmin();
        }
        String token = jwtProvider.issue(member.getId(), member.getRole());
        return ApiResponse.ok(new LoginResponse(token, MeResponse.from(member)));
    }
}
