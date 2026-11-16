package com.busan.eventplatform.user.community;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityChatMessageRepository extends JpaRepository<CommunityChatMessage, Long> {

    /** 회차 방의 최근 메시지 (최신순) — 작성자 닉네임을 같이 가져와 N+1 방지 */
    @Query("select m from CommunityChatMessage m join fetch m.user " +
            "where m.eventSession.id = :sessionId order by m.id desc")
    List<CommunityChatMessage> findRecent(@Param("sessionId") Long sessionId, Pageable pageable);

}
