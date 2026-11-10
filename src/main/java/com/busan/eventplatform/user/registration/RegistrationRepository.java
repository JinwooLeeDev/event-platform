package com.busan.eventplatform.user.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    /** 중복 신청 확인 (유효한 신청이 이미 있는지) */
    @Query("""
            select count(r) > 0 from Registration r
            where r.user.id = :userId
              and r.eventSession.id = :sessionId
              and r.status in :statuses
            """)
    boolean existsByUserAndSessionAndStatusIn(@Param("userId") Long userId,
                                              @Param("sessionId") Long sessionId,
                                              @Param("statuses") Collection<RegistrationStatus> statuses);

    /** 회차별 확정 인원 (행사 파트 제공용) */
    @Query("""
            select count(r) from Registration r
            where r.eventSession.id = :sessionId
              and r.status = com.busan.eventplatform.user.registration.RegistrationStatus.CONFIRMED
            """)
    long countConfirmedBySession(@Param("sessionId") Long sessionId);

    /** 행사 전체(모든 회차) 확정 인원 합계 (행사 파트 제공용) */
    @Query("""
            select count(r) from Registration r
            where r.eventSession.event.id = :eventId
              and r.status = com.busan.eventplatform.user.registration.RegistrationStatus.CONFIRMED
            """)
    long countConfirmedByEvent(@Param("eventId") Long eventId);

    /** 가장 먼저 대기열에 들어온 신청 (승격 대상) */
    Optional<Registration> findFirstByEventSessionIdAndStatusOrderByReceptionNumberAsc(Long sessionId, RegistrationStatus status);

    /** 내 앞에 있는 대기자 수 → 대기 번호 = 이 값 + 1 */
    @Query("""
            select count(r) from Registration r
            where r.eventSession.id = :sessionId
              and r.status = com.busan.eventplatform.user.registration.RegistrationStatus.WAITING
              and r.receptionNumber < :receptionNumber
            """)
    long countWaitingAhead(@Param("sessionId") Long sessionId,
                           @Param("receptionNumber") Integer receptionNumber);

    /** 마이페이지: 내 신청 내역 */
    @Query("""
            select r from Registration r
            join fetch r.eventSession s
            join fetch s.event
            where r.user.id = :userId
            order by r.id desc
            """)
    List<Registration> findAllByUserIdWithSession(@Param("userId") Long userId);

    /** 주최자 관리 화면: 회차별 참가자 목록 */
    @Query("""
            select r from Registration r
            join fetch r.user
            where r.eventSession.id = :sessionId
            order by r.receptionNumber asc
            """)
    List<Registration> findAllBySessionIdWithUser(@Param("sessionId") Long sessionId);

    /** [행사 상세용] 유저가 특정 회차들에 신청한 내역 (회차별 신청 여부 표시) */
    @Query("""
            select r from Registration r
            where r.user.id = :userId
              and r.eventSession.id in :sessionIds
            """)
    List<Registration> findAllByUserIdAndSessionIds(@Param("userId") Long userId,
                                                    @Param("sessionIds") List<Long> sessionIds);
}