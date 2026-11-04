package com.busan.eventplatform.company;

public enum CompanyStatus {
    PENDING, // 관리자 승인 대기중
    APPROVED, // 관리자 승인 완료
    REJECTED, // 관리자 승인 거부
    FEEDBACK_NEEDED, // 승인 사유 보완 필요.
    SUSPENDED // 계정 정지
}
