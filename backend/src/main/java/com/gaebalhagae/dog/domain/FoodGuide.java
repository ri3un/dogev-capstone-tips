package com.gaebalhagae.dog.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 영양 카드 고정 데이터: 위험·주의·안전 음식 (결정 11, LLM 미사용) */
@Getter
@Entity
@Table(name = "food_guide")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoodGuide extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FoodType type;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "how_to_feed", length = 300)
    private String howToFeed;

    @Column(name = "source_url", length = 300)
    private String sourceUrl;

    /** 이 음식을 조금만 줘야 하는 지병 키(DiseaseGuide.diseaseKey), 콤마 구분. 예: "OBESITY" */
    @Column(name = "avoid_diseases", length = 100)
    private String avoidDiseases;

    public FoodGuide(String name, FoodType type, String reason, String howToFeed, String sourceUrl) {
        this.name = name;
        this.type = type;
        this.reason = reason;
        this.howToFeed = howToFeed;
        this.sourceUrl = sourceUrl;
    }

    public FoodGuide avoidFor(String diseaseKeys) {
        this.avoidDiseases = diseaseKeys;
        return this;
    }

    public List<String> avoidDiseaseKeys() {
        return avoidDiseases == null || avoidDiseases.isBlank() ? List.of() : List.of(avoidDiseases.split(","));
    }
}
