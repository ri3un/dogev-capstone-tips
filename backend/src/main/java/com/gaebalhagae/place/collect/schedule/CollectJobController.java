package com.gaebalhagae.place.collect.schedule;

import com.gaebalhagae.global.response.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 매일 수집을 기다리지 않고 지금 한 번 돌리기 + 최근 실행 기록 (관리자만) */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "collect.schedule.enabled", havingValue = "true")
public class CollectJobController {

    private final DailyCollectJob job;
    private final CollectJobStore jobStore;

    /** 30곳 요약에 20분쯤 걸려서 바로 돌려주고 뒤에서 돌림 */
    @PostMapping("/api/admin/collect/run")
    public ApiResponse<Map<String, Boolean>> run() {
        if (job.isRunning()) {
            return ApiResponse.ok(Map.of("started", false));
        }
        new Thread(job::scheduled, "collect-now").start();
        return ApiResponse.ok(Map.of("started", true));
    }

    @GetMapping("/api/admin/collect/logs")
    public ApiResponse<List<CollectJobStore.RunLog>> logs(@RequestParam(defaultValue = "10") int n) {
        return ApiResponse.ok(jobStore.recentLogs(Math.min(Math.max(n, 1), 100)));
    }
}
