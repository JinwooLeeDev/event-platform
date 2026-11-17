package com.busan.eventplatform.user.community;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * [f/40] 웹소켓 사용자 이름(Principal)
 * ★ [6단계] "36002" → "user:36002" / "company:5"
 *  - 회원 id와 기업 id는 테이블이 달라 값이 겹칠 수 있음 → 종류를 붙여야 개인 알림(/user/queue/community)이 섞이지 않음
 *  - 보내는 쪽: convertAndSendToUser(CommunityViewer.userKey(36002), "/queue/community", ...)
 */
public class CommunityPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        CommunityViewer viewer = CommunityViewer.from(attributes);
        String name = viewer == null ? "anonymous" : viewer.key();
        return () -> name;
    }
}
