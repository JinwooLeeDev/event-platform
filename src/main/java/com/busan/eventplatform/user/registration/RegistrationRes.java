package com.busan.eventplatform.user.registration;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public class RegistrationRes {

    /**
     * 참가 신청 결과 (detail.mustache 팝업용)
     */
    @Getter
    @Builder
    public static class ApplyDto {
        private Long registrationId;
        private RegistrationStatus status;
        private String statusDescription;
        private Integer waitingNumber; // WAITING 일 때만 값 존재
        private String message;

        public static ApplyDto of(Registration registration, Integer waitingNumber) {
            RegistrationStatus status = registration.getStatus();
            String message = switch (status) {
                case CONFIRMED -> "참가 신청이 확정되었습니다";
                case PENDING -> "참가 신청이 접수되었습니다. 주최자 승인을 기다려 주세요";
                case WAITING -> "정원이 마감되어 대기열 " + waitingNumber + "번으로 등록되었습니다";
                default -> "";
            };
            return ApplyDto.builder()
                    .registrationId(registration.getId())
                    .status(status)
                    .statusDescription(status.getDescription())
                    .waitingNumber(waitingNumber)
                    .message(message)
                    .build();
        }
    }

    /**
     * 마이페이지 신청 내역 한 건
     */
    @Getter
    @Builder
    public static class MyRegistrationDto {
        private Long registrationId;
        private Long eventId;
        private String eventTitle;
        private Long sessionId;
        private String sessionName;
        private Integer receptionNumber;
        private RegistrationStatus status;
        private String statusDescription;
        private Integer waitingNumber;
        private boolean cancelable; // mustache 에서 취소 버튼 노출 여부
        private LocalDateTime createdAt;

        // cancelable: 서비스의 isCancelable() 결과를 받음 (활성 + 회차 시작 전) — /myhistory 기준과 통일
        public static MyRegistrationDto of(Registration registration, Integer waitingNumber, boolean cancelable) {
            RegistrationStatus status = registration.getStatus();
            return MyRegistrationDto.builder()
                    .registrationId(registration.getId())
                    .eventId(registration.getEventSession().getEvent().getId())
                    .eventTitle(registration.getEventSession().getEvent().getTitle())
                    .sessionId(registration.getEventSession().getId())
                    .sessionName(registration.getEventSession().getName())
                    .receptionNumber(registration.getReceptionNumber())
                    .status(status)
                    .statusDescription(status.getDescription())
                    .waitingNumber(waitingNumber)
                    .cancelable(cancelable)
                    .createdAt(registration.getCreatedAt())
                    .build();
        }
    }

    /** [나의 신청 내역 /myhistory] 화면 전체 — 요약 카드·탭 개수·목록 (머스태치용, null 없음) */
    @Getter
    @Builder
    public static class MyHistoryDto {
        private int totalCount;      // 요약 카드·탭: 전체
        private int confirmedCount;  // 요약 카드: 참가 확정 (시작 전)
        private int waitingCount;    // 요약 카드·탭: 대기 중
        private int upcomingCount;   // 탭: 예정 (확정, 시작 전)
        private int pastCount;       // 탭: 지난/취소 (시작됨·취소·거절)
        private String currentTab;   // all / upcoming / waiting / past
        private String keyword;      // 검색어 (없으면 "")
        private List<MyHistoryItemDto> items;
        private boolean empty;       // 목록이 비었으면 true → 빈 화면 안내
    }

    /** [나의 신청 내역 /myhistory] 신청 카드 한 건 — 문구·금액은 서비스에서 상황별로 계산해 문자열로 전달 */
    @Getter
    @Builder
    public static class MyHistoryItemDto {
        private Long registrationId;
        private Long eventId;
        private String category;           // 행사 카테고리명
        private String title;              // 행사명
        private String sessionName;        // 회차명
        private String place;              // 행사 주소
        private String scheduleText;       // 2026. 12. 20 (일) · 오후 2:00
        private String tab;                // upcoming / waiting / past (탭 필터용)
        private String statusLabel;        // 참가 확정 / 대기열 등록 / 참가 취소 / 참가 거절 / 행사 종료
        private String statusClass;        // 배지 CSS 클래스 (status-confirmed 등)
        private String statusMessage;      // 상황별 안내 문구
        private boolean waiting;           // 대기 순번 표시 여부
        private int waitingRank;           // 대기 순번 (대기가 아니면 0)
        private String paidPointText;      // 결제 포인트 "25,000P"
        private String purchasedAtText;    // 결제일 "2026. 11. 12" (결제 없으면 "")
        private String expectedRefundText; // 지금 취소하면 환불될 금액 (취소 가능할 때만, 아니면 "")
        private String refundText;         // 환불 완료 금액 — TODO step 8: 포인트 파트 환불 필드 연결
        private boolean cancelable;        // 참가 취소 버튼 표시 여부
        private String cancelConfirmText;  // 취소 버튼 확인 창 문구 (환불 예정 금액 포함)
        private String cancelUrl;          // /registrations/cancel/{id} (취소 불가면 "")
        private String detailUrl;          // /event/{eventId}
    }



    /**
     * 주최자 관리 화면 참가자 한 명
     */
    @Getter
    @Builder
    public static class ParticipantDto {
        private Long registrationId;
        private Long userId;
        private String displayName; // 닉네임
        private RegistrationStatus status;
        private String statusDescription;
        private boolean rejectable;
        private LocalDateTime createdAt;

        public static ParticipantDto of(Registration registration) {
            RegistrationStatus status = registration.getStatus();
            return ParticipantDto.builder()
                    .registrationId(registration.getId())
                    .userId(registration.getUser().getId())
                    .displayName(registration.getUser().getDisplayName())
                    .status(status)
                    .statusDescription(status.getDescription())
                    .rejectable(status.isActive())
                    .createdAt(registration.getCreatedAt())
                    .build();
        }
    }

    /** 신청 가능 여부 + 포인트 확인 (행사 상세: 충전 / 차감 화면 분기용) */
    @Getter
    @Builder
    public static class CheckDto {
        private Long sessionId;
        private int fee;               // 참가비
        private int currentPoint;      // 보유 포인트
        private boolean canApply;      // 신청 불가 사유가 없는지 (모집 기간, 중복 신청 등)
        private boolean enoughPoint;   // 보유 포인트 >= 참가비
        private boolean waitlist;      // 정원 마감 → 대기 신청으로 접수
        private String message;        // 신청 불가 사유 (신청 가능하면 빈 문자열)

        public static CheckDto of(Long sessionId, int fee, int currentPoint,
                                  int remainingSeats, String blockedReason) {
            return CheckDto.builder()
                    .sessionId(sessionId)
                    .fee(fee)
                    .currentPoint(currentPoint)
                    .canApply(blockedReason == null)
                    .enoughPoint(currentPoint >= fee)
                    .waitlist(remainingSeats <= 0)
                    .message(blockedReason == null ? "" : blockedReason)
                    .build();
        }
    }


    // 일정 표시 형식 (구매내역 시안 기준) — 예: 2026. 11. 21 (토) · 오후 6:00
    // 사용처: 임시 테스트 상세 화면(/test/events/{id}), 구매내역 카드(MyHistoryItemDto, 10단계 예정)
    private static final DateTimeFormatter SCHEDULE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy. MM. dd (E) · a h:mm", Locale.KOREAN);

    /**
     * 회차 일정 표시 문자열 (시안에 맞춰 시작 시각만 표시)
     * - startsAt 이 없으면 빈 문자열 (머스태치 null 방지)
     * - endsAt 은 시안에서 쓰지 않지만, 호출부를 바꾸지 않도록 파라미터는 유지
     */
    public static String formatSchedule(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null) {
            return "";
        }
        return startsAt.format(SCHEDULE_FORMAT);
    }

}