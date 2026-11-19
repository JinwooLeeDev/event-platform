package com.busan.eventplatform.user.attendance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * ★ [f/42] 출석 규칙 한 곳 — 출석 가능 시간 · 후기 작성 화면 주소
 * <p>
 * - 열림: 회차 시작 30분 전부터 (현장 입장 대기 시간)
 * - 닫힘: 회차 종료 시각까지 (종료 시각이 없으면 시작일 24시까지)
 * - 기준 시각은 신청 파트와 같은 LocalDateTime.now() (본인 취소·기업 거절의 '시작 전' 판단과 통일)
 */
public final class AttendancePolicy {

    public static final Duration OPEN_BEFORE_START = Duration.ofMinutes(30);

    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("MM. dd a h:mm", Locale.KOREAN);

    private AttendancePolicy() {
    }

    public static LocalDateTime opensAt(LocalDateTime startsAt) {
        return startsAt.minus(OPEN_BEFORE_START);
    }

    public static LocalDateTime closesAt(LocalDateTime startsAt, LocalDateTime endsAt) {
        return endsAt != null ? endsAt : startsAt.toLocalDate().plusDays(1).atStartOfDay();
    }

    /** 지금 출석할 수 있는 시간인지 */
    public static boolean isCheckTime(LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime now) {
        if (startsAt == null) {
            return false;
        }
        return !now.isBefore(opensAt(startsAt)) && now.isBefore(closesAt(startsAt, endsAt));
    }

    /** 화면 안내 "출석 가능 시간: 11. 27 오후 12:39 ~ 11. 27 오후 6:00" (시작 시각이 없으면 null) */
    public static String checkTimeText(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null) {
            return null;
        }
        return "출석 가능 시간: " + opensAt(startsAt).format(TIME_TEXT) + " ~ " + closesAt(startsAt, endsAt).format(TIME_TEXT);
    }

    /**
     * 후기 작성 화면 주소 ([후기 남기러 가기] 버튼)
     * ⚠ 후기 담당이 주소를 확정하면 이 한 줄만 바꾼다
     */
    public static String reviewUrl(Long sessionId) {
        return "/reviews/write/" + sessionId;
    }
}