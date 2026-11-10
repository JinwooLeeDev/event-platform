package com.busan.eventplatform.event;

import com.busan.eventplatform.event.event_session.EventSessionRequest;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.global.error.exception.Exception400;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_sessions")
public class EventSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [추가] 행사 최대 정원
    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // 행사 시작일
    @Column(nullable = false)
    private LocalDateTime startsAt;

    // 행사 종료일
    @Column(nullable = false)
    private LocalDateTime endsAt;

    // 현재 정원 점유 인원
    @Column(nullable = false)
    private int occupiedCount;

    // 다음 접수 번호
    @Column(nullable = false)
    private int nextReceptionNumber;

    // 임시 정원(좌석?) 확보 시간(초)
    @Column(nullable = false)
    private int holdDurationSeconds;

    // 참가비 유형
    @Column(nullable = false)
    private String pricingType;

    // 참가비 포인트
    @Column(nullable = false)
    private int fee;

    // 예약 보증금 포인트
    @Column(nullable = false)
    private int reservationDepositPoints;

    // 최소 진행 인원
    @Column(nullable = false)
    private int minimumParticipants;

    // 최소 인원 확인 시점
    @Column(nullable = false)
    private LocalDateTime minimumCheckAt;

    // 회차 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventSessionStatus status;

    // 생성일
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성 시각 넣기
    @PrePersist // 처음 저장하기 직전에 실행 -- 생성 시 자동 채움
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 회차 수정
    public void update(EventSessionRequest.UpdateDto dto) {
        this.name = dto.getName();
        this.capacity = dto.getCapacity();
        this.startsAt = dto.getStartsAt();
        this.endsAt = dto.getEndsAt();
        this.holdDurationSeconds = dto.getHoldDurationSeconds();
        this.pricingType = dto.getPricingType();
        this.fee = dto.getFee();
        this.reservationDepositPoints = dto.getReservationDepositPoints();
        this.minimumParticipants = dto.getMinimumParticipants();
        this.minimumCheckAt = dto.getMinimumCheckAt();
    }

    // =========================================================
    // [참가 신청 파트 추가] 정원 / 접수번호 관리
    // 아래 메서드는 모두 비관적 락(findByIdForUpdate)으로 조회한 엔티티에서만 호출할 것
    // =========================================================

    /** 잔여 정원이 있는지 */
    public boolean hasRemainingSeat() {
        return occupiedCount < capacity;
    }

    /** 정원 1자리 점유 (확정/승인대기로 들어갈 때) */
    public void occupySeat() {
        if (!hasRemainingSeat()) {
            throw new Exception400("정원이 초과되었습니다");
        }
        this.occupiedCount++;
    }

    /** 정원 1자리 반환 (확정/승인대기 상태에서 취소·반려될 때) */
    public void releaseSeat() {
        if (occupiedCount <= 0) {
            throw new IllegalStateException("점유 인원이 0인 회차의 정원을 반환할 수 없습니다. sessionId=" + id);
        }
        this.occupiedCount--;
    }

    /** 접수 번호 발급 후 다음 번호로 증가 */
    public int issueReceptionNumber() {
        return this.nextReceptionNumber++;
    }
}