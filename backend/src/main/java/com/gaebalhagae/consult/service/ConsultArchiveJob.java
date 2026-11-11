package com.gaebalhagae.consult.service;

import com.gaebalhagae.consult.repository.ConsultationAnswerRepository;
import com.gaebalhagae.consult.repository.ConsultationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 상담 원문 보관 기간 정리 (항목 6, B안: LLM 없음). 보관 기간이 지나면 consultation_answer(모델별 원문)만 지우고
 * consultation 의 질문·긴급도·병원 권유·종합 3줄·사용한 기록은 남깁니다. 건강 기록은 건드리지 않습니다.
 * 장소 수집이 꺼진 환경에서도 돌도록 @EnableScheduling 을 따로 둡니다.
 */
@Slf4j
@Component
@EnableScheduling
public class ConsultArchiveJob {

    static final int BATCH = 100;

    private final ConsultationRepository consultations;
    private final ConsultationAnswerRepository answers;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int retentionDays;

    public ConsultArchiveJob(ConsultationRepository consultations, ConsultationAnswerRepository answers,
                             TransactionTemplate tx, Clock clock,
                             @Value("${consult.retention-days:90}") int retentionDays) {
        this.consultations = consultations;
        this.answers = answers;
        this.tx = tx;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    /** 매일 04:00 (장소 수집 03:00 과 겹치지 않게) */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void run() {
        int n = archiveOld();
        if (n > 0) {
            log.info("상담 {}건 원문 정리 ({}일 보관)", n, retentionDays);
        }
    }

    /** @return 이번에 정리한 상담 수 */
    public int archiveOld() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minusDays(retentionDays);
        int total = 0;
        while (true) {
            Integer done = tx.execute(status -> {
                List<Long> ids = consultations.findArchiveTargets(cutoff, PageRequest.of(0, BATCH));
                if (!ids.isEmpty()) {
                    answers.deleteByConsultationIdIn(ids);
                    consultations.findAllById(ids).forEach(c -> c.archive(now));
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
