package com.gaebalhagae.consult.service;

import java.util.List;

/** 질문에 이런 표현이 있으면 답변보다 먼저 "즉시 동물병원"을 안내합니다 (결정 15) */
public final class EmergencyKeywords {

    private static final List<String> WORDS = List.of(
            "경련", "발작", "호흡곤란", "숨을 못", "숨을 헐떡", "의식", "쓰러", "기절", "피를 토", "토혈",
            "혈변", "검은 변", "중독", "독극물", "초콜릿", "포도", "건포도", "자일리톨", "양파", "쥐약", "부동액",
            "교통사고", "차에 치", "높은 곳에서 떨어", "배가 부풀", "배가 빵빵", "잇몸이 하얗", "잇몸이 파랗");

    private EmergencyKeywords() {
    }

    public static boolean matches(String text) {
        return text != null && WORDS.stream().anyMatch(text::contains);
    }
}
