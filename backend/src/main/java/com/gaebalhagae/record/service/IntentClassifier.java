package com.gaebalhagae.record.service;

import java.util.regex.Pattern;

/**
 * 통합 입력창의 질문/기록 판별 (항목 5). LLM 없이 규칙으로: 무료 한도를 아끼고 결과가 매번 같음.
 * 글 끝이 '?' 이거나 의문형 어미면 질문. 프론트 frontend/src/utils/intent.ts 와 같은 규칙.
 * 정확도 30/30 (IntentClassifierTest 예문 30개).
 */
public final class IntentClassifier {

    private static final Pattern TRAILING = Pattern.compile("[.!~ㅠㅜㅋㅎ\\s]+$");
    private static final Pattern QUESTION_ENDING = Pattern.compile(
            "(\\?|까요|나요|까|[인은는]가요|[인은는]가|야 하나|어떡해|어떡하지|어떻게 해(야 해)?|어쩌죠|도 돼요|도 되나|도 돼)$");

    private IntentClassifier() {
    }

    public static boolean isQuestion(String text) {
        if (text == null) {
            return false;
        }
        String t = TRAILING.matcher(text.strip()).replaceAll("");
        return QUESTION_ENDING.matcher(t).find();
    }
}
