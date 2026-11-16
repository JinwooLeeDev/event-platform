package com.busan.eventplatform.user.community;

import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * [f/40] 커뮤니티 멤버십 입장 및 퇴장
 *
 *  join/leave 가 REQUIRES_NEW 인 이유:
 *  - 이 메서드들은 신청 트랜잭션이 '커밋된 뒤'(AFTER_COMMIT) 리스너에서 불린다.
 *  - 그 시점에는 이미 끝난 신청 트랜잭션에 합류하면 저장이 반영되지 않으므로, 항상 '새 트랜잭션'으로 저장한다.
 * 클래스의 메서드명
 * 1. join()
 * 2. leave()
 * 3. isActiveMember()
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {
    private final CommunityMemberRepository communityMemberRepository;
    private final EventSessionRepository eventSessionRepository;
    private final UserRepository userRepository;

    /** 입장 — 처음이면 행 생성, 퇴장했던 멤버면 다시 활성화, 이미 활성이면 그대로 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void join(Long sessionId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        communityMemberRepository.findByEventSessionIdAndUserId(sessionId, userId)
                .ifPresentOrElse(
                        member -> {
                            if (!member.isActive()) {
                                member.rejoin(now);
                            }
                        },
                        () -> communityMemberRepository.save(CommunityMember.join(
                                eventSessionRepository.getReferenceById(sessionId),  // 조회 없이 id만으로 연결
                                userRepository.getReferenceById(userId),
                                now)));
        log.info("[community] 입장 회차={} 회원={}", sessionId, userId);
    }

    /** 퇴장 — 참가 확정 멤버가 아니면(대기자 등) 아무것도 하지 않음 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void leave(Long sessionId, Long userId, CommunityLeaveReason reason) {
        communityMemberRepository.findByEventSessionIdAndUserId(sessionId, userId)
                .ifPresent(member -> member.leave(reason, LocalDateTime.now()));
        log.info("[community] 퇴장 회차={} 회원={} 사유={}", sessionId, userId, reason);
    }

    /** 지금 활성 멤버인지 — 화면 진입·STOMP 구독 권한 (3단계) */
    public boolean isActiveMember(Long sessionId, Long userId) {
        return communityMemberRepository.existsByEventSessionIdAndUserIdAndLeftAtIsNull(sessionId, userId);
    }

}
