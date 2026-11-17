package com.busan.eventplatform.user.community;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * [f/40] 커뮤니티 운영 기간 규칙 (Q4·Q10·Q11) + 종료일 당일 안내(5-2)
 * - 각 회차의 종료일 '24시(다음 날 00:00)'까지 열려 있음
 *   예) 회차 종료 11/23 17:00 → 11/23 23:59:59까지 이용 → 11/24 00:00 종료
 * - 화면 진입·STOMP 구독·전송·내 행사 카드 링크·종료 스케줄러가 모두 이 규칙을 같이 사용
 * - 기준 시간대는 Asia/Seoul 고정 (조장님 확인 11-16) — 서버(JVM) 시간대가 UTC여도 한국 시각으로 계산
 */
public final class CommunityPolicy {

    /** 커뮤니티 시각 기준 (스케줄러 zone 과 같은 값) */
    public static final String ZONE_NAME = "Asia/Seoul";
    public static final ZoneId ZONE = ZoneId.of(ZONE_NAME);

    /**
     * Q11-1: 커뮤니티 종료 시 채팅 메시지를 DB에서 삭제할지
     * - true(제안안): 삭제 / 멤버십은 left_reason=CLOSED 기록으로 남김
     * - 조장님 결정이 '보관'이면 false 로만 바꾸면 됨
     */
    public static final boolean DELETE_MESSAGES_ON_CLOSE = true;

    /** ★ [5-2] 종료일 당일 안내 시각 (Asia/Seoul) — 스케줄러 기본 cron "0 0 11 * * *" 과 같은 값 */
    public static final int END_DAY_NOTICE_HOUR = 11;

    /** ★ [5-2] 종료일 당일 안내 문구 (화면에서는 줄바꿈 유지: white-space: pre-line) */
    public static final String END_DAY_NOTICE =
            "📢 오늘은 행사 종료일입니다.\n"
                    + "커뮤니티는 오늘 24시에 대화 내용과 함께 모두 삭제되니 일정에 참고해 주세요.\n"
                    + "오늘도 즐거운 관람 되세요 :)";

    private CommunityPolicy() {
    }

    /** 지금 한국 시각 — 커뮤니티 코드에서는 LocalDateTime.now() 대신 이것을 사용 */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** 커뮤니티가 닫히는 시각 = 회차 종료일 다음 날 00:00 */
    public static LocalDateTime closesAt(LocalDateTime sessionEndsAt) {
        return sessionEndsAt.toLocalDate().plusDays(1).atStartOfDay();
    }

    /** 지금 열려 있는지 (종료 시각이 없으면 닫힌 것으로 봄) */
    public static boolean isOpen(LocalDateTime sessionEndsAt, LocalDateTime now) {
        return sessionEndsAt != null && now.isBefore(closesAt(sessionEndsAt));
    }

    /** ★ [5-2] 오늘이 회차 종료일인지 (한국 날짜 기준) */
    public static boolean isEndDay(LocalDateTime sessionEndsAt, LocalDateTime now) {
        return sessionEndsAt != null && sessionEndsAt.toLocalDate().equals(now.toLocalDate());
    }

    /**
     * ★ [5-2] 방 상단에 종료일 안내를 보여줄지 — 종료일 당일 11:00 ~ 24:00
     * (11:00 실시간 알림을 놓친 사람도 그 뒤에 들어오면 보이도록)
     */
    public static boolean showsEndDayNotice(LocalDateTime sessionEndsAt, LocalDateTime now) {
        return isEndDay(sessionEndsAt, now) && now.getHour() >= END_DAY_NOTICE_HOUR;
    }

    /**
     * 종료 대상 기준 — 회차 종료 시각이 '오늘 00:00 이전'이면 이미 닫힌 방
     * (isOpen 과 같은 규칙을 DB 조회 조건으로 바꾼 것: endsAt < 오늘 00:00 ⇔ closesAt(endsAt) <= now)
     */
    public static LocalDateTime closedIfEndedBefore(LocalDateTime now) {
        return now.toLocalDate().atStartOfDay();
    }
}