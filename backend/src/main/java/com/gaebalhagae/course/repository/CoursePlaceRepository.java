package com.gaebalhagae.course.repository;

import com.gaebalhagae.course.domain.CoursePlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

    List<CoursePlace> findByCourseIdOrderBySeqAsc(Long courseId);

    @Modifying
    @Query("delete from CoursePlace p where p.courseId = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    @Modifying
    @Query("delete from CoursePlace p where p.courseId in (select c.id from Course c where c.dogId = :dogId)")
    void deleteByDogId(@Param("dogId") Long dogId);
}
