package com.gaebalhagae.record.repository;

import com.gaebalhagae.record.domain.HealthRecord;
import com.gaebalhagae.record.domain.RecordCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    List<HealthRecord> findByDogIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(Long dogId, LocalDate from,
                                                                             LocalDate to);

    List<HealthRecord> findByDogIdAndCategoryAndRecordDateBetweenOrderByRecordDateAscIdAsc(
            Long dogId, RecordCategory category, LocalDate from, LocalDate to);

    Optional<HealthRecord> findTopByDogIdAndCategoryAndWeightKgNotNullOrderByRecordDateDescIdDesc(
            Long dogId, RecordCategory category);

    @Modifying
    @Query("delete from HealthRecord r where r.dogId = :dogId")
    void deleteByDogId(@Param("dogId") Long dogId);
}
