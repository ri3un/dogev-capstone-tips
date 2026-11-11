package com.gaebalhagae.admin.controller;

import com.gaebalhagae.admin.dto.AdminMemberDtos.MemberDetail;
import com.gaebalhagae.admin.dto.AdminMemberDtos.MemberList;
import com.gaebalhagae.admin.service.AdminMemberService;
import com.gaebalhagae.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 회원 현황. /api/admin/** 은 SecurityConfig 에서 ADMIN 만 통과 */
@RestController
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping("/api/admin/members")
    public ApiResponse<MemberList> list() {
        return ApiResponse.ok(adminMemberService.list());
    }

    @GetMapping("/api/admin/members/{memberId}")
    public ApiResponse<MemberDetail> detail(@PathVariable Long memberId) {
        return ApiResponse.ok(adminMemberService.detail(memberId));
    }
}
