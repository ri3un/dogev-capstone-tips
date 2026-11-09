package com.gaebalhagae.consult.domain;

/** 긴급도. 여러 모델 중 가장 높은 값을 씁니다 (결정 14) */
public enum Urgency {
    LOW("지켜봐도 괜찮아요"),
    MEDIUM("며칠 안에 진료를 고려하세요"),
    HIGH("오늘 진료를 권해요"),
    EMERGENCY("지금 바로 동물병원");

    private final String label;

    Urgency(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
