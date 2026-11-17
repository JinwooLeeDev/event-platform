package com.busan.eventplatform.user.community;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunityChatMessageRepository extends JpaRepository<CommunityChatMessage, Long> {

    /**
     * 회차 방의 최근 메시지 (최신순) — 작성자(회원·기업)를 같이 가져와 N+1 방지
     * ★ [6단계] 작성자가 둘 중 하나만 있으므로 left join fetch
     */
    @Query("select m from CommunityChatMessage m left join fetch m.user left join fetch m.company " +
            "where m.eventSession.id = :sessionId order by m.id desc")
    List<CommunityChatMessage> findRecent(@Param("sessionId") Long sessionId, Pageable pageable);

    /** [5단계] 메시지가 남아 있는 종료된 방 (멤버가 모두 나간 방도 메시지는 지워야 하므로 따로 조회) */
    @Query("select distinct m.eventSession.id from CommunityChatMessage m " +
            "where m.eventSession.endsAt < :endedBefore")
    List<Long> findSessionIdsEndedBefore(@Param("endedBefore") LocalDateTime endedBefore);

    /** [5단계] 방 메시지 일괄 삭제 (Q11-1) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CommunityChatMessage m where m.eventSession.id in :sessionIds")
    int deleteAllBySessionIds(@Param("sessionIds") List<Long> sessionIds);
}
