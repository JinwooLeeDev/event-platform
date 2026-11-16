package com.busan.eventplatform.user.community;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * [f/40] 커뮤니티 멤버십 입장 및 퇴장
 /**
 * [f/40] 커뮤니티 — 멤버십 입장·퇴장(2단계) + 채팅방·메시지·내 커뮤니티(4단계)
 *
 * join/leave 가 REQUIRES_NEW 인 이유:
 * - 신청 트랜잭션이 '커밋된 뒤'(AFTER_COMMIT) 리스너에서 불리므로, 이미 끝난 트랜잭션에 합류하지 않고 새 트랜잭션으로 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private static final int RECENT_MESSAGE_LIMIT = 50;   // Q8: 입장 시 최근 50개
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy. MM. dd");

    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final EventSessionRepository eventSessionRepository;
    private final UserRepository userRepository;

    // =========================================================
    // 멤버십 (2단계)
    // =========================================================

    /** 입장 — 처음이면 행 생성, 퇴장했던 멤버면 다시 활성화, 이미 활성이면 그대로 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void join(Long sessionId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        communityMemberRepository.findByEventSessionIdAndUserId(sessionId, userId)
                .ifPresentOrElse(
                        member -> {
                            if (!member.isActive()) {
                                member.rejoin(now);
                            }
                        },
                        () -> communityMemberRepository.save(CommunityMember.join(
                                eventSessionRepository.getReferenceById(sessionId),
                                userRepository.getReferenceById(userId),
                                now)));
        log.info("[community] 입장 회차={} 회원={}", sessionId, userId);
    }

    /** 퇴장 — 멤버가 아니면(대기자 등) 아무것도 하지 않음 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void leave(Long sessionId, Long userId, CommunityLeaveReason reason) {
        communityMemberRepository.findByEventSessionIdAndUserId(sessionId, userId)
                .ifPresent(member -> member.leave(reason, LocalDateTime.now()));
        log.info("[community] 퇴장 회차={} 회원={} 사유={}", sessionId, userId, reason);
    }

    /** 지금 활성 멤버인지 — STOMP 구독·전송 권한 */
    public boolean isActiveMember(Long sessionId, Long userId) {
        return communityMemberRepository.existsByEventSessionIdAndUserIdAndLeftAtIsNull(sessionId, userId);
    }

    // =========================================================
    // 채팅방·메시지·내 커뮤니티 (4단계)
    // =========================================================

    /**
     * 채팅방 화면 데이터 — 진입 권한 검사 포함
     * - 회차 없음 404 / 커뮤니티 종료(회차 종료일 24시 지남) 400 / 활성 멤버 아님(대기·취소·거절, Q11) 403
     */
    public CommunityRes.RoomDto getRoom(Long sessionId, Long userId) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception404("커뮤니티를 찾을 수 없습니다"));
        if (!CommunityPolicy.isOpen(session.getEndsAt(), LocalDateTime.now())) {
            throw new Exception400("행사가 끝나 커뮤니티가 종료되었습니다");
        }
        if (!isActiveMember(sessionId, userId)) {
            throw new Exception403("참가 확정자만 커뮤니티에 입장할 수 있습니다");
        }

        List<CommunityChatMessage> recent = new ArrayList<>(
                communityChatMessageRepository.findRecent(sessionId, PageRequest.of(0, RECENT_MESSAGE_LIMIT)));
        Collections.reverse(recent); // 최신순으로 가져와서 화면에는 오래된 것부터

        List<CommunityRes.ChatItemDto> messages = recent.stream()
                .map(m -> new CommunityRes.ChatItemDto(
                        m.getUser().getDisplayName(),
                        m.getContent(),
                        CommunityMessage.format(m.getCreatedAt()),
                        m.getUser().getId().equals(userId)))
                .toList();

        return new CommunityRes.RoomDto(
                sessionId,
                session.getEvent().getTitle(),
                session.getName(),
                session.getEndsAt().toLocalDate().format(DATE) + " 24시에 커뮤니티가 종료됩니다",
                userId,
                messages,
                messages.isEmpty());
    }

    /**
     * 채팅 저장 — STOMP 전송(/app/community/{sessionId}/messages)에서 호출
     * 실패하면 IllegalArgumentException → 컨트롤러가 본인에게 ERROR 메시지로 안내
     */
    @Transactional
    public CommunityMessage saveChat(Long sessionId, Long userId, String content) {
        String text = content == null ? "" : content.strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("메시지를 입력해주세요");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("메시지는 " + MAX_CONTENT_LENGTH + "자까지 보낼 수 있습니다");
        }
        if (userId == null || !isActiveMember(sessionId, userId)) { // 인터셉터에서 이미 검사하지만 한 번 더
            throw new IllegalArgumentException("참가 확정자만 메시지를 보낼 수 있습니다");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        CommunityChatMessage saved = communityChatMessageRepository.save(
                CommunityChatMessage.create(eventSessionRepository.getReferenceById(sessionId), user, text));

        return CommunityMessage.chat(sessionId, userId, user.getDisplayName(), text, saved.getCreatedAt());
    }

    /** 내 커뮤니티 목록 (Q9: 마이페이지 "내 커뮤니티") — 활성 멤버 + 운영 중인 방만 */
    public List<CommunityRes.MyCommunityDto> getMyCommunities(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return communityMemberRepository.findAllByUserIdAndLeftAtIsNull(userId).stream()
                .map(CommunityMember::getEventSession)
                .filter(session -> CommunityPolicy.isOpen(session.getEndsAt(), now))
                .map(session -> new CommunityRes.MyCommunityDto(
                        session.getId(),
                        session.getEvent().getTitle(),
                        session.getName(),
                        session.getEndsAt().toLocalDate().format(DATE) + " 24시까지",
                        "/community/" + session.getId()))
                .toList();
    }

}
