package com.gaebalhagae.family.controller;

import com.gaebalhagae.family.dto.FamilyRequests;
import com.gaebalhagae.family.dto.FamilyResponse;
import com.gaebalhagae.family.dto.InvitePreviewResponse;
import com.gaebalhagae.family.dto.InviteResponse;
import com.gaebalhagae.family.service.FamilyService;
import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    @GetMapping
    public ApiResponse<FamilyResponse> get(@AuthenticationPrincipal LoginMember me) {
        return ApiResponse.ok(familyService.getMyFamily(me.id()));
    }

    @PatchMapping
    public ApiResponse<FamilyResponse> rename(@AuthenticationPrincipal LoginMember me,
                                              @Valid @RequestBody FamilyRequests.Rename request) {
        return ApiResponse.ok(familyService.rename(me.id(), request.name()));
    }

    @GetMapping("/invite")
    public ApiResponse<InviteResponse> invite(@AuthenticationPrincipal LoginMember me) {
        return ApiResponse.ok(familyService.getInvite(me.id()));
    }

    @PostMapping("/invite/reset")
    public ApiResponse<InviteResponse> resetInvite(@AuthenticationPrincipal LoginMember me) {
        return ApiResponse.ok(familyService.resetInvite(me.id()));
    }

    @GetMapping("/invites/{code}")
    public ApiResponse<InvitePreviewResponse> preview(@PathVariable String code) {
        return ApiResponse.ok(familyService.preview(code));
    }

    @PostMapping("/invites/{code}/accept")
    public ApiResponse<FamilyResponse> accept(@AuthenticationPrincipal LoginMember me, @PathVariable String code) {
        return ApiResponse.ok(familyService.accept(me.id(), code));
    }

    @DeleteMapping("/members/{memberId}")
    public ApiResponse<Void> kick(@AuthenticationPrincipal LoginMember me, @PathVariable Long memberId) {
        familyService.kick(me.id(), memberId);
        return ApiResponse.ok();
    }

    @PostMapping("/owner")
    public ApiResponse<Void> transferOwner(@AuthenticationPrincipal LoginMember me,
                                           @Valid @RequestBody FamilyRequests.TransferOwner request) {
        familyService.transferOwner(me.id(), request.memberId());
        return ApiResponse.ok();
    }

    @PostMapping("/leave")
    public ApiResponse<Void> leave(@AuthenticationPrincipal LoginMember me) {
        familyService.leave(me.id());
        return ApiResponse.ok();
    }
}
