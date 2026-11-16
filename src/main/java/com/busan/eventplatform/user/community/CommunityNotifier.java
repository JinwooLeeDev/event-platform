package com.busan.eventplatform.user.community;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * [f/40] 커뮤니티 실시간 알림 (STOMP 전송)
 * - 방 전체(/topic/community/{sessionId}) : 누가 들어오고 나갔는지
 * - 본인(/user/queue/community)           : 입장됨 / 강제 퇴장됨
 * - 접속 중이 아니면 메시지는 그냥 사라짐 (멤버십은 DB에 이미 반영됨)
 */
@Component
@RequiredArgsConstructor
public class CommunityNotifier {

    private static final String USER_QUEUE = "/queue/community";

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyJoined(Long sessionId, Long userId) {
        messagingTemplate.convertAndSend(topic(sessionId),
                CommunityMessage.system(CommunityMessage.Type.JOIN, sessionId, userId, "새 참가자가 입장했습니다."));
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE,
                CommunityMessage.system(CommunityMessage.Type.JOINED, sessionId, userId, "참가가 확정되어 커뮤니티에 입장했습니다."));
    }

    public void notifyLeft(Long sessionId, Long userId, CommunityLeaveReason reason) {
        messagingTemplate.convertAndSend(topic(sessionId),
                CommunityMessage.system(CommunityMessage.Type.LEAVE, sessionId, userId, "참가자가 퇴장했습니다."));
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE,
                CommunityMessage.system(CommunityMessage.Type.KICKED, sessionId, userId,
                        reason.getDescription() + "(으)로 커뮤니티에서 퇴장되었습니다."));
    }

    private String topic(Long sessionId) {
        return "/topic/community/" + sessionId;
    }

}
