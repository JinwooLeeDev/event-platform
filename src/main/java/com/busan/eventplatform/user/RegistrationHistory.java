package com.busan.eventplatform.user;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private RegistrationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private RegistrationStatus toStatus;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "reason_code", nullable = false, length = 80)
    private String reasonCode;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public RegistrationHistory(Registration registration, RegistrationStatus from, RegistrationStatus to, Long actorId, String reasonCode) {
        this.registration = registration;
        this.attemptNo = registration.getAttemptNo();
        this.fromStatus = from;
        this.toStatus = to;
        this.actorId = actorId;
        this.reasonCode = reasonCode;
    }
}

