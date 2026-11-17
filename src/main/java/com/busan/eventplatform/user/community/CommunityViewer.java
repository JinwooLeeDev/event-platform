package com.busan.eventplatform.user.community;

import java.util.Map;

/**
 * [f/40 6단계] 커뮤니티를 보는 사람 — 참가 확정 회원 또는 행사를 주최한 기업
 *
 * key() = STOMP 사용자 이름(Principal) — "user:36002" / "company:5"
 * 회원 id와 기업 id는 서로 다른 테이블이라 값이 겹칠 수 있음 → 앞에 종류를 붙여 개인 알림이 섞이지 않게 한다
 */
public record CommunityViewer(Type type, Long id) {

    public enum Type { USER, COMPANY }

    public static CommunityViewer user(Long userId) {
        return new CommunityViewer(Type.USER, userId);
    }

    public static CommunityViewer company(Long companyId) {
        return new CommunityViewer(Type.COMPANY, companyId);
    }

    public boolean isUser() {
        return type == Type.USER;
    }

    public boolean isCompany() {
        return type == Type.COMPANY;
    }

    /** STOMP 사용자 이름이자 화면의 '내 메시지' 판단 기준 */
    public String key() {
        return keyOf(type, id);
    }

    public static String userKey(Long userId) {
        return keyOf(Type.USER, userId);
    }

    public static String companyKey(Long companyId) {
        return keyOf(Type.COMPANY, companyId);
    }

    private static String keyOf(Type type, Long id) {
        return type.name().toLowerCase() + ":" + id;
    }

    /** 웹소켓 세션 속성(핸드셰이크에서 저장)에서 꺼내기 — 없으면 null */
    public static CommunityViewer from(Map<String, Object> attributes) {
        if (attributes == null) {
            return null;
        }
        if (attributes.get(CommunityHandshakeInterceptor.ATTR_USER_ID) instanceof Long userId) {
            return user(userId);
        }
        if (attributes.get(CommunityHandshakeInterceptor.ATTR_COMPANY_ID) instanceof Long companyId) {
            return company(companyId);
        }
        return null;
    }
}