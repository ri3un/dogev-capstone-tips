package com.gaebalhagae.place.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 요약에 쓴 글의 출처 링크. 원문은 저장하지 않고 판정만 남깁니다 (결정 30) */
@Getter
@Entity
@Table(name = "place_source", indexes = @Index(columnList = "place_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceSource extends BaseTimeEntity {

    public static final String BLOG = "BLOG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "source_type", nullable = false, length = 10)
    private String sourceType;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 200)
    private String title;

    @Column(name = "snippet_hash", length = 64)
    private String snippetHash;

    @Column(name = "published_at")
    private LocalDate publishedAt;

    @Column(name = "judgement_json", columnDefinition = "text")
    private String judgementJson;

    @Column(nullable = false)
    private boolean excluded;

    public PlaceSource(Long placeId, String sourceType, String url) {
        this.placeId = placeId;
        this.sourceType = sourceType;
        this.url = url;
    }
}
