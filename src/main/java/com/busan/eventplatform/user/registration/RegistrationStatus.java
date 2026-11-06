package com.busan.eventplatform.user.registration;



public enum RegistrationStatus {
    HOLD,             // [임시] 결제/차감을 진행 중인 상태 (트랜잭션 내에서만 짧게 유지되거나 생략 가능)
    WAITING,          // [유료/무료] 정원 초과로 대기열에 등록된 상태 (유료의 경우 포인트가 이미 차감된 상태)
    PENDING_APPROVAL, // [유료] 결제 차감 후 정원 내 자리를 확보하여 주최측 승인을 기다리는 상태
    CONFIRMED,        // [유료/무료] 참가 최종 확정 (무료는 즉시, 유료는 승인 후 진입)
    CANCELLED         // 사용자 자진 취소, 주최자 반려, 대기 취소 등
}
