package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.event.EventSession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_cancels")
public class EventCancel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;

    // 취소 신청 사유
    @Column(nullable = false)
    private String reason;

    // 취소 신청 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventCancelStatus status;

    // 취소 신청 승인 처리
    public void approve() {
        this.status = EventCancelStatus.APPROVED;
    }

    // 취소 신청 거절 처리
    public void reject() {
        this.status = EventCancelStatus.REJECTED;
    }

    // 신청서 제출 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성 시각 자동 저장
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = EventCancelStatus.PENDING;
        }
    }
}
