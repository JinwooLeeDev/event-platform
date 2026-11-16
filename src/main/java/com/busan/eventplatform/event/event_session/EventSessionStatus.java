package com.busan.eventplatform.event.event_session;

public enum EventSessionStatus {
    SCHEDULED, // 행사 진행 예정
    IN_PROGRESS, // 행사 진행 중
    COMPLETED, // 행사 종료
    CANCELLED , // 행사 취소
}