package com.gaebalhagae.place.collect;

import com.gaebalhagae.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 노트북에서만 쓰는 수집용 API. collect 프로필에서만 켜집니다.
 * 나중에 관리자 화면의 "다시 수집" 버튼(/api/admin/...)으로 옮깁니다.
 */
@Profile("collect")
@RestController
@RequestMapping("/api/collect/blog")
@RequiredArgsConstructor
public class BlogCollectController {

    private final BlogCollectService blogCollectService;

    public record CollectRequest(@NotEmpty List<String> places) {
    }

    /** 장소 여러 곳을 차례로 수집합니다. API 한도를 지키려고 장소 사이에 1초 쉽니다. */
    @PostMapping
    public ApiResponse<List<BlogCollectService.CollectResult>> collect(@Valid @RequestBody CollectRequest request)
            throws InterruptedException {
        List<BlogCollectService.CollectResult> results = new ArrayList<>();
        for (String place : request.places()) {
            results.add(blogCollectService.collect(place.trim()));
            Thread.sleep(1000);
        }
        return ApiResponse.ok(results);
    }

    @GetMapping
    public ApiResponse<List<BlogPost>> find(@RequestParam String place) {
        return ApiResponse.ok(blogCollectService.findByPlace(place));
    }
}
