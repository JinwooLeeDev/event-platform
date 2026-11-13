package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Data;

import java.time.LocalDateTime;

public class EventSessionRequest {

    @Data
    public static class SaveDto {
        private String name;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private int capacity;
        private int holdDurationSeconds;
        private String pricingType;
        private int fee;
        private int reservationDepositPoints;
        private int minimumParticipants;
        private LocalDateTime minimumCheckAt;
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;


        public void validate() {
            if (name == null || name.trim().isEmpty()) {
                throw new Exception400("회차 이름은 필수입니다.");
            }
            if (startsAt == null) {
                throw new Exception400("행사 시작일은 필수입니다.");
            }
            if (endsAt == null) {
                throw new Exception400("행사 종료일은 필수입니다.");
            }
            if (startsAt.isAfter(endsAt)) {
                throw new Exception400("행사 시작일은 종료일보다 늦을 수 없습니다.");
            }
            if (capacity < 1) {
                throw new Exception400("정원은 1명 이상이어야 합니다.");
            }
            if (holdDurationSeconds < 0) {
                throw new Exception400("임시 정원 확보 시간은 0 이상이어야 합니다.");
            }
            if (pricingType == null || pricingType.trim().isEmpty()) {
                throw new Exception400("참가비 유형은 필수입니다.");
            }
            if (fee < 0) {
                throw new Exception400("참가비는 0 이상이어야 합니다.");
            }
            if (reservationDepositPoints < 0) {
                throw new Exception400("예약 보증금은 0 이상이어야 합니다.");
            }
            if (minimumParticipants < 1 || minimumParticipants > capacity) {
                throw new Exception400("최소 진행 인원은 정원 이하이어야 합니다.");
            }
            if (minimumCheckAt == null) {
                throw new Exception400("최소 인원 확인 시점은 필수입니다.");
            }
            if (applyStartAt == null) {
                throw new Exception400("모집 시작일은 필수입니다.");
            }
            if (applyEndAt == null) {
                throw new Exception400("모집 마감일은 필수입니다.");
            }
            if (applyStartAt.isAfter(applyEndAt)) {
                throw new Exception400("모집 시작일은 마감일보다 늦을 수 없습니다.");
            }
        }

        public EventSession toEntity(Event event) {
            return EventSession.builder()
                    .event(event)
                    .name(this.name)
                    .startsAt(this.startsAt)
                    .endsAt(this.endsAt)
                    .capacity(this.capacity)
                    .occupiedCount(0)
                    .nextReceptionNumber(1)
                    .holdDurationSeconds(this.holdDurationSeconds)
                    .pricingType(this.pricingType)
                    .fee(this.fee)
                    .reservationDepositPoints(this.reservationDepositPoints)
                    .minimumParticipants(this.minimumParticipants)
                    .minimumCheckAt(this.minimumCheckAt)
                    .status(EventSessionStatus.SCHEDULED)
                    .applyStartAt(this.applyStartAt)
                    .applyEndAt(this.applyEndAt)
                    .build();
        }
    }

    @Data
    public static class UpdateDto {
        private String name;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private int capacity;
        private int holdDurationSeconds;
        private String pricingType;
        private int fee;
        private int reservationDepositPoints;
        private int minimumParticipants;
        private LocalDateTime minimumCheckAt;

        public void validate() {
            if (name == null || name.trim().isEmpty()) {
                throw new Exception400("회차 이름은 필수입니다.");
            }
            if (startsAt == null) {
                throw new Exception400("행사 시작일은 필수입니다.");
            }
            if (endsAt == null) {
                throw new Exception400("행사 종료일은 필수입니다.");
            }
            if (startsAt.isAfter(endsAt)) {
                throw new Exception400("행사 시작일은 종료일보다 늦을 수 없습니다.");
            }
            if (capacity < 1) {
                throw new Exception400("정원은 1명 이상이어야 합니다.");
            }
            if (holdDurationSeconds < 0) {
                throw new Exception400("임시 정원 확보 시간은 0 이상이어야 합니다.");
            }
            if (pricingType == null || pricingType.trim().isEmpty()) {
                throw new Exception400("참가비 유형은 필수입니다.");
            }
            if (fee < 0) {
                throw new Exception400("참가비는 0 이상이어야 합니다.");
            }
            if (reservationDepositPoints < 0) {
                throw new Exception400("예약 보증금은 0 이상이어야 합니다.");
            }
            if (minimumParticipants < 1 || minimumParticipants > capacity) {
                throw new Exception400("최소 진행 인원은 정원 이하이어야 합니다.");
            }
            if (minimumCheckAt == null) {
                throw new Exception400("최소 인원 확인 시점은 필수입니다.");
            }
        }
    }

}
