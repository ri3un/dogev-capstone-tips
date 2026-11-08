package com.gaebalhagae.place.repository;

import com.gaebalhagae.place.domain.Place;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

    Optional<Place> findFirstByName(String name);
}
