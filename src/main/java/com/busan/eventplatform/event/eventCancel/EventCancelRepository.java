package com.busan.eventplatform.event.eventCancel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventCancelRepository extends JpaRepository<EventCancel, Long> {

    // 해당 회차에 검토 대기 중인 취소 신청이 있는지 확인
    @Query("""
        select count(ec) > 0
        from EventCancel ec
        where ec.eventSession.id = :sessionId
        and ec.status = :status
        """)
    boolean existsBySessionIdAndStatus(
            @Param("sessionId") Long sessionId,
            @Param("status") EventCancelStatus status
    );
}
