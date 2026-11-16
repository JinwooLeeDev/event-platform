package com.busan.eventplatform.user.registration;

/**
 * [f/40] 참가 신청 상태 변경 이벤트
 * - RegistrationService가 상태를 바꾼 직후 발행한다.
 * - 커뮤니티 등 다른 기능은 이 이벤트를 받아서 처리한다.
 * - RegistrationService(신청 코드) 안에 커뮤니티 관련 코드를 직접 쓰지 않는다.
 * - 신청 코드는 "상태가 바뀌었다"는 알림만 보내고, 그 알림을 받아서 무엇을 할지는 커뮤니티 코드가 정해요.
 * - 받는 쪽은 @TransactionalEventListener(AFTER_COMMIT)로 받아야
 * 롤백된 변경(예: 환불 실패로 취소가 되돌아감)에는 반응하지 않는다.
 *
 * @param before 변경 전 상태 (새 신청이면 null)
 * @param after  변경 후 상태
 */
public record RegistrationStatusChangedEvent(
        Long registrationId, // 어떤 신청이 바뀌었는지 (신청번호)
        Long userId, // 누구의 신청인지(신청한 개인 회원 id)
        Long sessionId, // 어느 회차의 신청인지(회차 id)
        RegistrationStatus before, // 바뀌기 전의 상태(상태를 바꾸기 전에 저장해 둔값 )
        RegistrationStatus after // 바뀐 후의 상태
) {
    // 확정 상태가 새로 업데이트 되었는가? -> 신청 즉시 확정, 대기 -> 확정 승격
    public boolean becameConfirmed() {
        return after == RegistrationStatus.CONFIRMED && before != RegistrationStatus.CONFIRMED;
    }

    /**
     * 확정 상태에서 '벗어났는지' — 확정 후 본인 취소, 기업 거절
     */
    public boolean leftConfirmed() {
        return before == RegistrationStatus.CONFIRMED && after != RegistrationStatus.CONFIRMED;
    }


}
