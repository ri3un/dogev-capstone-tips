package com.gaebalhagae.place.repository;

import com.gaebalhagae.place.domain.PlaceSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceSourceRepository extends JpaRepository<PlaceSource, Long> {

    List<PlaceSource> findByPlaceIdAndExcludedFalseOrderByIdAsc(Long placeId);

    @Modifying(flushAutomatically = true)
    @Query("delete from PlaceSource s where s.placeId = :placeId and s.sourceType = :type")
    void deleteByPlaceIdAndSourceType(@Param("placeId") Long placeId, @Param("type") String type);
}
