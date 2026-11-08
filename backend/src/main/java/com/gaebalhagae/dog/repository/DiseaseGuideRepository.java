package com.gaebalhagae.dog.repository;

import com.gaebalhagae.dog.domain.DiseaseGuide;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiseaseGuideRepository extends JpaRepository<DiseaseGuide, Long> {

    List<DiseaseGuide> findByDiseaseKeyIn(Collection<String> keys);
}
