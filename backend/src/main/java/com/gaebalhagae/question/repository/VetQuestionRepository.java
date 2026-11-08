package com.gaebalhagae.question.repository;

import com.gaebalhagae.question.domain.VetQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VetQuestionRepository extends JpaRepository<VetQuestion, Long> {

    List<VetQuestion> findByDogIdOrderByIdDesc(Long dogId);

    List<VetQuestion> findByDogIdAndResolvedOrderByIdDesc(Long dogId, boolean resolved);

    @Modifying
    @Query("delete from VetQuestion q where q.dogId = :dogId")
    void deleteByDogId(@Param("dogId") Long dogId);
}
