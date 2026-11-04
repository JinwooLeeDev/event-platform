package com.busan.eventplatform.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Registration {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Column(name = "queue_number", nullable = false)
    private Long queueNumber;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 80)
    private String cancellationReason;

    @Column(name = "policy_snapshot", columnDefinition = "JSON", nullable = false)
    private String policySnapshot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    private Attendance attendance;

    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistrationHistory> histories = new ArrayList<>();

    @Builder
    public Registration(Long sessionId, User user, RegistrationStatus status, Long queueNumber, String policySnapshot) {
        this.sessionId = sessionId;
        this.user = user;
        this.status = status;
        this.queueNumber = queueNumber;
        this.policySnapshot = policySnapshot;
        this.appliedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();

        if (status == RegistrationStatus.CONFIRMED) {
            this.confirmedAt = this.appliedAt;
        }
    }

    public void updateStatus(RegistrationStatus newStatus, String reason) {
        this.status = newStatus;
        if (newStatus == RegistrationStatus.CONFIRMED) {
            this.confirmedAt = LocalDateTime.now();
        } else if (newStatus == RegistrationStatus.CANCELLED) {
            this.cancelledAt = LocalDateTime.now();
            this.cancellationReason = reason;
        }
    }
}