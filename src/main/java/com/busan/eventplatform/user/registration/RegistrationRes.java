package com.busan.eventplatform.user.registration;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class RegistrationRes {

    /** 참가 신청 결과 (detail.mustache 팝업용) */
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

    /** 마이페이지 신청 내역 한 건 */
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

    /** 주최자 관리 화면 참가자 한 명 */
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
}