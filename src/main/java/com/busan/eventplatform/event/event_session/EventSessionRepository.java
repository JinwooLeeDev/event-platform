package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventSessionRepository extends JpaRepository<EventSession, Long> {

    // 행사 ID별 회차 목록 조회
    @Query("""
            select s from EventSession s
            where s.event.id = :eventId
            order by s.startsAt asc
            """)
    List<EventSession> findByEventId(@Param("eventId") Long eventId);

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


}
