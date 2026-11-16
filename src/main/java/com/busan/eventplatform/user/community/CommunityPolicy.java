package com.busan.eventplatform.user.community;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * [f/40] 커뮤니티 운영 기간 규칙 (Q4·Q10)
 * - 각 회차의 종료일 '24시(다음 날 00:00)'까지 열려 있음
 *   예) 회차 종료 11/23 17:00 → 11/23 23:59:59까지 이용 → 11/24 00:00 종료
 * - 화면 진입·내 행사 카드 링크·(5단계) 종료 스케줄러가 모두 이 규칙을 같이 사용
 * - 기준 시간대는 Asia/Seoul 고정 (조장님 확인 11-16) — 서버(JVM) 시간대가 UTC여도 한국 시각으로 계산
 */
public final class CommunityPolicy {

    /** 커뮤니티 시각 기준 (스케줄러 zone 과 같은 값) */
    public static final String ZONE_NAME = "Asia/Seoul";
    public static final ZoneId ZONE = ZoneId.of(ZONE_NAME);

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
}
