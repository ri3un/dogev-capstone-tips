package com.gaebalhagae.place.collect;

import com.gaebalhagae.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 노트북에서만 쓰는 장소 목록 수집 API. collect 프로필에서만 켜집니다.
 * 여기서 모은 장소 이름으로 /api/collect/blog 를 돌립니다.
 */
@Profile("collect")
@RestController
@RequestMapping("/api/collect/place")
@RequiredArgsConstructor
public class PlaceCollectController {

    private final PlaceCollectService placeCollectService;

    /** area, addressPrefix 를 비우면 안양 기준으로 검색합니다. */
    public record CollectRequest(String area, String addressPrefix, @NotEmpty List<String> keywords) {
    }

    @PostMapping
    public ApiResponse<PlaceCollectService.CollectResult> collect(@Valid @RequestBody CollectRequest request) {
        String area = request.area() == null || request.area().isBlank() ? "안양" : request.area();
        String prefix = request.addressPrefix() == null || request.addressPrefix().isBlank()
                ? "경기 안양시" : request.addressPrefix();
        return ApiResponse.ok(placeCollectService.collect(area, prefix, request.keywords()));
    }

    @GetMapping
    public ApiResponse<List<CollectedPlace>> findAll() {
        return ApiResponse.ok(placeCollectService.findAll());
    }

    /** 블로그 수집 요청에 바로 붙여 넣을 수 있게 이름만 돌려줍니다. */
    @GetMapping("/names")
    public ApiResponse<List<String>> names() {
        return ApiResponse.ok(placeCollectService.findAll().stream().map(CollectedPlace::name).distinct().toList());
    }
}
