package com.busan.eventplatform.user.registration;


import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "registration_history")
@Getter
@NoArgsConstructor
public class RegistrationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    // 등록 현황 관련 상태 변경
    @Enumerated(EnumType.STRING)
    @Column(name = "changed_status", nullable = false, length = 50)
    private RegistrationStatus changedStatus;

    // 상태를 변경한 주체 (시스템 자동 취소 등일 경우 null 허용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedByUser;

    // 변경 사유 (예: "주최자 반려", "선행 대기자 승격" 등)
    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public RegistrationHistory(Registration registration, RegistrationStatus changedStatus, User processedByUser, String reason) {
        this.registration = registration;
        this.changedStatus = changedStatus;
        this.processedByUser = processedByUser;
        this.reason = reason;
    }

    // 선착순 시스템에서의 절대적인 기준점
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}

