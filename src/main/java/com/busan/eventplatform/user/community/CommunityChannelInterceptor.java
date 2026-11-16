package com.busan.eventplatform.user.community;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * [f/40] STOMP 구독·전송 권한 검사 — "참가 확정자만, 운영 기간에만" (대기자·취소자·거절자·다른 회차 회원·종료된 방 거부)
 * - SUBSCRIBE /topic/community/{sessionId} : 그 회차의 활성 멤버 + 운영 중
 * - SEND      /app/community/{sessionId}/… : 그 회차의 활성 멤버 + 운영 중
 * - /user/queue/community (본인 알림)         : 허용
 * - 그 밖의 /topic, /queue 직접 구독          : 거부 (다른 사람의 개인 알림 주소를 직접 구독하는 것 방지)
 * 거부하면 클라이언트에 STOMP ERROR 프레임이 가고 연결이 끊긴다.
 * ★ [5단계] 운영 기간 검사 추가 (B-2) — 연결된 채로 24시를 넘겨도 스케줄러를 기다리지 않고 바로 막힘
 */
@Component
@RequiredArgsConstructor
public class CommunityChannelInterceptor implements ChannelInterceptor {

    private static final String TOPIC_PREFIX = "/topic/community/";
    private static final String APP_PREFIX = "/app/community/";

    private final CommunityService communityService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command != StompCommand.SUBSCRIBE && command != StompCommand.SEND) {
            return message; // CONNECT, DISCONNECT 등은 통과 (연결 자체는 핸드셰이크에서 이미 검사)
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Long sessionId;
        if (destination.startsWith(TOPIC_PREFIX)) {
            sessionId = parseSessionId(destination.substring(TOPIC_PREFIX.length()));
        } else if (destination.startsWith(APP_PREFIX)) {
            sessionId = parseSessionId(destination.substring(APP_PREFIX.length()));
        } else if (destination.startsWith("/topic/") || destination.startsWith("/queue/")) {
            throw new MessagingException("허용되지 않은 주소입니다");
        } else {
            return message; // /user/queue/community 등
        }

        if (!communityService.canAccess(sessionId, userIdOf(accessor))) { // ★ 멤버 + 운영 중
            throw new MessagingException("참가 확정자만, 커뮤니티 운영 기간에만 이용할 수 있습니다");
        }
        return message;
    }

    /** "36002" 또는 "36002/messages" → 36002 */
    private Long parseSessionId(String rest) {
        int slash = rest.indexOf('/');
        String id = slash < 0 ? rest : rest.substring(0, slash);
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new MessagingException("잘못된 커뮤니티 주소입니다");
        }
    }

    private Long userIdOf(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(CommunityHandshakeInterceptor.ATTR_USER_ID);
        return value instanceof Long id ? id : null;
    }
}
