package com.busan.eventplatform.user.registration;

/**
 * ★ [f/41] 회차 삭제(취소) 승인으로 그 회차의 신청이 일괄 정리되었다는 이벤트
 * - RegistrationService.cancelAllBySession() 이 신청 정리 직후 '한 번' 발행한다.
 * - 커뮤니티는 이 이벤트를 커밋 후(AFTER_COMMIT) 받아 방 전체를 종료·삭제한다.
 * - 신청 코드는 커뮤니티를 모른다 (RegistrationStatusChangedEvent 와 같은 방식).
 *
 * @param sessionId 취소된 회차 id
 */
public record RegistrationSessionCancelledEvent(Long sessionId) {
}