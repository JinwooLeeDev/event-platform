package com.busan.eventplatform.user.attendance;


import com.busan.eventplatform.user.registration.Registration;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
@Getter
@NoArgsConstructor
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "checked_by", nullable = false)
    private Long checkedBy;

    @Column(name = "attended_at", nullable = false)
    private LocalDateTime attendedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * ★ [f/42] QR 출석 기록 생성 — 참가 확정 신청 1건당 1번만 저장한다 (출석 취소 없음)
     *
     * @param registration 출석한 참가 확정 신청
     * @param checkedBy    출석 확인자 — QR 을 띄운 주최 기업 id (시드 데이터와 같은 의미)
     */
    public static Attendance create(Registration registration, Long checkedBy) {
        Attendance attendance = new Attendance();
        attendance.registration = registration;
        attendance.attemptNo = 1;          // 재출석이 없으므로 항상 1 (같은 신청 중복 출석은 서비스에서 차단)
        attendance.checkedBy = checkedBy;
        return attendance;
    }
}