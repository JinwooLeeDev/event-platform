package com.busan.eventplatform.user.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CommunityNoticeRepository extends JpaRepository<CommunityNotice, Long> {

    /** 방(회차)의 공지 — 없으면 빈 값 */
    Optional<CommunityNotice> findByEventSessionId(Long eventSessionId);

    /** [5단계 종료] 종료된 방의 공지 일괄 삭제 — 회차 종료 시각이 기준 이전 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommunityNotice n where n.eventSession.id in " +
            "(select s.id from EventSession s where s.endsAt < :endedBefore)")
    int deleteAllEndedBefore(@Param("endedBefore") LocalDateTime endedBefore);
}
