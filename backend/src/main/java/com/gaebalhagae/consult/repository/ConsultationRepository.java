package com.gaebalhagae.consult.repository;

import com.gaebalhagae.consult.domain.Consultation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByDogIdOrderByIdDesc(Long dogId);

    @Modifying
    @Query("delete from Consultation c where c.dogId = :dogId")
    void deleteByDogId(@Param("dogId") Long dogId);
}
