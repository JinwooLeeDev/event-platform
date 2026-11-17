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
 * - ★ [6단계] 개인 회원 + 기업 계정 연결 허용 (기업은 '주최한 행사의 방'만 — 권한은 CommunityChannelInterceptor 에서)
 * - 회원이면 회원 id, 기업이면 기업 id를 웹소켓 세션 속성에 저장 → CommunityViewer.from() 으로 꺼내 씀
 */
public class CommunityHandshakeInterceptor implements HandshakeInterceptor {

    /** 웹소켓 세션 속성 이름 — 연결한 개인 회원 id */
    public static final String ATTR_USER_ID = "communityUserId";
    /** ★ [6단계] 웹소켓 세션 속성 이름 — 연결한 기업 id */
    public static final String ATTR_COMPANY_ID = "communityCompanyId";

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
        if (!(value instanceof LoginIdentity identity) || identity.getId() == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED); // 로그인 정보 없음
            return false;
        }
        if (identity.isUser()) {
            attributes.put(ATTR_USER_ID, identity.getId());
            return true;
        }
        if (identity.isCompany()) {                          // ★ [6단계]
            attributes.put(ATTR_COMPANY_ID, identity.getId());
            return true;
        }
        response.setStatusCode(HttpStatus.FORBIDDEN);        // 그 밖의 계정 (관리자 등)
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 할 일 없음
    }
}
