package com.busan.eventplatform.user.community;

import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * [f/40] 커뮤니티 실시간 알림 (STOMP 전송)
 * - 방 전체(/topic/community/{sessionId}) : 누가 들어오고 나갔는지 ("예시회원03님이 입장했습니다.")
 * - 본인(/user/queue/community)           : 입장됨 / 강제 퇴장됨
 * - 접속 중이 아니면 메시지는 그냥 사라짐 (멤버십은 DB에 이미 반영됨)
 * - 방 전체 안내에는 퇴장 사유(취소·거절)를 넣지 않는다 — 다른 참가자에게 사유가 드러나지 않게, 사유는 본인 알림에만
 * - ★ [5-2] 종료일 당일 11:00 안내(REMINDER) — 방 전체
 * - ★ [5단계] 커뮤니티 종료(CLOSED) — 방 전체에 한 번 → 화면이 연결을 끊고 내 행사로 이동
 */
@Component
@RequiredArgsConstructor
public class CommunityNotifier {

    private static final String USER_QUEUE = "/queue/community";
    private static final String UNKNOWN_NAME = "참가자";   // 회원을 못 찾았을 때 대신 쓰는 이름

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;           // ★ 닉네임 조회용

    public void notifyJoined(Long sessionId, Long userId) {
        String name = nameOf(userId);
        messagingTemplate.convertAndSend(topic(sessionId),                                // ★ [6단계] 참여자 목록 갱신용 닉네임 포함
                CommunityMessage.member(CommunityMessage.Type.JOIN, sessionId, userId, name, name + "님이 입장했습니다."));
        messagingTemplate.convertAndSendToUser(CommunityViewer.userKey(userId), USER_QUEUE, // ★ [6단계] "user:{id}"
                CommunityMessage.system(CommunityMessage.Type.JOINED, sessionId, userId, "참가가 확정되어 커뮤니티에 입장했습니다."));
    }

    public void notifyLeft(Long sessionId, Long userId, CommunityLeaveReason reason) {
        String name = nameOf(userId);
        messagingTemplate.convertAndSend(topic(sessionId),                                // 사유는 넣지 않음
                CommunityMessage.member(CommunityMessage.Type.LEAVE, sessionId, userId, name, name + "님이 퇴장했습니다."));
        messagingTemplate.convertAndSendToUser(CommunityViewer.userKey(userId), USER_QUEUE, // ★ [6단계] "user:{id}"
                CommunityMessage.system(CommunityMessage.Type.KICKED, sessionId, userId,
                        reason.getDescription() + "(으)로 커뮤니티에서 퇴장되었습니다."));
    }

    /** ★ [5단계] 커뮤니티 종료 — 스케줄러가 DB 정리(커밋)를 마친 뒤 호출 */
    public void notifyClosed(Long sessionId) {
        messagingTemplate.convertAndSend(topic(sessionId),
                CommunityMessage.system(CommunityMessage.Type.CLOSED, sessionId, null,
                        "행사가 끝나 커뮤니티가 종료되었습니다."));
    }

    /** ★ [5-2] 종료일 당일 안내 — 방 전체, 화면은 상단 배너로 표시 */
    public void notifyEndDay(Long sessionId) {
        messagingTemplate.convertAndSend(topic(sessionId),
                CommunityMessage.system(CommunityMessage.Type.REMINDER, sessionId, null, CommunityPolicy.END_DAY_NOTICE));
    }

    /** 화면에 보여줄 닉네임 — 리스너(커밋 후)에서 불리므로 엔티티가 아닌 문자열만 꺼내 쓴다 */
    private String nameOf(Long userId) {
        return userRepository.findById(userId)
                .map(User::getDisplayName)
                .filter(name -> !name.isBlank())
                .orElse(UNKNOWN_NAME);
    }

    private String topic(Long sessionId) {
        return "/topic/community/" + sessionId;
    }
}
