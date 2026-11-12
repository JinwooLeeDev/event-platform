package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.api.purchase.PurchaseService;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.error.exception.RegistrationForbiddenException;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 참가 신청 도메인.
 * <p>
 * 결제 정책: "신청 시 결제"
 * - CONFIRMED / PENDING / WAITING 모두 신청 시점에 PurchaseService.purchase() 로 결제.
 * - 대기열 승격 시에는 추가 결제 없이 상태만 변경.
 * - 취소/반려 시 환불은 PurchaseService 의 환불 메서드(구현 중)에 위임.
 * <p>
 * 결제·환불 로직은 이 클래스에서 직접 처리하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final RegistrationHistoryRepository registrationHistoryRepository;
    private final EventSessionRepository eventSessionRepository;
    private final UserRepository userRepository;
    private final PurchaseService purchaseService;

    // =========================================================
    // 참가 신청
    // =========================================================

    @Transactional
    public RegistrationRes.ApplyDto apply(Long sessionId, LoginIdentity loginIdentity) {
        // 0. 개인 회원만 신청 가능
        if (!loginIdentity.isUser()) {
            throw new RegistrationForbiddenException("개인 회원만 행사에 참가 신청할 수 있습니다");
        }

        // 1. 회차 조회 + 비관적 락 (정원 확인 ~ 저장까지 동시 신청 직렬화)
        EventSession session = eventSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new Exception404("신청하려는 행사 회차가 존재하지 않습니다"));

        validateApplicable(session);

        User user = userRepository.findById(loginIdentity.getId())
                .orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new Exception400("정지되었거나 탈퇴한 계정은 행사에 신청할 수 없습니다");
        }

        // 2. 중복 신청 확인 — 결제보다 먼저 해야 신청 도메인 메시지가 나간다
        if (registrationRepository.existsByUserAndSessionAndStatusIn(
                user.getId(), sessionId, RegistrationStatus.ACTIVE)) {
            throw new Exception400("이미 신청한 행사 회차입니다");
        }

        // 3. 잔여 정원으로 상태 결정 + 자리 점유 (occupiedCount 증가)
        RegistrationStatus status;
        if (session.hasRemainingSeat()) {
            session.occupySeat();
            status = seatStatusFor(session);
        } else {
            status = RegistrationStatus.WAITING;
        }

        // 5. 접수 번호 발급 + 신청 저장
        int receptionNumber = session.issueReceptionNumber();
        Registration registration = registrationRepository.save(
                Registration.create(user, session, status, receptionNumber));

        // 6. 이력 기록
        recordHistory(registration, status, user, "참가 신청");

        Integer waitingNumber = waitingNumberOf(registration);
        return RegistrationRes.ApplyDto.of(registration, waitingNumber);
    }

    // =========================================================
    // 유저 본인 취소
    // =========================================================

    @Transactional
    public void cancel(Long registrationId, LoginIdentity loginIdentity) {
        if (!loginIdentity.isUser()) {
            throw new RegistrationForbiddenException("개인 회원만 신청을 취소할 수 있습니다");
        }

        Registration registration = findRegistration(registrationId);
        if (!registration.isOwnedBy(loginIdentity.getId())) {
            throw new RegistrationForbiddenException("본인의 신청만 취소할 수 있습니다");
        }

        // 승격 로직과 동시 신청이 겹치지 않도록 회차 락
        EventSession session = lockSession(registration.getEventSession().getId());

        boolean releasedSeat = registration.getStatus().isSeatOccupying();
        registration.cancel();
        recordHistory(registration, RegistrationStatus.CANCELLED, registration.getUser(), "본인 취소");

        // TODO: 포인트 파트 환불 메서드 완성 후 연결
        // purchaseService.refund(session.getId(), registration.getUser().getId());

        if (releasedSeat) {
            session.releaseSeat();
            promoteWaitingUsersIfAny(session);
        }
    }

    // =========================================================
    // 주최자 반려
    // =========================================================

    @Transactional
    public void reject(Long registrationId, LoginIdentity loginIdentity) {
        Registration registration = findRegistration(registrationId);
        EventSession session = lockSession(registration.getEventSession().getId());

        validateOrganizer(session, loginIdentity);

        boolean releasedSeat = registration.getStatus().isSeatOccupying();
        registration.reject();
        // 주최자는 Company 라 processedByUser(User 타입)에 넣을 수 없음 → null + 사유에 기업 id 기록
        recordHistory(registration, RegistrationStatus.REJECTED, null,
                "주최자 반려 (companyId=" + loginIdentity.getId() + ")");

        // TODO: 포인트 파트 환불 메서드 완성 후 연결 (주최자가 실행하므로 대상 userId 를 넘겨야 함)
        // purchaseService.refund(session.getId(), registration.getUser().getId());

        if (releasedSeat) {
            session.releaseSeat();
            promoteWaitingUsersIfAny(session);
        }
    }

    // =========================================================
    // 대기열 승격 — 결제는 신청 시 이미 완료되었으므로 상태만 변경
    // 호출 전 반드시 회차 락을 잡고 있어야 한다
    // =========================================================

    /**
     * [행사 파트 제공] 회차 정원이 늘어났을 때 대기자 승격.
     * 행사 파트에서 회차 수정(정원 증가) 후 호출한다.
     */
    @Transactional
    public void promoteWaitingUsers(Long sessionId) {
        EventSession session = lockSession(sessionId);
        promoteWaitingUsersIfAny(session);
    }


    private void promoteWaitingUsersIfAny(EventSession session) {
        while (session.hasRemainingSeat()) {
            Registration next = registrationRepository
                    .findFirstByEventSessionIdAndStatusOrderByReceptionNumberAsc(session.getId(), RegistrationStatus.WAITING)
                    .orElse(null);
            if (next == null) {
                return;
            }
            session.occupySeat();
            RegistrationStatus promotedStatus = seatStatusFor(session);
            next.promote(promotedStatus);
            recordHistory(next, promotedStatus, null, "선행 대기자 승격"); // 시스템 자동 처리
            registrationRepository.flush(); // 다음 findFirst 조회에 상태 변경이 반영되도록
        }
    }

    // =========================================================
    // 조회
    // =========================================================

    // [마이페이지용] 유저 본인의 참가 신청 내역 최신순 조회
    public List<RegistrationRes.MyRegistrationDto> getMyRegistrations(LoginIdentity loginIdentity) {
        if (!loginIdentity.isUser()) {
            throw new RegistrationForbiddenException("개인 회원만 신청 내역을 조회할 수 있습니다");
        }
        return registrationRepository.findAllByUserIdWithSession(loginIdentity.getId()).stream()
                .map(r -> RegistrationRes.MyRegistrationDto.of(r, waitingNumberOf(r)))
                .toList();
    }

    // [주최자용] 회차별 참가자 목록 조회
    public List<RegistrationRes.ParticipantDto> getParticipants(Long sessionId, LoginIdentity loginIdentity) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception404("행사 회차가 존재하지 않습니다"));
        validateOrganizer(session, loginIdentity);

        return registrationRepository.findAllBySessionIdWithUser(sessionId).stream()
                .map(RegistrationRes.ParticipantDto::of)
                .toList();
    }

    /**
     * [행사 파트 제공] 회차별 확정 인원
     */
    public long getConfirmedCountBySession(Long sessionId) {
        return registrationRepository.countConfirmedBySession(sessionId);
    }

    /**
     * [행사 파트 제공] 행사 전체 확정 인원 합계
     */
    public long getTotalConfirmedCountByEvent(Long eventId) {
        return registrationRepository.countConfirmedByEvent(eventId);
    }

    // =========================================================
    // 내부 헬퍼
    // =========================================================

    /**
     * 상태 변경 이력 저장.
     *
     * @param processedBy 처리한 개인 회원 (주최자·시스템 처리면 null)
     */
    private void recordHistory(Registration registration, RegistrationStatus changedStatus,
                               User processedBy, String reason) {
        registrationHistoryRepository.save(RegistrationHistory.builder()
                .registration(registration)
                .changedStatus(changedStatus)
                .processedByUser(processedBy)
                .reason(reason)
                .build());
    }

    private Registration findRegistration(Long registrationId) {
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> new Exception404("신청 내역을 찾을 수 없습니다"));
    }

    private EventSession lockSession(Long sessionId) {
        return eventSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new Exception404("행사 회차가 존재하지 않습니다"));
    }

    /**
     * 자리가 있을 때 들어갈 상태.
     * 현재 Event 에 승인제 여부 필드가 없으므로 항상 CONFIRMED.
     * 승인제가 도입되면 여기서 PENDING 을 분기하면 된다 (나머지 로직은 PENDING 도 정원 점유로 이미 처리함).
     */
    private RegistrationStatus seatStatusFor(EventSession session) {
        return RegistrationStatus.CONFIRMED;
    }

    /**
     * 행사·회차 단위 신청 가능 여부 검증
     */
    private void validateApplicable(EventSession session) {
        Event event = session.getEvent();
        LocalDateTime now = LocalDateTime.now();

        // 공개(승인 완료)된 행사만 신청 가능
        if (event.getStatus() != EventStatus.PUBLISHED) {
            String message = switch (event.getStatus()) {
                case CANCELLED -> "취소된 행사입니다";
                case COMPLETED -> "이미 종료된 행사입니다";
                default -> "아직 공개되지 않은 행사입니다"; // PENDING_REVIEW, FEEDBACK_REQUIRED
            };
            throw new Exception400(message);
        }
        if (event.getCancelledAt() != null) { // 상태값과 취소일이 어긋난 데이터 방어
            throw new Exception400("취소된 행사입니다");
        }
        // 행사 모집 기간 (Event.applyStartAt ~ applyEndAt)
        if (now.isBefore(event.getApplyStartAt())) {
            throw new Exception400("아직 모집 기간이 아닙니다");
        }
        if (event.isApplicationClosed()) {
            throw new Exception400("모집이 마감된 행사입니다");   // ← 추가
        }
        // 회차 상태: 진행 예정(SCHEDULED) 회차만 신청 가능 (상태가 비어 있는 데이터는 허용)
            EventSessionStatus sessionStatus = session.getStatus();
            if (sessionStatus != null && sessionStatus != EventSessionStatus.SCHEDULED) {
                String message = switch (sessionStatus) {
                    case IN_PROGRESS -> "이미 진행 중인 회차입니다";
                    case COMPLETED -> "이미 종료된 회차입니다";
                    case CANCELLED -> "취소된 회차입니다";
                    default -> "신청할 수 없는 회차입니다";
                };
                throw new Exception400(message);
            }
            // 회차 시작 시각이 지났으면 신청 불가 (상태값이 아직 갱신되지 않은 경우 방어)
            if (!now.isBefore(session.getStartsAt())) {
                throw new Exception400("이미 시작된 회차입니다");
            }
        }

        private Integer waitingNumberOf (Registration registration){
            if (registration.getStatus() != RegistrationStatus.WAITING) {
                return null;
            }
            return (int) registrationRepository.countWaitingAhead(
                    registration.getEventSession().getId(), registration.getReceptionNumber()) + 1;
        }

        private void validateOrganizer (EventSession session, LoginIdentity loginIdentity){
            boolean isOrganizer = loginIdentity.isCompany()
                    && session.getEvent().isOwner(loginIdentity.getId());
            if (!isOrganizer) {
                throw new RegistrationForbiddenException("행사 주최자만 처리할 수 있습니다");
            }
        }
    }