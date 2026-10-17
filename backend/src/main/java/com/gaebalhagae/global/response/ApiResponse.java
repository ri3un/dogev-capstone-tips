package com.gaebalhagae.global.response;

/**
 * 모든 API가 같은 모양으로 응답합니다. (결정 18)
 * 성공: {"success": true, "data": {...}, "error": null}
 * 실패: {"success": false, "data": null, "error": {"code": "NOT_FOUND", "message": "..."}}
 */
public record ApiResponse<T>(boolean success, T data, ErrorBody error) {

    public record ErrorBody(String code, String message) {
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.name(), message));
    }
}
