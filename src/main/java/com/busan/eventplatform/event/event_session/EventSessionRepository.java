package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.api.purchase.DepositStatus;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {

    @Query(value = "select s from EventSession s join fetch s.event e join fetch e.company where (:status is null or s.status = :status) order by s.startsAt desc, s.id desc",
            countQuery = "select count(s) from EventSession s where (:status is null or s.status = :status)")
    org.springframework.data.domain.Page<EventSession> findForAdmin(@Param("status") EventSessionStatus status,
            org.springframework.data.domain.Pageable pageable);

    // 행사 ID별 회차 목록 조회
    @Query("""
            select s from EventSession s
            where s.event.id = :eventId
            order by s.startsAt asc
            """)
    List<EventSession> findByEventId(@Param("eventId") Long eventId);

    // 여러 행사 ID에 해당하는 회차를 한 번에 조회
    @Query("""
            select s from EventSession s
            where s.event.id in :eventIds
            order by s.event.id asc, s.startsAt asc
            """)
    List<EventSession> findAllByEventIds(@Param("eventIds") List<Long> eventIds);

    // 회차 상태별 조회
    @Query("""
        select s from EventSession s
        where s.event.id = :eventId
        and s.status = :status
        """)
    List<EventSession> findAllByEventIdAndStatus(
            @Param("eventId") Long eventId,
            @Param("status") EventSessionStatus status
    );

    // [참가 신청 파트 추가] 정원 확인 ~ 신청 저장까지 같은 회차에 대한 동시 신청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from EventSession s 
            join fetch s.event 
            where s.id = :sessionId
        """)
    Optional<EventSession> findByIdForUpdate(@Param("sessionId") Long sessionId);


    @Query("""
            select es from EventSession es join fetch es.event e join fetch e.category c
            where es.id = :eventSessionId
            """)
    Optional<EventSession> findByIdWithEvent(@Param("eventSessionId") Long eventSessionId);


    @Query("""
        select es from EventSession es where es.event.company.id = :companyId and es.id = :sessionId
""")
    Optional<EventSession> findByCompanyIdAndSessionId(@Param("companyId") Long companyId , @Param("sessionId") Long sessionId);

    @Query("""
            select es from EventSession es 
            where es.event.id = :eventId and not exists (
            select d from Deposit d where d.eventSession = es and d.status = :status
            )
""")
    int countNotPaidSession(@Param("eventId") Long eventId , @Param("status") DepositStatus status);


    // 신청 이력이 없는 회차만 실제 삭제
    @Modifying
    @Query("""
            delete from EventSession es
            where es.id = :sessionId
            and not exists (
                select r
                from Registration r
                where r.eventSession.id = es.id
            )
            """)
    int deleteIfNoRegistrationHistory(@Param("sessionId") Long sessionId);

}
