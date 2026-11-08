package com.gaebalhagae.member.controller;

import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import com.gaebalhagae.member.dto.MeResponse;
import com.gaebalhagae.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/api/members/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal LoginMember me) {
        return ApiResponse.ok(memberService.me(me.id()));
    }
}
