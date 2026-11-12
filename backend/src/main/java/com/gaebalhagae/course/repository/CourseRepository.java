package com.gaebalhagae.course.repository;

import com.gaebalhagae.course.domain.Course;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByDogIdOrderByIdDesc(Long dogId);

    /** "다녀왔어요" 두 번 눌림 방지: 같은 코스는 한 요청씩 처리 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("delete from Course c where c.dogId = :dogId")
    void deleteByDogId(@Param("dogId") Long dogId);
}
