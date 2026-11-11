package com.gaebalhagae.dog.domain;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 반려견 프로필. 주인은 가족 그룹(family_group_id), 등록한 사람은 created_by (결정 6·7·26) */
@Getter
@Entity
@Table(name = "dog", indexes = @Index(columnList = "family_group_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_group_id", nullable = false)
    private Long familyGroupId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 50)
    private String breed;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    /** "2023-05" */
    @Column(name = "birth_ym", nullable = false, length = 7)
    private String birthYm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sex sex;

    @Column(nullable = false)
    private boolean neutered;

    @Column(name = "has_carrier", nullable = false)
    private boolean hasCarrier;

    @Column(name = "has_stroller", nullable = false)
    private boolean hasStroller;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", length = 10)
    private ActivityLevel activityLevel;

    /** 지병. 영양 카드·코스 규칙이 읽는 키: KIDNEY, PANCREATITIS, OBESITY, JOINT, SKIN (자유 입력도 허용) */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> diseases = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> allergies = new ArrayList<>();

    @Column(name = "food_kcal_per_kg")
    private Integer foodKcalPerKg;

    @Column(name = "photo_url", length = 300)
    private String photoUrl;

    /** 등록 화면의 건강 메모(자유 입력) */
    @Column(length = 300)
    private String memo;

    public Dog(Long familyGroupId, Long createdBy, Profile profile) {
        this.familyGroupId = familyGroupId;
        this.createdBy = createdBy;
        update(profile);
    }

    /** 등록·수정 화면에서 받는 값 묶음 */
    public record Profile(String name, String breed, BigDecimal weightKg, String birthYm, Sex sex, boolean neutered,
                          boolean hasCarrier, boolean hasStroller, ActivityLevel activityLevel,
                          List<String> diseases, List<String> allergies, Integer foodKcalPerKg, String memo) {
    }

    public void update(Profile p) {
        this.name = p.name().strip();
        this.breed = p.breed().strip();
        this.weightKg = p.weightKg();
        this.birthYm = p.birthYm();
        this.sex = p.sex();
        this.neutered = p.neutered();
        this.hasCarrier = p.hasCarrier();
        this.hasStroller = p.hasStroller();
        this.activityLevel = p.activityLevel();
        this.diseases = p.diseases() == null ? new ArrayList<>() : new ArrayList<>(p.diseases());
        this.allergies = p.allergies() == null ? new ArrayList<>() : new ArrayList<>(p.allergies());
        this.foodKcalPerKg = p.foodKcalPerKg();
        this.memo = p.memo() == null || p.memo().isBlank() ? null : p.memo().strip();
    }

    public void changeWeight(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public void changePhoto(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public DogSize size() {
        return DogSize.of(weightKg);
    }

    /** 생년월 1일 기준 개월 수 */
    public int ageMonths(LocalDate today) {
        return (int) Math.max(0, ChronoUnit.MONTHS.between(YearMonth.parse(birthYm).atDay(1), today));
    }

    public boolean hasDisease() {
        return diseases != null && !diseases.isEmpty();
    }

    public boolean hasDisease(String key) {
        return diseases != null && diseases.stream().anyMatch(d -> d.equalsIgnoreCase(key));
    }
}
