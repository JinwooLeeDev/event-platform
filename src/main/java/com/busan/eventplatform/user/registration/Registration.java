package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 행사 회차 참가 신청.
 * 결제 정보(금액, 포인트)는 Purchase 도메인이 관리하므로 이 엔티티에는 두지 않는다.
 */


@Entity
@Table(name = "registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false)
    private EventSession eventSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    /** 회차 내 접수 번호 (EventSession.nextReceptionNumber 에서 발급). 대기열 순서 기준 */
    @Column(name = "reception_number", nullable = false)
    private Integer receptionNumber;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 개인회원의 행사 참가 신청을 위해 생성된 결제 내역 (신청 1건 = 결제 1건, 기존 데이터는 null 가능)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    private Registration(User user, EventSession eventSession, Purchase purchase, RegistrationStatus status, Integer receptionNumber) {
        this.user = user;
        this.eventSession = eventSession;
        this.purchase = purchase;
        this.status = status;
        this.receptionNumber = receptionNumber;
    }

    public static Registration create(User user, EventSession eventSession, Purchase purchase,
                                      RegistrationStatus status, int receptionNumber) {
        if (!status.isActive()) {
            throw new IllegalArgumentException("신청 생성 시 상태는 CONFIRMED/PENDING/WAITING 중 하나여야 합니다");
        }
        return new Registration(user, eventSession, purchase, status, receptionNumber);
    }

    // ===== 상태 변경 =====

    /** 대기열 → 확정/승인대기 승격 */
    public void promote(RegistrationStatus nextStatus) {
        if (this.status != RegistrationStatus.WAITING) {
            throw new Exception400("대기열 상태인 신청만 승격할 수 있습니다");
        }
        if (!nextStatus.isSeatOccupying()) {
            throw new IllegalArgumentException("승격 대상 상태는 CONFIRMED 또는 PENDING 이어야 합니다");
        }
        this.status = nextStatus;
    }

    /** 유저 본인 취소 */
    public void cancel() {
        validateActive("이미 취소되었거나 반려된 신청입니다");
        this.status = RegistrationStatus.CANCELLED;
    }

    /** 주최자 반려 */
    public void reject() {
        validateActive("이미 취소되었거나 반려된 신청입니다");
        this.status = RegistrationStatus.REJECTED;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    private void validateActive(String message) {
        if (!this.status.isActive()) {
            throw new Exception400(message);
        }
    }

}