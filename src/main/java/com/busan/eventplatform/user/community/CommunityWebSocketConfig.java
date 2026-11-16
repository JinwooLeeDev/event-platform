package com.busan.eventplatform.user.community;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * [f/40] 커뮤니티 STOMP 설정
 * <p>
 * 주소 정리
 * - 연결:   /ws  (SockJS)                       ← 로그인한 개인 회원만 (CommunityHandshakeInterceptor)
 * - 방:     /topic/community/{sessionId}         ← 그 회차의 활성 멤버(참가 확정자)만 구독 (CommunityChannelInterceptor)
 * - 본인:   /user/queue/community               ← 입장 안내·강제 퇴장(KICKED) 알림
 * - 전송:   /app/community/{sessionId}/...       ← 활성 멤버만 (4단계 채팅에서 사용)
 */

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class CommunityWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CommunityChannelInterceptor communityChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new CommunityHandshakeInterceptor())        // HTTP 로그인 세션 → 웹소켓 세션
                .setHandshakeHandler(new CommunityPrincipalHandshakeHandler()) // 웹소켓 사용자 이름 = 회원 id
                .withSockJS();                                                // 같은 출처(localhost:8080)만 허용 (기본값)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");  // 서버 메모리 브로커 (방 메시지·개인 알림)
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");       // /user/queue/community → 로그인한 본인에게만
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(communityChannelInterceptor); // 구독·전송 권한 검사
    }

}
