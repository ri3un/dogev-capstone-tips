package com.gaebalhagae.course.service;

import com.gaebalhagae.course.repository.CoursePlaceRepository;
import com.gaebalhagae.course.repository.CourseRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 임시 코스 정리. 추천만 받고 저장도 다녀오지도 않은 코스는 "저장한 코스"에 안 보이므로 하루 뒤 지웁니다.
 * 보고 있는 코스가 사라지지 않게 하루를 기다립니다. 활동 기록은 다녀온 코스에만 있어 건드리지 않습니다.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CourseDraftCleanupJob {

    static final int BATCH = 100;

    private final CourseRepository courses;
    private final CoursePlaceRepository coursePlaces;
    private final TransactionTemplate tx;
    private final Clock clock;

    /** 매일 04:30 (상담 정리 04:00 과 겹치지 않게) */
    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void run() {
        int n = cleanOld();
        if (n > 0) {
            log.info("임시 코스 {}건 정리", n);
        }
    }

    /** @return 이번에 지운 코스 수 */
    public int cleanOld() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(1);
        int total = 0;
        while (true) {
            Integer done = tx.execute(status -> {
                List<Long> ids = courses.findDraftIdsBefore(cutoff, PageRequest.of(0, BATCH));
                if (!ids.isEmpty()) {
                    coursePlaces.deleteByCourseIdIn(ids);
                    courses.deleteAllByIdInBatch(ids);
                }
                return ids.size();
            });
            total += done;
            if (done < BATCH) {
                return total;
            }
        }
    }
}
