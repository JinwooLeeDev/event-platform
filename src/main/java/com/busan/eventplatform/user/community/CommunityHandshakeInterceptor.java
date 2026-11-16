package com.busan.eventplatform.user.community;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * [f/40] 웹소켓 연결(핸드셰이크) 시 로그인 확인
 * - 기존 로그인 방식(HTTP 세션의 LoginSession.IDENTITY)을 그대로 사용
 * - 로그인한 '개인 회원'만 연결 허용 → 회원 id를 웹소켓 세션 속성에 저장해 이후 권한 검사에 사용
 */

public class CommunityHandshakeInterceptor implements HandshakeInterceptor {

    /** 웹소켓 세션 속성 이름 — 연결한 개인 회원 id */
    public static final String ATTR_USER_ID = "communityUserId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpSession session = servletRequest.getServletRequest().getSession(false); // 세션이 없으면 새로 만들지 않음
        if (session == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED); // 로그인 안 함
            return false;
        }
        Object value = session.getAttribute(LoginSession.IDENTITY);
        if (!(value instanceof LoginIdentity identity) || identity.getId() == null || !identity.isUser()) {
            response.setStatusCode(HttpStatus.FORBIDDEN);    // 로그인 정보 없음 또는 기업 계정
            return false;
        }
        attributes.put(ATTR_USER_ID, identity.getId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 할 일 없음
    }


}
