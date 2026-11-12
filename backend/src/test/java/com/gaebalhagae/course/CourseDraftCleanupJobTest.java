package com.gaebalhagae.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.gaebalhagae.course.domain.Course;
import com.gaebalhagae.course.domain.CoursePlace;
import com.gaebalhagae.course.repository.CoursePlaceRepository;
import com.gaebalhagae.course.repository.CourseRepository;
import com.gaebalhagae.course.service.CourseDraftCleanupJob;
import com.gaebalhagae.support.ApiTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CourseDraftCleanupJobTest extends ApiTestSupport {

    @Autowired
    private CourseDraftCleanupJob job;
    @Autowired
    private CourseRepository courses;
    @Autowired
    private CoursePlaceRepository coursePlaces;
    @Autowired
    private JdbcTemplate jdbc;

    private Long course(long dogId, int hoursAgo, boolean saved, boolean visited) {
        Course c = new Course(1L, dogId, 100, 300, 5, null);
        if (saved) {
            c.markSaved(LocalDateTime.now());
        }
        if (visited) {
            c.markVisited(LocalDateTime.now());
        }
        c = courses.save(c);
        coursePlaces.save(new CoursePlace(c.getId(), 1L, 1, 100, 2));
        courses.flush();
        jdbc.update("update course set created_at = ? where id = ?", LocalDateTime.now().minusHours(hoursAgo),
                c.getId());
        return c.getId();
    }

    @Test
    void 하루_지난_임시_코스만_장소와_함께_지운다() throws Exception {
        long dogId = createDog(login("다인"), "콩이");
        Long oldDraft = course(dogId, 25, false, false);
        Long newDraft = course(dogId, 23, false, false);
        Long oldSaved = course(dogId, 25, true, false);
        Long oldVisited = course(dogId, 25, false, true);

        assertThat(job.cleanOld()).isEqualTo(1);

        assertThat(courses.existsById(oldDraft)).isFalse();
        assertThat(coursePlaces.findByCourseIdOrderBySeqAsc(oldDraft)).isEmpty();
        assertThat(courses.existsById(newDraft)).isTrue();
        assertThat(courses.existsById(oldSaved)).isTrue();
        assertThat(courses.existsById(oldVisited)).isTrue();
        assertThat(coursePlaces.findByCourseIdOrderBySeqAsc(oldSaved)).hasSize(1);
    }
}
