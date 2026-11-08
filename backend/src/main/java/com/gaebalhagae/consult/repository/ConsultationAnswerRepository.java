package com.gaebalhagae.consult.repository;

import com.gaebalhagae.consult.domain.ConsultationAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultationAnswerRepository extends JpaRepository<ConsultationAnswer, Long> {

    List<ConsultationAnswer> findByConsultationIdOrderByIdAsc(Long consultationId);

    @Modifying
    @Query("delete from ConsultationAnswer a where a.consultationId in "
            + "(select c.id from Consultation c where c.dogId = :dogId)")
    void deleteByDogId(@Param("dogId") Long dogId);
}
