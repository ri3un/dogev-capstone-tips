package com.gaebalhagae.consult;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.consult.domain.Consultation;
import com.gaebalhagae.consult.domain.ConsultationAnswer;
import com.gaebalhagae.consult.domain.Urgency;
import com.gaebalhagae.consult.repository.ConsultationAnswerRepository;
import com.gaebalhagae.consult.repository.ConsultationRepository;
import com.gaebalhagae.consult.service.ConsultArchiveJob;
import com.gaebalhagae.llm.LlmParallelCaller.Status;
import com.gaebalhagae.support.ApiTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ConsultArchiveJobTest extends ApiTestSupport {

    @Autowired
    private ConsultArchiveJob job;
    @Autowired
    private ConsultationRepository consultations;
    @Autowired
    private ConsultationAnswerRepository answers;
    @Autowired
    private JdbcTemplate jdbc;

    private Long consult(long dogId, int daysAgo) {
        Consultation c = consultations.save(new Consultation(dogId, 1L, "토했어요", false, 1, Urgency.LOW, false,
                false, List.of("종합 1", "종합 2", "종합 3"), List.of(), List.of()));
        answers.save(new ConsultationAnswer(c.getId(), "gemini/test", Status.OK, "{\"answer\":\"원문\"}", 100));
        consultations.flush();
        jdbc.update("update consultation set created_at = ? where id = ?",
                LocalDateTime.now().minusDays(daysAgo), c.getId());
        return c.getId();
    }

    @Test
    void 구십일_지난_상담은_원문만_지우고_요약은_남긴다() throws Exception {
        long dogId = createDog(login("다인"), "콩이");
        Long recent = consult(dogId, 89);
        Long old = consult(dogId, 91);
        Long done = consult(dogId, 91);
        jdbc.update("update consultation set archived_at = ? where id = ?", LocalDateTime.now(), done);
        Integer recordsBefore = jdbc.queryForObject("select count(*) from health_record", Integer.class);

        assertThat(job.archiveOld()).isEqualTo(1);

        assertThat(answers.findByConsultationIdOrderByIdAsc(old)).isEmpty();
        assertThat(answers.findByConsultationIdOrderByIdAsc(recent)).hasSize(1);
        assertThat(answers.findByConsultationIdOrderByIdAsc(done)).hasSize(1);
        Consultation archived = consultations.findById(old).orElseThrow();
        assertThat(archived.getArchivedAt()).isNotNull();
        assertThat(archived.getSummary()).hasSize(3);
        assertThat(jdbc.queryForObject("select count(*) from health_record", Integer.class)).isEqualTo(recordsBefore);
    }
}
