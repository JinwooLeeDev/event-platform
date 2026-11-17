package com.busan.eventplatform.user.community;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * [f/40] 커뮤니티 STOMP 메시지 (JSON으로 전송)
 *
 * @param type      메시지 종류
 * @param sessionId 회차 id (커뮤니티 방)
 * @param userId    관련 회원 id (JOIN·LEAVE·CHAT 회원 / 기업 채팅·시스템 메시지는 null)
 * @param nickname  보낸 사람·입장·퇴장한 사람 이름 (CHAT·JOIN·LEAVE)
 * @param content   화면에 보여줄 문구 — 화면에서는 반드시 textContent 로 넣는다 (XSS 방지)
 * @param sentAt    보낸 시각 "MM.dd HH:mm" (Asia/Seoul)
 * @param senderKey ★ [6단계] 보낸 사람 "user:36002" / "company:5" — 화면의 '내 메시지' 판단 (CHAT만)
 * @param host      ★ [6단계] 주최 기업이 보낸 채팅인지 (말풍선 색 구분)
 */
public record CommunityMessage(
        Type type,
        Long sessionId,
        Long userId,
        String nickname,
        String content,
        String sentAt,
        String senderKey,
        boolean host
) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("MM.dd HH:mm");

    public enum Type {
        JOIN,    // 방 전체: 누군가 입장 (참여자 목록에 추가)
        LEAVE,   // 방 전체: 누군가 퇴장 (참여자 목록에서 제거)
        JOINED,  // 본인: 커뮤니티에 입장됨 (대기 → 확정 승격 등)
        KICKED,  // 본인: 취소·거절로 퇴장됨 → 화면에서 연결 종료
        CLOSED,  // 방 전체: 커뮤니티 종료 (5단계)
        REMINDER,// 방 전체: 종료일 당일 11:00 안내 (5-2단계)
        NOTICE,  // ★ 방 전체: 주최 기업 공지 등록·수정 (빈 문구 = 공지 내림) (6단계)
        CHAT,    // 채팅
        ERROR    // 본인: 메시지 전송 실패 안내
    }

    public static CommunityMessage system(Type type, Long sessionId, Long userId, String content) {
        return new CommunityMessage(type, sessionId, userId, null, content, now(), null, false);
    }

    /** ★ [6단계] 입장·퇴장 — 참여자 목록 갱신을 위해 회원 id와 닉네임을 함께 보냄 */
    public static CommunityMessage member(Type type, Long sessionId, Long userId, String nickname, String content) {
        return new CommunityMessage(type, sessionId, userId, nickname, content, now(), null, false);
    }

    public static CommunityMessage chat(Long sessionId, CommunityViewer sender, String nickname,
                                        String content, LocalDateTime at) {
        Long userId = sender.isUser() ? sender.id() : null;
        return new CommunityMessage(Type.CHAT, sessionId, userId, nickname, content, at.format(FORMAT),
                sender.key(), sender.isCompany());
    }

    /** ★ [6단계] 공지 등록·수정 (content 가 빈 문자열이면 공지 내림) */
    public static CommunityMessage notice(Long sessionId, String content, LocalDateTime at) {
        return new CommunityMessage(Type.NOTICE, sessionId, null, null, content, at.format(FORMAT), null, true);
    }

    public static String format(LocalDateTime at) {
        return at.format(FORMAT);
    }

    private static String now() {
        return CommunityPolicy.now().format(FORMAT);
    }
}
