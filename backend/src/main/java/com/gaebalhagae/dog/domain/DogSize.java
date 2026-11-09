package com.gaebalhagae.dog.domain;

import java.math.BigDecimal;

/** 소형 10kg 미만, 중형 25kg 미만, 대형 25kg 이상 (결정 6) */
public enum DogSize {
    SMALL, MEDIUM, LARGE;

    private static final BigDecimal MEDIUM_FROM = BigDecimal.TEN;
    private static final BigDecimal LARGE_FROM = new BigDecimal("25");

    public static DogSize of(BigDecimal weightKg) {
        if (weightKg.compareTo(MEDIUM_FROM) < 0) {
            return SMALL;
        }
        return weightKg.compareTo(LARGE_FROM) < 0 ? MEDIUM : LARGE;
    }
}
