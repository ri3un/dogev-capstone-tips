package com.gaebalhagae.record.domain;

/** 글 입력 카테고리 9개 (결정 9·10). QUESTION 은 저장할 때 질문 메모(vet_question)로 갑니다 */
public enum RecordCategory {
    MEAL("식사"),
    SNACK("간식"),
    STOOL("배변"),
    ACTIVITY("활동"),
    SYMPTOM("증상"),
    WEIGHT("체중"),
    MEDICATION("투약·접종"),
    HOSPITAL("병원"),
    QUESTION("질문 메모");

    private final String label;

    RecordCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
