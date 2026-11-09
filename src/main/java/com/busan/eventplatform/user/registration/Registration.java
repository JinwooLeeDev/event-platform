package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 참가 신청 원장

@Entity
@Table(name = "registrations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Registration {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [추가] 행사 회차 연관 관계 매핑 (지연 로딩)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private EventSession session; // todo - 추후 윤아씨 구현 예정인 점 확인 완료

    // 신청자 연관관계 매핑(지연 로딩)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    // 대기 번호 (CONFIRMED 등은 null, WAITING 상태인 경우에만 값 할당)
    @Column(name = "queue_number", nullable = true)
    private Integer queueNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder
    public Registration(EventSession session, User user, RegistrationStatus status, Integer queueNumber) {
        this.session = session;
        this.user = user;
        this.status = status;
        this.queueNumber = queueNumber;
    }


    // 엔티티 영속화 시 시간 자동 기록
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // 엔티티 수정 시 시간 자동 기록
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // 대기자를 승격시킬 때 객체 지향적으로 상태를 변경하는 비즈니스 메서드
    // 객체 지향(자신의 상태를 책임지고 능동적으로 변경)
    public void promoteTo(RegistrationStatus targetStatus) {
        this.status = targetStatus;
        this.queueNumber = null; // 승격 되었으므로 대기 번호 회수 (null 처리)
    }

    // === 머스태치 화면 렌더링용 편의 메서드 추가 ===
    public boolean isConfirmed() {
        return this.status == RegistrationStatus.CONFIRMED; // (참가 확정 상태 이넘명에 맞게 조절)
    }

    public boolean isWaiting() {
        return this.status == RegistrationStatus.WAITING; // (대기 상태 이넘명에 맞게 조절)
    }

    public boolean isCancelled() {
        return this.status == RegistrationStatus.CANCELLED; // (취소 상태 이넘명에 맞게 조절)
    }

}