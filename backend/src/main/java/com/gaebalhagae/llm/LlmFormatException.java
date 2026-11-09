package com.gaebalhagae.llm;

/** LLM 응답이 약속한 JSON 형식이 아닐 때. 같은 모델에 한 번 더 묻는 기준입니다 (결정 14-①) */
public class LlmFormatException extends RuntimeException {

    public LlmFormatException(String message) {
        super(message);
    }
}
