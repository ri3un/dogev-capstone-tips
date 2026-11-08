package com.gaebalhagae.place.domain;

/** 조건 출처 우선순위 ADMIN > MANUAL > TOUR > BLOG. 높은 쪽 값은 낮은 쪽 수집이 덮어쓰지 않습니다 */
public enum ConditionSource {
    BLOG(1), TOUR(2), MANUAL(3), ADMIN(4);

    private final int priority;

    ConditionSource(int priority) {
        this.priority = priority;
    }

    public boolean canOverwrite(ConditionSource existing) {
        return existing == null || priority >= existing.priority;
    }
}
