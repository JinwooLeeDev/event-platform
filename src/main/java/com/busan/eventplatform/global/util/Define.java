package com.busan.eventplatform.global.util;

public class Define {
    public static final String SESSION_COMPANY = "sessionCompanyDto";
    public static final String SESSION_USER = "sessionUserDto";

    public static final int EMAIL_CODE_EXPIRE_MINUTES = 5;

    // 인증 정보를 저장할 세션 키
    public static final String EMAIL_VERIFICATION = "emailVerification";

    // 재발송 대기시간: 60초
    public static final int EMAIL_RESEND_INTERVAL_SECONDS = 60;

    // 코드당 검증 실패 제한: 5회
    public static final int EMAIL_MAX_FAILED_ATTEMPTS = 5;

    // 인증 완료 결과의 유효시간: 10분
    public static final int EMAIL_VERIFIED_EXPIRE_MINUTES = 10;
}
