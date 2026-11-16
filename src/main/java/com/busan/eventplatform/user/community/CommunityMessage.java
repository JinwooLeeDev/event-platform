package com.busan.eventplatform.user.community;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * [f/40] 커뮤니티 STOMP 메시지 (JSON으로 전송)
 *
 * @param type      메시지 종류
 * @param sessionId 회차 id (커뮤니티 방)
 * @param userId    관련 회원 id
 * @param nickname  보낸 사람 닉네임 (CHAT만, 시스템 메시지는 null)
 * @param content   화면에 보여줄 문구 — 화면에서는 반드시 textContent 로 넣는다 (XSS 방지)
 * @param sentAt    보낸 시각 "MM.dd HH:mm" (문자열 — 날짜 변환 설정에 영향받지 않게)
 */
public record CommunityMessage(
        Type type,
        Long sessionId,
        Long userId,
        String nickname,
        String content,
        String sentAt
) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM.dd HH:mm");

    public enum Type {
        JOIN,    // 방 전체: 누군가 입장
        LEAVE,   // 방 전체: 누군가 퇴장
        JOINED,  // 본인: 커뮤니티에 입장됨 (대기 → 확정 승격 등)
        KICKED,  // 본인: 취소·거절로 퇴장됨 → 화면에서 연결 종료
        CLOSED,  // 방 전체: 커뮤니티 종료 (5단계)
        REMINDER,// 방 전체: 종료일 당일 11:00 안내 (5-2단계)
        CHAT,    // 채팅
        ERROR    // 본인: 메시지 전송 실패 안내
    }

    public static CommunityMessage system(Type type, Long sessionId, Long userId, String content) {
        return new CommunityMessage(type, sessionId, userId, null, content, CommunityPolicy.now().format(FORMAT));
    }

    public static CommunityMessage chat(Long sessionId, Long userId, String nickname, String content, LocalDateTime at) {
        return new CommunityMessage(Type.CHAT, sessionId, userId, nickname, content, at.format(FORMAT));
    }

    public static String format(LocalDateTime at) {
        return at.format(FORMAT);
    }
}