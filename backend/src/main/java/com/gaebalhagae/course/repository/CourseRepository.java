package com.gaebalhagae.course.repository;

import com.gaebalhagae.course.domain.Course;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /** 저장한 코스: 저장했거나 다녀온 코스만 (다녀오면 저장도 됨) */
    List<Course> findByDogIdAndSavedAtIsNotNullOrderByIdDesc(Long dogId);

    /** "다녀왔어요" 두 번 눌림 방지: 같은 코스는 한 요청씩 처리 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    /** 정리 대상: 저장도 안 하고 다녀오지도 않은 채 cutoff 전에 만든 임시 코스 */
    @Query("select c.id from Course c where c.savedAt is null and c.visitedAt is null and c.createdAt < :cutoff"
            + " order by c.id")
    List<Long> findDraftIdsBefore(@Param("cutoff") LocalDateTime cutoff, Pageable page);

    @Modifying
    @Query("delete from Course c where c.dogId = :dogId")
    void deleteByDogId(@Param("dogId") Long dogId);
}
