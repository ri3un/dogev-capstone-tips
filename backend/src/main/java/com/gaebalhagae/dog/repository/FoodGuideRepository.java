package com.gaebalhagae.dog.repository;

import com.gaebalhagae.dog.domain.FoodGuide;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodGuideRepository extends JpaRepository<FoodGuide, Long> {

    List<FoodGuide> findAllByOrderByIdAsc();
}
