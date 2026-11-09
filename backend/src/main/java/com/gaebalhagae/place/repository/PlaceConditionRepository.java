package com.gaebalhagae.place.repository;

import com.gaebalhagae.place.domain.PlaceCondition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceConditionRepository extends JpaRepository<PlaceCondition, Long> {

    List<PlaceCondition> findByPlaceId(Long placeId);

    Optional<PlaceCondition> findByPlaceIdAndConditionKey(Long placeId, String conditionKey);
}
