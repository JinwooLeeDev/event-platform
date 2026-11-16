package com.busan.eventplatform.user.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    /** 회원의 그 회차 멤버십 행 (활성·퇴장 상관없이) */
    Optional<CommunityMember> findByEventSessionIdAndUserId(Long eventSessionId, Long userId);

    /** 지금 그 회차 커뮤니티의 활성 멤버인지 */
    boolean existsByEventSessionIdAndUserIdAndLeftAtIsNull(Long eventSessionId, Long userId);

    /** 회원이 지금 들어가 있는 커뮤니티 목록 — 마이페이지 "내 커뮤니티" (Q9) */
    List<CommunityMember> findAllByUserIdAndLeftAtIsNull(Long userId);

    /**
     * ★ [5단계] 활성 멤버라면 그 회차의 종료 시각 (멤버가 아니면 빈 값)
     * STOMP 구독·전송 때마다 불리므로 '멤버 여부 + 종료 시각'을 쿼리 1번으로 확인
     */
    @Query("select m.eventSession.endsAt from CommunityMember m " +
            "where m.eventSession.id = :sessionId and m.user.id = :userId and m.leftAt is null")
    Optional<LocalDateTime> findActiveSessionEndsAt(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    /** ★ [5단계] 종료 대상 — 활성 멤버가 남아 있고 회차 종료 시각이 기준 이전인 방 */
    @Query("select distinct m.eventSession.id from CommunityMember m " +
            "where m.leftAt is null and m.eventSession.endsAt < :endedBefore")
    List<Long> findOpenSessionIdsEndedBefore(@Param("endedBefore") LocalDateTime endedBefore);

    /** ★ [5-2] 오늘이 종료일인 방 — 활성 멤버가 있고 회차 종료 시각이 [오늘 00:00, 내일 00:00) */
    @Query("select distinct m.eventSession.id from CommunityMember m " +
            "where m.leftAt is null and m.eventSession.endsAt >= :dayStart and m.eventSession.endsAt < :nextDayStart")
    List<Long> findOpenSessionIdsEndingBetween(@Param("dayStart") LocalDateTime dayStart,
                                               @Param("nextDayStart") LocalDateTime nextDayStart);

    /** ★ [5단계] 방 전체 멤버 비활성 (CLOSED) — 행은 남김 (Q11) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommunityMember m set m.leftAt = :now, m.leftReason = :reason " +
            "where m.eventSession.id in :sessionIds and m.leftAt is null")
    int closeAll(@Param("sessionIds") List<Long> sessionIds,
                 @Param("reason") CommunityLeaveReason reason,
                 @Param("now") LocalDateTime now);
}