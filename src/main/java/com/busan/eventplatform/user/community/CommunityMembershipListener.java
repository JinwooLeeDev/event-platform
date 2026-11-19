package com.busan.eventplatform.user.community;

import com.busan.eventplatform.user.registration.RegistrationSessionCancelledEvent;   // ★ [f/41]
import com.busan.eventplatform.user.registration.RegistrationStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * [f/40] 신청 상태 변경 → 커뮤니티 입장·퇴장
 * - AFTER_COMMIT: 신청 변경이 DB에 '확정'된 뒤에만 반응 (환불 실패로 취소가 롤백되면 퇴장도 안 함)
 * - 여기서 예외가 나도 신청은 이미 저장됐으므로, 화면 요청이 실패하지 않도록 잡아서 로그만 남긴다.
 *   (멤버십이 어긋나면 서버 재시작 시 CommunityMembershipInitializer 가 확정자 기준으로 다시 맞춤)
 * ★ [6단계] 실제로 입장·퇴장됐을 때만 → 입장·퇴장 기록 저장(새로고침해도 보이게) + 실시간 알림
 * ★ [f/41] 회차 삭제 승인 → 방 전체 종료·삭제 + 접속 중인 화면에 종료 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityMembershipListener {
    private final CommunityService communityService;
    private final CommunityNotifier communityNotifier;   // [3단계] 실시간 알림

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationStatusChanged(RegistrationStatusChangedEvent event) {
        try {
            if (event.becameConfirmed()) {
                if (communityService.join(event.sessionId(), event.userId())) {               // ★ 실제로 입장했을 때만
                    communityService.recordMemberEvent(event.sessionId(), event.userId(),
                            CommunityChatMessage.Kind.JOIN);                                  // ★ 입장 기록 저장
                    communityNotifier.notifyJoined(event.sessionId(), event.userId());        // 방 + 본인에게 입장 알림
                }
            } else if (event.leftConfirmed()) {
                CommunityLeaveReason reason = CommunityLeaveReason.from(event.after());
                if (communityService.leave(event.sessionId(), event.userId(), reason)) {      // ★ 실제로 퇴장했을 때만
                    communityService.recordMemberEvent(event.sessionId(), event.userId(),
                            CommunityChatMessage.Kind.LEAVE);                                 // ★ 퇴장 기록 저장
                    communityNotifier.notifyLeft(event.sessionId(), event.userId(), reason);  // 방 + 본인(KICKED) 알림
                }
            }
        } catch (Exception e) {
            log.error("[community] 멤버십 반영 실패 신청={} 회원={} 회차={} {} → {}",
                    event.registrationId(), event.userId(), event.sessionId(),
                    event.before(), event.after(), e);
        }
    }

    /**
     * ★ [f/41] 회차 삭제 승인 (RegistrationService.cancelAllBySession 이 발행)
     * 승인 트랜잭션이 커밋된 뒤에만 실행 → 승인이 롤백되면(예: 예약금 환불 실패) 방도 그대로 유지
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSessionCancelled(RegistrationSessionCancelledEvent event) {
        try {
            communityService.closeCancelledSession(event.sessionId());
            communityNotifier.notifySessionCancelled(event.sessionId());
        } catch (Exception e) {
            log.error("[community] 회차 취소 커뮤니티 정리 실패 회차={}", event.sessionId(), e);
        }
    }
}
