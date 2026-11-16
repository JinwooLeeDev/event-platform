package com.busan.eventplatform.user.community;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * [f/40] 웹소켓 사용자 이름(Principal) = 회원 id
 * - convertAndSendToUser("36005", "/queue/community", ...) 처럼 '회원 id'로 본인에게만 알림을 보낼 수 있게 함
 */

public class CommunityPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object userId = attributes.get(CommunityHandshakeInterceptor.ATTR_USER_ID);
        String name = String.valueOf(userId);
        return () -> name;
    }

}
