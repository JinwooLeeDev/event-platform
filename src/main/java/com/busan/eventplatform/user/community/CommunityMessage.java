package com.busan.eventplatform.user.community;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * [f/40] 커뮤니티 STOMP 메시지 (JSON으로 전송)
 *
 * @param type      메시지 종류
 * @param sessionId 회차 id (커뮤니티 방)
 * @param userId    관련 회원 id (시스템 메시지면 입장·퇴장한 회원)
 * @param content   화면에 보여줄 문구
 * @param sentAt    보낸 시각 "yyyy-MM-dd HH:mm:ss" (문자열 — 날짜 변환 설정에 영향받지 않게)
 */
public record CommunityMessage (
        Type type,
        Long sessionId,
        Long userId,
        String content,
        String sentAt
) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public enum Type {
        JOIN,    // 방 전체: 누군가 입장
        LEAVE,   // 방 전체: 누군가 퇴장
        JOINED,  // 본인: 커뮤니티에 입장됨 (대기 → 확정 승격 등)
        KICKED,  // 본인: 취소·거절로 퇴장됨 → 화면에서 연결 종료
        CLOSED,  // 방 전체: 커뮤니티 종료 (5단계)
        CHAT     // 채팅 (4단계)
    }

    public static CommunityMessage system(Type type, Long sessionId, Long userId, String content) {
        return new CommunityMessage(type, sessionId, userId, content, LocalDateTime.now().format(FORMAT));
    }
}
