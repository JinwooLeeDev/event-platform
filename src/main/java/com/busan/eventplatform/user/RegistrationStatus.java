package com.busan.eventplatform.user;

public enum RegistrationStatus {
    PENDING_REVIEW,  // 유료 행사: 결제 완료 후 주최자 심사 대기
    CONFIRMED,       // 참가 확정 (무료 행사: 즉시 확정, 유료 행사: 심사 승인)
    WAITING,         // 만석으로 인한 대기열 등록
    CANCELLED        // 취소 (사용자 자발적 취소 또는 주최자 심사 반려)
}
