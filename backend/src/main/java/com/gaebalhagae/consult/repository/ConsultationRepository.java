package com.gaebalhagae.consult.repository;

import com.gaebalhagae.consult.domain.Consultation;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByDogIdOrderByIdDesc(Long dogId);

    /** 보관 기간이 지났는데 아직 정리 안 된 상담 id (항목 6) */
    @Query("select c.id from Consultation c where c.archivedAt is null and c.createdAt < :cutoff order by c.id")
    List<Long> findArchiveTargets(@Param("cutoff") LocalDateTime cutoff, Pageable page);

    @Modifying
    @Query("delete from Consultation c where c.dogId = :dogId")
    void deleteByDogId(@Param("dogId") Long dogId);
}
