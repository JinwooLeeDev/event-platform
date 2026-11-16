package com.busan.eventplatform.user.community;

import java.time.LocalDateTime;

/**
 * [f/40] 커뮤니티 운영 기간 규칙 (Q4·Q10)
 * - 각 회차의 종료일 '24시(다음 날 00:00)'까지 열려 있음
 *   예) 회차 종료 11/23 17:00 → 11/23 23:59:59까지 이용 → 11/24 00:00 종료
 * - 화면 진입·내 행사 카드 링크·(5단계) 종료 스케줄러가 모두 이 규칙을 같이 사용
 */

public class CommunityPolicy {

    private CommunityPolicy() {
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
