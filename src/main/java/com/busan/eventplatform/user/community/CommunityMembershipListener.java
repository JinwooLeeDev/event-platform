package com.busan.eventplatform.user.community;

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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityMembershipListener {
    private final CommunityService communityService;
    private final CommunityNotifier communityNotifier;   // ★ [3단계] 실시간 알림

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationStatusChanged(RegistrationStatusChangedEvent event) {
        try {
            if (event.becameConfirmed()) {
                communityService.join(event.sessionId(), event.userId());
                communityNotifier.notifyJoined(event.sessionId(), event.userId());          // ★ 방 + 본인에게 입장 알림
            } else if (event.leftConfirmed()) {
                CommunityLeaveReason reason = CommunityLeaveReason.from(event.after());
                communityService.leave(event.sessionId(), event.userId(), reason);
                communityNotifier.notifyLeft(event.sessionId(), event.userId(), reason);    // ★ 방 + 본인(KICKED) 알림
            }
        } catch (Exception e) {
            log.error("[community] 멤버십 반영 실패 신청={} 회원={} 회차={} {} → {}",
                    event.registrationId(), event.userId(), event.sessionId(),
                    event.before(), event.after(), e);
        }
    }

}
