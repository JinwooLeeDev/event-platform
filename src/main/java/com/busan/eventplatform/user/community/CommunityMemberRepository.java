package com.busan.eventplatform.user.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 클래스의 역할
 * 멤버 조회(활성 여부, 내 커뮤니티 목록)
 */
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {
    /** 회원의 그 회차 멤버십 행 (활성·퇴장 상관없이) */
    Optional<CommunityMember> findByEventSessionIdAndUserId(Long eventSessionId, Long userId);

    /** 지금 그 회차 커뮤니티의 활성 멤버인지 — 3단계 STOMP 구독 권한 검사에 사용 */
    boolean existsByEventSessionIdAndUserIdAndLeftAtIsNull(Long eventSessionId, Long userId);

    /** 회원이 지금 들어가 있는 커뮤니티 목록 — 마이페이지 "내 커뮤니티" */
    List<CommunityMember> findAllByUserIdAndLeftAtIsNull(Long userId);

}
