package com.busan.eventplatform.user.community;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import org.springframework.stereotype.Component;

/**
 * [f/40] STOMP 구독·전송 권한 검사 — "참가 확정자 또는 주최 기업만, 운영 기간에만"
 * - SUBSCRIBE /topic/community/{sessionId} : 그 회차의 활성 멤버 / ★ 주최 기업 + 운영 중
 * - SEND      /app/community/{sessionId}/… : 위와 같음 (채팅 · ★ 공지 — 공지는 서비스에서 주최 기업인지 한 번 더)
 * - /user/queue/community (본인 알림)         : 허용
 * - 그 밖의 /topic, /queue 직접 구독          : 거부 (다른 사람의 개인 알림 주소를 직접 구독하는 것 방지)
 * 거부하면 클라이언트에 STOMP ERROR 프레임이 가고 연결이 끊긴다.
 * [5단계] 운영 기간 검사 (B-2) / ★ [6단계] 보는 사람을 CommunityViewer(회원·기업)로 판단
 *
 * ★ [6단계 수정] CONNECT 때 STOMP 사용자(Principal)를 직접 지정
 *  - SockJS 는 웹소켓이 안 되면 HTTP 스트리밍(xhr-streaming 등)으로 연결하는데, 이때는 핸드셰이크 핸들러의
 *    Principal 이 적용되지 않아 사용자 이름이 비어 있음 → /user/queue/community(KICKED·ERROR) 가 전달되지 않음
 *  - 세션 속성(핸드셰이크 인터셉터가 모든 전송 방식에서 저장)으로 "user:{id}" / "company:{id}" 를 지정해
 *    어떤 전송 방식이든 개인 알림이 전달되게 한다
 */
@Component
@RequiredArgsConstructor
public class CommunityChannelInterceptor implements ChannelInterceptor {

    private static final String TOPIC_PREFIX = "/topic/community/";
    private static final String APP_PREFIX = "/app/community/";

    private final CommunityService communityService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 원본 메시지의 헤더를 직접 고칠 수 있는 accessor (setUser 가 실제 메시지에 반영되도록 wrap 대신 사용)
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }
        StompCommand command = accessor.getCommand();

        // ★ 연결 시 사용자 지정 — 전송 방식(웹소켓·HTTP 스트리밍)과 관계없이 개인 알림이 가도록
        if (command == StompCommand.CONNECT) {
            CommunityViewer viewer = CommunityViewer.from(accessor.getSessionAttributes());
            if (viewer != null) {
                String name = viewer.key();
                Principal user = () -> name;
                accessor.setUser(user);
            }
            return message;
        }

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

        CommunityViewer viewer = CommunityViewer.from(accessor.getSessionAttributes()); // ★ [6단계]
        if (!communityService.canAccess(sessionId, viewer)) {
            throw new MessagingException("참가 확정자·주최 기업만, 커뮤니티 운영 기간에만 이용할 수 있습니다");
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
}
