package com.busan.eventplatform.event;

import com.busan.eventplatform.event.events.Event;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "event_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [추가] 행사 최대 정원
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    // 행사 회차명
    @Column(nullable = false)
    private String name;

    // 행사와의 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 신청 시작일
    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;

    // 신청 마감일
    @Column(name = "registration_closes_at")
    private Instant registrationClosesAt;

    // 행사 시작일
    @Column(name = "starts_at")
    private Instant startsAt;

    // 행사 종료일
    @Column(name = "ends_at")
    private Instant endsAt;

    // 현재 정원 점유 인원
    @Column(name = "occupied_count", nullable = false)
    private Integer occupiedCount;

    // 다음 접수 번호
    @Column(name = "next_reception_number", nullable = false)
    private Integer nextReceptionNumber;

    // 임시 정원 확보 시간(초)
    @Column(name = "hold_duration_seconds")
    private Integer holdDurationSeconds;

    // 참가비 유형
    @Column(name = "pricing_type")
    private String pricingType;

    // 참가비 포인트
    @Column(name = "fee")
    private Integer fee;

    // 예약 보증금 포인트
    @Column(name = "reservation_deposit_points")
    private Integer reservationDepositPoints;

    // 최소 진행 인원
    @Column(name = "minimum_participants")
    private Integer minimumParticipants;

    // 최소 인원 확인 시점
    @Column(name = "minimum_check_at")
    private Instant minimumCheckAt;

    // 회차 상태
    @Column
    private String status;

    // 생성일
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}