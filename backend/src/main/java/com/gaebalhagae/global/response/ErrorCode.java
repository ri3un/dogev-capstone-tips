package com.gaebalhagae.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 오류 코드는 이 enum 하나에서만 관리합니다. 기능별 코드는 아래에 추가합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "jpg·png·webp 이미지(5MB 이하)만 올릴 수 있습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다"),

    // 가족 그룹·반려견 (결정 26). 남의 반려견은 존재 여부를 드러내지 않도록 404
    DOG_NOT_FOUND(HttpStatus.NOT_FOUND, "반려견을 찾을 수 없습니다"),
    FAMILY_NOT_FOUND(HttpStatus.NOT_FOUND, "가족 그룹을 찾을 수 없습니다"),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "초대 링크가 올바르지 않거나 새로 만들어졌습니다"),
    FAMILY_OWNER_ONLY(HttpStatus.FORBIDDEN, "가족 관리자만 할 수 있습니다"),
    FAMILY_FULL(HttpStatus.BAD_REQUEST, "가족은 최대 6명까지 함께할 수 있습니다"),
    FAMILY_ALREADY_JOINED(HttpStatus.BAD_REQUEST, "다른 가족이 있는 그룹에서는 초대를 수락할 수 없습니다. 먼저 그룹을 나가 주세요"),
    FAMILY_OWNER_MUST_TRANSFER(HttpStatus.BAD_REQUEST, "다른 가족이 있으면 관리자를 넘긴 뒤 나갈 수 있습니다"),
    FAMILY_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "우리 가족이 아닌 회원입니다"),

    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "기록을 찾을 수 없습니다"),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문 메모를 찾을 수 없습니다"),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다"),
    EDIT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "수정 요청을 찾을 수 없습니다"),
    EDIT_REQUEST_ALREADY_HANDLED(HttpStatus.BAD_REQUEST, "이미 처리된 수정 요청입니다"),
    CONSULTATION_NOT_FOUND(HttpStatus.NOT_FOUND, "상담을 찾을 수 없습니다"),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다"),
    COURSE_NOT_ENOUGH_PLACES(HttpStatus.NOT_FOUND, "조건에 맞는 장소가 부족해 코스를 만들지 못했습니다"),

    EXTERNAL_API_FAILED(HttpStatus.BAD_GATEWAY, "외부 API 호출에 실패했습니다"),
    LLM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String message;
}
