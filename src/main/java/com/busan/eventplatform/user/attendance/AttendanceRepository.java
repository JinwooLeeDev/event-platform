package com.busan.eventplatform.user.attendance;

import com.busan.eventplatform.user.registration.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // ★ [f/42] 같은 신청이 이미 출석했는지 (중복 출석 방지)
    boolean existsByRegistrationId(Long registrationId);

    // ★ [f/42] 회원이 회차에 출석했는지 — 신청 상태가 status(참가 확정)인 출석만 인정 (후기 작성 검증용)
    @Query("select count(a) > 0 from Attendance a join a.registration r " +
            "where r.user.id = :userId and r.eventSession.id = :sessionId and r.status = :status")
    boolean existsAttended(@Param("userId") Long userId,
                           @Param("sessionId") Long sessionId,
                           @Param("status") RegistrationStatus status);

    // ★ [f/42] 회원이 출석한 회차 id 목록 (최근 출석순, 후기 탭용)
    @Query("select r.eventSession.id from Attendance a join a.registration r " +
            "where r.user.id = :userId and r.status = :status order by a.attendedAt desc")
    List<Long> findAttendedSessionIds(@Param("userId") Long userId,
                                      @Param("status") RegistrationStatus status);

    // ★ [f/42] 신청 id 목록의 출석 시각 — [신청 id, 출석 시각] (목록 화면에서 쿼리 1번으로 조회)
    @Query("select a.registration.id, a.attendedAt from Attendance a where a.registration.id in :registrationIds")
    List<Object[]> findAttendedAtByRegistrationIds(@Param("registrationIds") Collection<Long> registrationIds);

    // ★ [f/42] 회차의 출석 인원 (참가 확정 신청 기준, QR 화면 요약용)
    @Query("select count(a) from Attendance a join a.registration r " +
            "where r.eventSession.id = :sessionId and r.status = :status")
    long countAttended(@Param("sessionId") Long sessionId, @Param("status") RegistrationStatus status);
}