package com.gaebalhagae.global.response;

import lombok.Getter;

/**
 * 서비스 코드에서 throw new BusinessException(ErrorCode.NOT_FOUND) 처럼 던지면
 * GlobalExceptionHandler가 공통 응답 형식으로 바꿔 줍니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
