package com.gaebalhagae.dog.domain;

/** 반려견 삭제 시 기록·질문·상담·코스가 같은 트랜잭션에서 각자 정리합니다 */
public record DogDeletedEvent(Long dogId) {
}
