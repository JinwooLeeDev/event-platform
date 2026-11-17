package com.busan.eventplatform.user.community;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
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

    /** ★ [6단계] 방의 활성 멤버(참여자 목록) — 닉네임을 같이 가져와 N+1 방지, 먼저 들어온 순 */
    @Query("select m from CommunityMember m join fetch m.user " +
            "where m.eventSession.id = :sessionId and m.leftAt is null order by m.joinedAt")
    List<CommunityMember> findActiveWithUser(@Param("sessionId") Long sessionId);

    /**
     * ★ [6단계] 이 기업이 주최한 회차라면 그 회차의 종료 시각 (아니면 빈 값)
     * 주최 기업의 STOMP 구독·전송 권한 — '주최 여부 + 종료 시각'을 쿼리 1번으로 (EventSession.isOwner 와 같은 기준)
     * (행사 파트 Repository 를 수정하지 않기 위해 커뮤니티 Repository 에 둔다)
     */
    @Query("select s.endsAt from EventSession s where s.id = :sessionId and s.event.company.id = :companyId")
    Optional<LocalDateTime> findOwnedSessionEndsAt(@Param("sessionId") Long sessionId,
                                                   @Param("companyId") Long companyId);

    /** ★ [6단계] 기업용 "내 커뮤니티" — 이 기업이 주최한 회차 (취소된 회차 제외, 시작 순) */
    @Query("select s from EventSession s join fetch s.event e " +
            "where e.company.id = :companyId and s.status <> :excluded order by s.startsAt")
    List<EventSession> findSessionsOwnedBy(@Param("companyId") Long companyId,
                                           @Param("excluded") EventSessionStatus excluded);

    /** ★ [5단계] 방 전체 멤버 비활성 (CLOSED) — 행은 남김 (Q11) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommunityMember m set m.leftAt = :now, m.leftReason = :reason " +
            "where m.eventSession.id in :sessionIds and m.leftAt is null")
    int closeAll(@Param("sessionIds") List<Long> sessionIds,
                 @Param("reason") CommunityLeaveReason reason,
                 @Param("now") LocalDateTime now);
}
