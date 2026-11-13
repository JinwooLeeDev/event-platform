package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class EventSessionResponse {

    // 특정 행사 회차 목록
    @Data
    public static class ListDto {
        private List<EventSessionDto> eventSessionList;
        public ListDto(List<EventSession> eventSessionList) {
            this.eventSessionList = eventSessionList.stream()
                    .map(EventSessionDto::new)
                    .toList();
        }
    }

    // 회차 한 개 정보
    @Data
    public static class EventSessionDto {
        private Long id;
        private String name;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private int capacity;
        private int occupiedCount;
        private int fee;
        private String pricingType;
        private EventSessionStatus status;
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;
        private boolean canApply;
        private String closedReason;

        public EventSessionDto(EventSession eventSession) {
            this(eventSession, null);
        }

        public EventSessionDto(EventSession eventSession, String closedReason) {
            this.id = eventSession.getId();
            this.name = eventSession.getName();
            this.startsAt = eventSession.getStartsAt();
            this.endsAt = eventSession.getEndsAt();
            this.capacity = eventSession.getCapacity();
            this.occupiedCount = eventSession.getOccupiedCount();
            this.fee = eventSession.getFee();
            this.pricingType = eventSession.getPricingType();
            this.status = eventSession.getStatus();
            this.applyStartAt = eventSession.getApplyStartAt();
            this.applyEndAt = eventSession.getApplyEndAt();
            this.closedReason = closedReason;
            this.canApply = (closedReason == null);
        }
    }

    // 행사 회차 상세 조회
    @Data
    public static class DetailDto {
        private Long id;
        private Long eventId;
        private String name;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private int capacity;
        private int occupiedCount;
        private int nextReceptionNumber;
        private int holdDurationSeconds;
        private String pricingType;
        private int fee;
        private int reservationDepositPoints;
        private int minimumParticipants;
        private LocalDateTime minimumCheckAt;
        private EventSessionStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime applyStartAt;
        private LocalDateTime applyEndAt;
        private boolean applicationClosed;


        public DetailDto(EventSession eventSession) {
            this.id = eventSession.getId();
            if (eventSession.getEvent() != null) {
                this.eventId = eventSession.getEvent().getId();
            }
            this.name = eventSession.getName();
            this.startsAt = eventSession.getStartsAt();
            this.endsAt = eventSession.getEndsAt();
            this.capacity = eventSession.getCapacity();
            this.occupiedCount = eventSession.getOccupiedCount();
            this.nextReceptionNumber = eventSession.getNextReceptionNumber();
            this.holdDurationSeconds = eventSession.getHoldDurationSeconds();
            this.pricingType = eventSession.getPricingType();
            this.fee = eventSession.getFee();
            this.reservationDepositPoints = eventSession.getReservationDepositPoints();
            this.minimumParticipants = eventSession.getMinimumParticipants();
            this.minimumCheckAt = eventSession.getMinimumCheckAt();
            this.status = eventSession.getStatus();
            this.createdAt = eventSession.getCreatedAt();
            this.applyStartAt = eventSession.getApplyStartAt();
            this.applyEndAt = eventSession.getApplyEndAt();
            this.applicationClosed = eventSession.isApplicationClosed();

        }
    }

    @Data
    public static class UpdateFormDto {
        private Long id;
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

        public UpdateFormDto(EventSession eventSession) {
            this.id = eventSession.getId();
            this.name = eventSession.getName();
            this.startsAt = eventSession.getStartsAt();
            this.endsAt = eventSession.getEndsAt();
            this.capacity = eventSession.getCapacity();
            this.holdDurationSeconds = eventSession.getHoldDurationSeconds();
            this.pricingType = eventSession.getPricingType();
            this.fee = eventSession.getFee();
            this.reservationDepositPoints = eventSession.getReservationDepositPoints();
            this.minimumParticipants = eventSession.getMinimumParticipants();
            this.minimumCheckAt = eventSession.getMinimumCheckAt();
            this.applyStartAt = eventSession.getApplyStartAt();
            this.applyEndAt = eventSession.getApplyEndAt();
        }
    }
}
