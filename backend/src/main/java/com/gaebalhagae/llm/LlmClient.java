package com.gaebalhagae.llm;

/**
 * LLM 한 곳을 호출하는 공통 모양. 모델 이름과 주소는 application.yml 의 llm.* 에서 바꿉니다.
 */
public interface LlmClient {

    /** 시스템 지시와 사용자 입력을 보내고, 모델이 만든 JSON 문자열을 그대로 돌려줍니다. */
    String completeJson(String system, String user);

    /** 저장할 때 어떤 모델이 만들었는지 남기기 위한 이름 (예: groq/openai/gpt-oss-120b) */
    String name();
}
