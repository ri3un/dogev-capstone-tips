package com.gaebalhagae.record.domain;

public enum StoolStatus {
    NORMAL, SOFT, DIARRHEA, HARD, BLOODY;

    /** 코스 규칙 3·리포트 이상 신호에서 "설사·무른 변"으로 세는 상태 */
    public boolean isLoose() {
        return this == SOFT || this == DIARRHEA || this == BLOODY;
    }
}
