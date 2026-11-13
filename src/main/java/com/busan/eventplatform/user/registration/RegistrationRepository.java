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

    /** [나의 신청 내역 /myhistory] 회차·행사·카테고리·결제를 한 번에 조회 (N+1 방지), 최신 신청순
     *  결제 없는 신청(예전 데이터)도 나오도록 결제는 left join */
    @Query("""
            select r from Registration r
            join fetch r.eventSession s
            join fetch s.event e
            join fetch e.category
            left join fetch r.purchase
            where r.user.id = :userId
            order by r.id desc
            """)
    List<Registration> findAllByUserIdForHistory(@Param("userId") Long userId);

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

    // 특정 회원이 특정 행사의 참가 확정 상태인지 확인
    @Query("""
    select count(r)
    from Registration r
    where r.eventSession.event.id = :eventId
    and r.user.id = :userId
    and r.status = :status
""")
    long countConfirmedByEventIdAndUserId(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId,
            @Param("status") RegistrationStatus status
    );

    // 행사 회차별 단일 상태 (확정 or 대기) 인원수 조회 (편의성 메서드)
    long countByEventSessionIdAndStatus(Long sessionId, RegistrationStatus status);

    // 행사 전체 회차의 특정 상태(확정 or 대기) 인원수 합산
    // Registration -> session(EventSession) -> event(Event) 로 탐색하여 합산
    @Query("select count(r) from Registration r where r.eventSession.event.id = :eventId and r.status = :status")
    long countTotalByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") RegistrationStatus status);

    /** 신청한 회차 id만 조회 (락을 먼저 잡기 위해 신청 엔티티를 불러오지 않음) */
    @Query("select r.eventSession.id from Registration r where r.id = :registrationId")
    Optional<Long> findSessionIdById(@Param("registrationId") Long registrationId);



}