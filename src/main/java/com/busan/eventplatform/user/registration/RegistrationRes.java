package com.busan.eventplatform.user.registration;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

        public static MyRegistrationDto of(Registration registration, Integer waitingNumber) {
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
                    .cancelable(status.isActive())
                    .createdAt(registration.getCreatedAt())
                    .build();
        }
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


    // todo - 아래의 메서드는 임시 상세 화면을 지울 때 삭제 여부 함께 판단하기. 지금 formatSchedule을 쓰는 곳은 다음 단계에서 만들 임시 테스트 상세 화면(/test/events/{id}) 에서만 활용
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy. MM. dd (E) HH:mm", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 예: 2026. 12. 20 (일) 14:00 ~ 16:00
     */
    public static String formatSchedule(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt == null) {
            return "";
        }
        String start = startsAt.format(DATE_FORMAT);
        if (endsAt == null) {
            return start;
        }
        boolean sameDay = startsAt.toLocalDate().equals(endsAt.toLocalDate());
        return start + " ~ " + (sameDay ? endsAt.format(TIME_FORMAT) : endsAt.format(DATE_FORMAT));
    }

}