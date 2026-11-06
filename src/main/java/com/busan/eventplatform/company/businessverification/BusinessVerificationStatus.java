package com.busan.eventplatform.company.businessverification;

public enum BusinessVerificationStatus {
    NOT_CHECKED,    // 검증진행 안 함
    MATCHED,        // 사업자등록증 정보가 일치함
    MISMATCHED,     // 사업자등록증 정보가 일치하지 않음
    ERROR           // 서버 연결에 문제가 발생함
}
