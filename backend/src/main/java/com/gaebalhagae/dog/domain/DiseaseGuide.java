package com.gaebalhagae.dog.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 영양 카드 ⑤ 질병별 주의점 고정 데이터. disease_key 는 dog.diseases 의 키와 같음 */
@Getter
@Entity
@Table(name = "disease_guide")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiseaseGuide extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disease_key", nullable = false, unique = true, length = 20)
    private String diseaseKey;

    @Column(nullable = false, length = 20)
    private String label;

    @Column(name = "caution_text", nullable = false, length = 500)
    private String cautionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avoid_keywords")
    private List<String> avoidKeywords = new ArrayList<>();

    @Column(name = "source_url", length = 300)
    private String sourceUrl;

    public DiseaseGuide(String diseaseKey, String label, String cautionText, List<String> avoidKeywords,
                        String sourceUrl) {
        this.diseaseKey = diseaseKey;
        this.label = label;
        this.cautionText = cautionText;
        this.avoidKeywords = new ArrayList<>(avoidKeywords);
        this.sourceUrl = sourceUrl;
    }
}
