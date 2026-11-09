package com.gaebalhagae.place.controller;

import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import com.gaebalhagae.place.domain.EditRequestStatus;
import com.gaebalhagae.place.dto.EditRequestDtos;
import com.gaebalhagae.place.dto.PlaceImportRequest;
import com.gaebalhagae.place.service.EditRequestService;
import com.gaebalhagae.place.service.PlaceImportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 장소 수정 요청(회원)과 관리자 기능. /api/admin/** 은 SecurityConfig 에서 ADMIN 만 통과 */
@RestController
@RequiredArgsConstructor
public class EditRequestController {

    private final EditRequestService editRequestService;
    private final PlaceImportService placeImportService;

    @PostMapping("/api/places/{placeId}/edit-requests")
    public ApiResponse<EditRequestDtos.Response> create(@AuthenticationPrincipal LoginMember me,
                                                        @PathVariable Long placeId,
                                                        @Valid @RequestBody EditRequestDtos.Create request) {
        return ApiResponse.ok(editRequestService.create(me.id(), placeId, request));
    }

    @GetMapping("/api/me/edit-requests")
    public ApiResponse<List<EditRequestDtos.Response>> mine(@AuthenticationPrincipal LoginMember me) {
        return ApiResponse.ok(editRequestService.mine(me.id()));
    }

    @GetMapping("/api/admin/edit-requests")
    public ApiResponse<List<EditRequestDtos.Response>> adminList(
            @RequestParam(required = false) EditRequestStatus status) {
        return ApiResponse.ok(editRequestService.forAdmin(status));
    }

    @PostMapping("/api/admin/edit-requests/{id}/approve")
    public ApiResponse<EditRequestDtos.Response> approve(@PathVariable Long id) {
        return ApiResponse.ok(editRequestService.approve(id));
    }

    @PostMapping("/api/admin/edit-requests/{id}/reject")
    public ApiResponse<EditRequestDtos.Response> reject(@PathVariable Long id,
                                                        @Valid @RequestBody EditRequestDtos.Reject request) {
        return ApiResponse.ok(editRequestService.reject(id, request.reason()));
    }

    /** 노트북 수집 JSON 을 DB 에 적재 (결정 32) */
    @PostMapping("/api/admin/places/import")
    public ApiResponse<PlaceImportRequest.Result> importPlaces(@Valid @RequestBody PlaceImportRequest request) {
        return ApiResponse.ok(placeImportService.importData(request));
    }
}
