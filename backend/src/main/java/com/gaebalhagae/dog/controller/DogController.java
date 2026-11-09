package com.gaebalhagae.dog.controller;

import com.gaebalhagae.dog.dto.DogRequest;
import com.gaebalhagae.dog.dto.DogResponse;
import com.gaebalhagae.dog.service.DogService;
import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.security.LoginMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/dogs")
@RequiredArgsConstructor
public class DogController {

    private final DogService dogService;

    @GetMapping
    public ApiResponse<List<DogResponse>> list(@AuthenticationPrincipal LoginMember me) {
        return ApiResponse.ok(dogService.list(me.id()));
    }

    @PostMapping
    public ApiResponse<DogResponse> create(@AuthenticationPrincipal LoginMember me,
                                           @Valid @RequestBody DogRequest request) {
        return ApiResponse.ok(dogService.create(me.id(), request));
    }

    @GetMapping("/{dogId}")
    public ApiResponse<DogResponse> get(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId) {
        return ApiResponse.ok(dogService.get(me.id(), dogId));
    }

    @PatchMapping("/{dogId}")
    public ApiResponse<DogResponse> update(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
                                           @Valid @RequestBody DogRequest request) {
        return ApiResponse.ok(dogService.update(me.id(), dogId, request));
    }

    @DeleteMapping("/{dogId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId) {
        dogService.delete(me.id(), dogId);
        return ApiResponse.ok();
    }

    /** 반려견 사진 (multipart, 필드 이름 file) */
    @PostMapping(value = "/{dogId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DogResponse> photo(@AuthenticationPrincipal LoginMember me, @PathVariable Long dogId,
                                          @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(dogService.changePhoto(me.id(), dogId, file));
    }
}
