package com.busan.eventplatform.event.events;

public enum Status {

    DRAFT,	// 임시 작성
    PENDING_REVIEW,	// 승인 대기
    FEEDBACK_REQUIRED,	// 보완 요청
    PUBLISHED,	// 공개 및 승인 완료
    COMPLETED,	// 행사 종료
    CANCELLED	// 행사 취소
}
