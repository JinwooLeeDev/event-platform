package com.busan.eventplatform.global.error.exception;

/**
 * 신청 도메인 권한 위반 (403).
 * 예: 타인의 신청 취소, 주최자가 아닌 기업의 반려·참가자 조회.
 */

public class RegistrationForbiddenException extends RuntimeException {
    public RegistrationForbiddenException(String message) {
        super(message);
    }
}