package com.busan.eventplatform.event.events;

public enum EventStatus {
    PENDING_REVIEW,	// 승인 대기
    FEEDBACK_REQUIRED,	// 보완 요청
    PUBLISHED,	// 공개 및 승인 완료. 최종 확정.
    PAID_READY ,   // 결제 대기
    COMPLETED,	// 행사 종료
    CANCELLED // 행사 취소
}