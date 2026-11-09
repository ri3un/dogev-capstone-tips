package com.gaebalhagae.place.domain;

import com.gaebalhagae.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 반려견 동반 장소. 리뷰 요약·키워드는 수집 때 미리 만들어 둡니다(화면을 열 때 LLM 을 부르지 않음) */
@Getter
@Entity
@Table(name = "place", indexes = @Index(columnList = "lat, lng"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PlaceCategory category;

    @Column(name = "category_name", length = 100)
    private String categoryName;

    @Column(length = 200)
    private String address;

    @Column(name = "road_address", length = 200)
    private String roadAddress;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(name = "tour_content_id", length = 30)
    private String tourContentId;

    @Column(name = "kakao_place_id", unique = true, length = 30)
    private String kakaoPlaceId;

    @Column(length = 30)
    private String phone;

    @Column(name = "place_url", length = 300)
    private String placeUrl;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(name = "last_collected_at")
    private LocalDateTime lastCollectedAt;

    /** 3줄 요약 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "review_summary")
    private List<String> reviewSummary = new ArrayList<>();

    /** 고정 키워드 + 분위기 태그, 최대 5개 (결정 33) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "review_keywords")
    private List<String> reviewKeywords = new ArrayList<>();

    @Column(name = "review_source_count", nullable = false)
    private int reviewSourceCount;

    @Column(name = "review_summarized_at")
    private LocalDateTime reviewSummarizedAt;

    public Place(String kakaoPlaceId, String name) {
        this.kakaoPlaceId = kakaoPlaceId;
        this.name = name;
        this.category = PlaceCategory.ETC;
    }

    public void updateBasic(String name, PlaceCategory category, String categoryName, String address,
                            String roadAddress, double lat, double lng, String phone, String placeUrl,
                            LocalDateTime collectedAt) {
        this.name = name;
        this.category = category;
        this.categoryName = categoryName;
        this.address = address;
        this.roadAddress = roadAddress;
        this.lat = lat;
        this.lng = lng;
        this.phone = phone;
        this.placeUrl = placeUrl;
        this.lastCollectedAt = collectedAt;
    }

    public void updateReview(List<String> summary, int sourceCount, LocalDateTime summarizedAt) {
        this.reviewSummary = summary == null ? new ArrayList<>() : new ArrayList<>(summary);
        this.reviewSourceCount = sourceCount;
        this.reviewSummarizedAt = summarizedAt;
    }

    public void changeKeywords(List<String> keywords) {
        this.reviewKeywords = new ArrayList<>(keywords);
    }
}
