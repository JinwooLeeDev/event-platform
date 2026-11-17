package com.busan.eventplatform.user.community;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * [f/40] 커뮤니티 — 멤버십 입장·퇴장(2단계) + 채팅방·메시지·내 커뮤니티(4단계) + 종료(5단계) + 종료일 안내(5-2)
 *       + 주최 기업 참여·상단 고정 공지·참여자 목록(6단계)
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
    private final CommunityNoticeRepository communityNoticeRepository; // ★ [6단계] 공지
    private final CompanyRepository companyRepository;                 // ★ [6단계] 주최 기업 채팅·공지

    // =========================================================
    // 멤버십 (2단계)
    // =========================================================

    /**
     * 입장 — 처음이면 행 생성, 퇴장했던 멤버면 다시 활성화, 이미 활성이면 그대로
     * ★ [5단계] 이미 종료된 회차면 입장시키지 않음 (서버 시작 동기화가 지난 회차 확정자를 다시 넣지 않게)
     * ★ [6단계] 실제로 입장 처리됐으면 true (새로 입장·재입장) — 리스너가 입장 기록·알림 여부를 판단
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean join(Long sessionId, Long userId) {
        LocalDateTime now = CommunityPolicy.now();
        EventSession session = eventSessionRepository.findById(sessionId).orElse(null);
        if (session == null || !CommunityPolicy.isOpen(session.getEndsAt(), now)) {
            log.debug("[community] 종료된 회차라 입장 생략 회차={} 회원={}", sessionId, userId);
            return false;
        }
        CommunityMember member = communityMemberRepository.findByEventSessionIdAndUserId(sessionId, userId).orElse(null);
        if (member == null) {
            communityMemberRepository.save(CommunityMember.join(session, userRepository.getReferenceById(userId), now));
        } else if (!member.isActive()) {
            member.rejoin(now);
        } else {
            return false;                                   // 이미 활성 멤버 — 변화 없음
        }
        log.info("[community] 입장 회차={} 회원={}", sessionId, userId);
        return true;
    }

    /**
     * 퇴장 — 멤버가 아니면(대기자 등) 아무것도 하지 않음
     * ★ [6단계] 실제로 퇴장 처리됐으면 true (활성 멤버였음)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean leave(Long sessionId, Long userId, CommunityLeaveReason reason) {
        CommunityMember member = communityMemberRepository.findByEventSessionIdAndUserId(sessionId, userId).orElse(null);
        if (member == null || !member.isActive()) {
            return false;
        }
        member.leave(reason, CommunityPolicy.now());
        log.info("[community] 퇴장 회차={} 회원={} 사유={}", sessionId, userId, reason);
        return true;
    }

    /**
     * ★ [6단계] 입장·퇴장 기록 저장 — 채팅 사이에 남겨서 새로고침·늦게 들어온 사람도 볼 수 있게
     * - 리스너(신청 상태 변경)에서만 호출 → 서버 시작 동기화(Initializer)는 기록을 남기지 않음
     * - 문구는 실시간 알림(CommunityNotifier)과 같게
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMemberEvent(Long sessionId, Long userId, CommunityChatMessage.Kind kind) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        String content = user.getDisplayName() + (kind == CommunityChatMessage.Kind.JOIN ? "님이 입장했습니다." : "님이 퇴장했습니다.");
        communityChatMessageRepository.save(CommunityChatMessage.memberEvent(
                eventSessionRepository.getReferenceById(sessionId), user, kind, content));
    }

    /** 지금 활성 멤버인지 (종료 여부는 보지 않음 — 종료까지 보려면 canAccess) */
    public boolean isActiveMember(Long sessionId, Long userId) {
        return communityMemberRepository.existsByEventSessionIdAndUserIdAndLeftAtIsNull(sessionId, userId);
    }

    /**
     * 지금 이 방을 이용할 수 있는지 — STOMP 구독·전송 권한 (CommunityChannelInterceptor)
     * - 회원: 활성 멤버(참가 확정자) + 운영 중                 [5단계]
     * - ★ 기업: 이 회차 행사를 주최한 기업 + 운영 중          [6단계, Q14 — 다른 기업은 불가]
     * 스케줄러가 돌기 전이라도 24시가 지나면 바로 막힌다 (이중 차단, B-2)
     */
    public boolean canAccess(Long sessionId, CommunityViewer viewer) {
        if (viewer == null || viewer.id() == null) {
            return false;
        }
        return (viewer.isUser()
                ? communityMemberRepository.findActiveSessionEndsAt(sessionId, viewer.id())
                : communityMemberRepository.findOwnedSessionEndsAt(sessionId, viewer.id()))
                .map(endsAt -> CommunityPolicy.isOpen(endsAt, CommunityPolicy.now()))
                .orElse(false);
    }

    // =========================================================
    // 채팅방·메시지·내 커뮤니티 (4단계) + 주최 기업·공지·참여자 (6단계)
    // =========================================================

    /**
     * 채팅방 화면 데이터 — 진입 권한 검사 포함
     * - 회차 없음 404 / 커뮤니티 종료(회차 종료일 24시 지남) 400
     * - 회원: 활성 멤버 아님(대기·취소·거절, Q11) 403 / ★ 기업: 주최 기업 아님 403
     */
    public CommunityRes.RoomDto getRoom(Long sessionId, CommunityViewer viewer) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception404("커뮤니티를 찾을 수 없습니다"));
        LocalDateTime now = CommunityPolicy.now();
        if (!CommunityPolicy.isOpen(session.getEndsAt(), now)) {
            throw new Exception400("행사가 끝나 커뮤니티가 종료되었습니다");
        }
        if (!canAccess(sessionId, viewer)) {
            throw new Exception403(viewer.isCompany()
                    ? "행사를 주최한 기업만 커뮤니티에 입장할 수 있습니다"
                    : "참가 확정자만 커뮤니티에 입장할 수 있습니다");
        }

        List<CommunityChatMessage> recent = new ArrayList<>(
                communityChatMessageRepository.findRecent(sessionId, PageRequest.of(0, RECENT_MESSAGE_LIMIT)));
        Collections.reverse(recent); // 최신순으로 가져와서 화면에는 오래된 것부터

        List<CommunityRes.ChatItemDto> messages = recent.stream()
                .map(m -> new CommunityRes.ChatItemDto(
                        m.isSystem() ? null : m.getWriterName(),
                        m.getContent(),
                        CommunityMessage.format(m.getCreatedAt()),
                        m.isWrittenBy(viewer),
                        m.isByCompany(),
                        m.isSystem()))                          // ★ [6단계] 입장·퇴장 기록
                .toList();

        // ★ [6단계] 참여자 목록 (Q16·Q18: 참가자·주최 기업 모두에게 보임)
        List<CommunityRes.ParticipantDto> participants = communityMemberRepository.findActiveWithUser(sessionId).stream()
                .map(m -> new CommunityRes.ParticipantDto(m.getUser().getId(), m.getUser().getDisplayName()))
                .toList();

        // ★ [6단계] 상단 고정 공지 (Q15)
        CommunityNotice notice = communityNoticeRepository.findByEventSessionId(sessionId).orElse(null);

        return CommunityRes.RoomDto.builder()
                .sessionId(sessionId)
                .eventTitle(session.getEvent().getTitle())
                .sessionName(session.getName())
                .closeText(session.getEndsAt().toLocalDate().format(DATE) + " 24시에 커뮤니티가 종료됩니다")
                .endDayNotice(CommunityPolicy.showsEndDayNotice(session.getEndsAt(), now) ? CommunityPolicy.END_DAY_NOTICE : null)
                .myKey(viewer.key())
                .host(viewer.isCompany())
                .hostName(session.getEvent().getCompany() == null ? "주최 기업" : session.getEvent().getCompany().getName()) // 시드에 기업 없는 행사 방어
                .notice(notice == null ? null : notice.getContent())
                .noticeUpdatedAt(notice == null ? null : CommunityMessage.format(notice.getUpdatedAt()))
                .participants(participants)
                .participantCount(participants.size())
                .messages(messages)
                .empty(messages.isEmpty())
                .build();
    }

    /**
     * 채팅 저장 — STOMP 전송(/app/community/{sessionId}/messages)에서 호출
     * ★ [6단계] 주최 기업도 채팅 가능 — "기업명 (주최)"로 표시
     * 실패하면 IllegalArgumentException → 컨트롤러가 본인에게 ERROR 메시지로 안내
     */
    @Transactional
    public CommunityMessage saveChat(Long sessionId, CommunityViewer viewer, String content) {
        String text = validText(content, "메시지를 입력해주세요");
        if (!canAccess(sessionId, viewer)) { // 인터셉터에서 이미 검사하지만 한 번 더
            throw new IllegalArgumentException("참가 확정자·주최 기업만, 커뮤니티 운영 기간에만 메시지를 보낼 수 있습니다");
        }
        EventSession session = eventSessionRepository.getReferenceById(sessionId);

        if (viewer.isCompany()) {
            Company company = companyRepository.findById(viewer.id())
                    .orElseThrow(() -> new IllegalArgumentException("기업을 찾을 수 없습니다"));
            CommunityChatMessage saved = communityChatMessageRepository.save(
                    CommunityChatMessage.createByCompany(session, company, text));
            return CommunityMessage.chat(sessionId, viewer, saved.getWriterName(), text, saved.getCreatedAt());
        }

        User user = userRepository.findById(viewer.id())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));
        CommunityChatMessage saved = communityChatMessageRepository.save(
                CommunityChatMessage.create(session, user, text));
        return CommunityMessage.chat(sessionId, viewer, user.getDisplayName(), text, saved.getCreatedAt());
    }

    /**
     * ★ [6단계] 상단 고정 공지 등록·수정 (Q15) — STOMP 전송(/app/community/{sessionId}/notice)에서 호출
     * - 주최 기업만 / 방당 1개 — 새 공지가 이전 공지를 대체
     * - 빈 문구로 보내면 공지 내림 (삭제 후 빈 NOTICE 전송 → 화면에서 공지 칸 숨김)
     */
    @Transactional
    public CommunityMessage saveNotice(Long sessionId, CommunityViewer viewer, String content) {
        if (viewer == null || !viewer.isCompany() || !canAccess(sessionId, viewer)) {
            throw new IllegalArgumentException("행사를 주최한 기업만 공지를 등록할 수 있습니다");
        }
        String text = content == null ? "" : content.strip();
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("공지는 " + MAX_CONTENT_LENGTH + "자까지 등록할 수 있습니다");
        }
        LocalDateTime now = CommunityPolicy.now();
        CommunityNotice notice = communityNoticeRepository.findByEventSessionId(sessionId).orElse(null);

        if (text.isEmpty()) {                                    // 공지 내리기
            if (notice != null) {
                communityNoticeRepository.delete(notice);
            }
            return CommunityMessage.notice(sessionId, "", now);
        }

        Company company = companyRepository.findById(viewer.id())
                .orElseThrow(() -> new IllegalArgumentException("기업을 찾을 수 없습니다"));
        if (notice == null) {
            communityNoticeRepository.save(CommunityNotice.create(
                    eventSessionRepository.getReferenceById(sessionId), company, text, now));
        } else {
            notice.edit(company, text, now);
        }
        log.info("[community] 공지 등록 회차={} 기업={}", sessionId, viewer.id());
        return CommunityMessage.notice(sessionId, text, now);
    }

    /**
     * 내 커뮤니티 목록 (Q9: 마이페이지 "내 커뮤니티") — 운영 중인 방만
     * - 회원: 활성 멤버인 방
     * - ★ 기업: 주최한 행사의 회차 (취소된 회차 제외)  [6단계]
     */
    public List<CommunityRes.MyCommunityDto> getMyCommunities(CommunityViewer viewer) {
        LocalDateTime now = CommunityPolicy.now();
        List<EventSession> sessions = viewer.isCompany()
                ? communityMemberRepository.findSessionsOwnedBy(viewer.id(), EventSessionStatus.CANCELLED)
                : communityMemberRepository.findAllByUserIdAndLeftAtIsNull(viewer.id()).stream()
                .map(CommunityMember::getEventSession)
                .toList();
        return sessions.stream()
                .filter(session -> CommunityPolicy.isOpen(session.getEndsAt(), now))
                .map(session -> new CommunityRes.MyCommunityDto(
                        session.getId(),
                        session.getEvent().getTitle(),
                        session.getName(),
                        session.getEndsAt().toLocalDate().format(DATE) + " 24시까지",
                        "/community/" + session.getId()))
                .toList();
    }

    /** 앞뒤 공백 제거 + 빈 문구·500자 초과 거부 */
    private String validText(String content, String emptyMessage) {
        String text = content == null ? "" : content.strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("메시지는 " + MAX_CONTENT_LENGTH + "자까지 보낼 수 있습니다");
        }
        return text;
    }

    // =========================================================
    // 종료일 당일 안내 (5-2단계)
    // =========================================================

    /** ★ 오늘이 종료일인 운영 중 방 — 11:00 스케줄러가 안내를 보낼 대상 */
    public List<Long> findRoomsEndingToday(LocalDateTime now) {
        LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
        return communityMemberRepository.findOpenSessionIdsEndingBetween(dayStart, dayStart.plusDays(1));
    }

    // =========================================================
    // 커뮤니티 종료 (5단계, Q4·Q10·Q11)
    // =========================================================

    /**
     * ★ 종료 시각이 지난 방 정리 — 스케줄러(매일 00:00)·서버 시작 시 호출
     * 1. 활성 멤버 전원 비활성 (left_reason = CLOSED, 행은 기록으로 남김)
     * 2. 메시지·★공지 삭제 (Q11-1, CommunityPolicy.DELETE_MESSAGES_ON_CLOSE)
     * 시각을 인자로 받아 테스트에서 날짜를 바꿔 검증할 수 있게 함
     *
     * @return 이번에 닫은 방(활성 멤버가 있던 회차) id — 커밋 후 CLOSED 알림 대상
     */
    @Transactional
    public List<Long> closeExpired(LocalDateTime now) {
        LocalDateTime endedBefore = CommunityPolicy.closedIfEndedBefore(now);

        List<Long> roomsToClose = communityMemberRepository.findOpenSessionIdsEndedBefore(endedBefore);
        int closedMembers = roomsToClose.isEmpty() ? 0
                : communityMemberRepository.closeAll(roomsToClose, CommunityLeaveReason.CLOSED, now);

        int deletedMessages = 0;
        int deletedNotices = 0;
        if (CommunityPolicy.DELETE_MESSAGES_ON_CLOSE) {
            deletedNotices = communityNoticeRepository.deleteAllEndedBefore(endedBefore); // ★ [6단계] 공지도 함께 삭제
            // 멤버가 모두 나간 방에도 메시지가 남아 있을 수 있으므로 메시지 기준으로도 찾는다
            Set<Long> roomsWithMessages = new LinkedHashSet<>(roomsToClose);
            roomsWithMessages.addAll(communityChatMessageRepository.findSessionIdsEndedBefore(endedBefore));
            if (!roomsWithMessages.isEmpty()) {
                deletedMessages = communityChatMessageRepository.deleteAllBySessionIds(new ArrayList<>(roomsWithMessages));
            }
        }

        if (!roomsToClose.isEmpty() || deletedMessages > 0 || deletedNotices > 0) {
            log.info("[community] 종료 정리 기준={} 방={} 멤버 비활성={}명 메시지 삭제={}건 공지 삭제={}건",
                    endedBefore, roomsToClose, closedMembers, deletedMessages, deletedNotices);
        }
        return roomsToClose;
    }
}
