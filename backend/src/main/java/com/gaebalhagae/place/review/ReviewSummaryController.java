package com.gaebalhagae.place.review;

import com.gaebalhagae.global.response.ApiResponse;
import com.gaebalhagae.global.response.BusinessException;
import com.gaebalhagae.global.response.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 노트북에서만 쓰는 요약용 API. collect 프로필에서만 켜집니다.
 * 먼저 /api/collect/blog 로 글을 모은 장소만 요약할 수 있습니다.
 */
@Profile("collect")
@RestController
@RequestMapping("/api/collect/summary")
public class ReviewSummaryController {

    private final ReviewSummaryService reviewSummaryService;
    private final long delayMs;

    public ReviewSummaryController(ReviewSummaryService reviewSummaryService,
                                   @Value("${collect.summary-delay-ms:20000}") long delayMs) {
        this.reviewSummaryService = reviewSummaryService;
        this.delayMs = delayMs;
    }

    public record SummaryRequest(@NotEmpty List<String> places, boolean force) {
    }

    /** Groq 분당 토큰 한도 때문에 LLM을 부른 뒤에는 장소 사이에 delayMs(기본 20초) 쉽니다. */
    @PostMapping
    public ApiResponse<List<ReviewSummaryService.SummaryResult>> summarize(@Valid @RequestBody SummaryRequest request)
            throws InterruptedException {
        List<ReviewSummaryService.SummaryResult> results = new ArrayList<>();
        for (String place : request.places()) {
            ReviewSummaryService.SummaryResult result = reviewSummaryService.summarize(place.trim(), request.force());
            results.add(result);
            if ("SUMMARIZED".equals(result.status()) && results.size() < request.places().size()) {
                Thread.sleep(delayMs);
            }
        }
        return ApiResponse.ok(results);
    }

    @GetMapping
    public ApiResponse<ReviewSummary> find(@RequestParam String place) {
        return ApiResponse.ok(reviewSummaryService.findByPlace(place)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
    }
}
