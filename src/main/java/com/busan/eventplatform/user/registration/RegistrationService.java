package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.api.purchase.Purchase;
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
import java.time.format.DateTimeFormatter;
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
    // [나의 신청 내역] 결제일 표시 형식
    private static final DateTimeFormatter HISTORY_DATE = DateTimeFormatter.ofPattern("yyyy. MM. dd");

    /**
     * 신청 검증 결과 (prepareApply → 흐름 클래스 → completeApply 로 전달)
     *
     * @param session 비관적 락으로 조회한 회차 (트랜잭션 끝까지 락 유지)
     * @param user    검증을 통과한 신청자
     */
    public record ApplyContext(EventSession session, User user) {
    }

    /**
     * 환불 요청 — cancel()/reject() 가 만들어 흐름 클래스로 넘기면, 흐름 클래스가 PurchaseService.refund() 호출
     *
     * @param purchaseId   환불할 결제 (신청에 연결된 결제, 연결 전 기존 데이터면 null)
     * @param refundAmount 환불 규칙(RegistrationRefundPolicy)으로 계산한 금액 (0 가능)
     */
    public record RefundRequest(Long purchaseId, int refundAmount) {
    }


    // =========================================================
    // 참가 신청
    // =========================================================

    /**
     * [흐름 클래스 ① 단계] 신청 가능 여부 검증 — 돈이 움직이기 전에 확인
     * 여기서 예외가 나면 결제(②)가 실행되지 않아 포인트가 차감되지 않는다.
     */
    @Transactional
    public ApplyContext prepareApply(Long sessionId, LoginIdentity loginIdentity) {
        // 1. 개인 회원만 신청 가능
        if (!loginIdentity.isUser()) {
            throw new RegistrationForbiddenException("개인 회원만 행사에 참가 신청할 수 있습니다");
        }

        // 2. 회차 조회 + 비관적 락 (흐름 클래스 트랜잭션이 끝날 때까지 유지)
        EventSession session = eventSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new Exception404("신청하려는 행사 회차가 존재하지 않습니다"));

        // 3. 모집 가능 여부 (공개·모집 기간·마감·회차 상태·시작 전)
        validateApplicable(session);

        // 4. 계정 상태
        User user = userRepository.findById(loginIdentity.getId())
                .orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new Exception400("정지되었거나 탈퇴한 계정은 행사에 신청할 수 없습니다");
        }

        // 5. 중복 신청 — 결제보다 먼저 확인해야 신청 메시지가 나간다 (취소·반려 건은 제외 → 재신청 허용)
        if (registrationRepository.existsByUserAndSessionAndStatusIn(
                user.getId(), sessionId, RegistrationStatus.ACTIVE)) {
            throw new Exception400("이미 신청한 행사 회차입니다");
        }

        return new ApplyContext(session, user);
    }

    /**
     * [흐름 클래스 ③ 단계] 신청 저장 — 결제(②) 성공 후에만 호출
     *
     * @param purchase PurchaseService.pay() 가 만든 결제 내역 (신청에 purchase_id 로 연결)
     */
    @Transactional
    public RegistrationRes.ApplyDto completeApply(ApplyContext ctx, Purchase purchase) {
        EventSession session = ctx.session();
        User user = ctx.user();

        // 1. 잔여 정원으로 상태 결정 + 자리 점유 (결제 성공 후 점유 → 실패 시 되돌릴 자리 없음)
        RegistrationStatus status;
        if (session.hasRemainingSeat()) {
            session.occupySeat();
            status = seatStatusFor(session);
        } else {
            status = RegistrationStatus.WAITING;
        }

        // 2. 접수 번호 발급 + 신청 저장 (결제 연결)
        int receptionNumber = session.issueReceptionNumber();
        Registration registration = registrationRepository.save(
                Registration.create(user, session, purchase, status, receptionNumber));

        // 3. 이력 기록
        recordHistory(registration, status, user, "참가 신청");

        Integer waitingNumber = waitingNumberOf(registration);
        return RegistrationRes.ApplyDto.of(registration, waitingNumber);
    }

    /**
     * 참가 신청 도메인 (신청 규칙만 담당).
     * - 결제는 직접 호출하지 않는다. 신청 + 결제 순서는 RegistrationPurchaseService 가 조율한다.
     *   검증(prepareApply) → 결제(PurchaseService.pay) → 저장(completeApply), 하나의 트랜잭션
     * - 대기열 승격 시에는 추가 결제 없이 상태만 변경 (신청 시점에 이미 결제)
     * - 취소·반려 시 환불은 포인트 파트 refund() 완성 후 흐름 클래스에서 연결 (TODO)
     */

    // =========================================================
    // 유저 본인 취소
    // =========================================================

    /**
     * 본인 취소. 회차 시작 전이면 언제든지 취소 가능, 환불 금액은 날짜별 차등 (조장님 결정 D3·D5).
     * 포인트는 여기서 돌려주지 않는다 — 반환된 RefundRequest 로 흐름 클래스가 환불 처리 (진행 순서 8)
     *
     * @return 환불 요청 (어느 결제를 얼마 환불할지)
     */
    @Transactional
    public RefundRequest cancel(Long registrationId, LoginIdentity loginIdentity) {
        if (!loginIdentity.isUser()) {
            throw new RegistrationForbiddenException("개인 회원만 신청을 취소할 수 있습니다");
        }

        // 회차 락 → 신청 조회 순서 (동시 승격과 겹쳐도 최신 상태로 판단)
        Registration registration = findRegistrationWithSessionLock(registrationId);
        EventSession session = registration.getEventSession();

        if (!registration.isOwnedBy(loginIdentity.getId())) {
            throw new RegistrationForbiddenException("본인의 신청만 취소할 수 있습니다");
        }

        LocalDateTime now = LocalDateTime.now();
        String blockedReason = getCancelBlockedReason(registration, now);
        if (blockedReason != null) {
            throw new Exception400(blockedReason);
        }

        // 취소 전 상태 기억 — 좌석 반납 여부와 환불 규칙(대기 취소 전액)에 사용
        RegistrationStatus statusBefore = registration.getStatus();
        boolean releasedSeat = statusBefore.isSeatOccupying();

        registration.cancel();
        recordHistory(registration, RegistrationStatus.CANCELLED, registration.getUser(), "본인 취소");

        if (releasedSeat) {
            session.releaseSeat();
            promoteWaitingUsersIfAny(session);
        }

        // 환불 금액 계산 (실제 결제 금액 기준, 날짜 기준 차등)
        int paidAmount = paidAmountOf(registration);
        int refundAmount = RegistrationRefundPolicy.forUserCancel(
                paidAmount, statusBefore, now.toLocalDate(), session.getStartsAt().toLocalDate());
        return new RefundRequest(purchaseIdOf(registration), refundAmount);
    }


    // =========================================================
    // 주최자 반려
    // =========================================================

    /**
     * 주최 기업의 참가 신청 거절. 기업 거절은 전액 환불 (조장님 결정 D5).
     * 포인트는 여기서 돌려주지 않는다 — 반환된 RefundRequest 로 흐름 클래스가 환불 처리 (진행 순서 8)
     *
     * @return 환불 요청 (결제 금액 전액)
     */
    @Transactional
    public RefundRequest reject(Long registrationId, LoginIdentity loginIdentity) {
        Registration registration = findRegistrationWithSessionLock(registrationId);
        EventSession session = registration.getEventSession();

        validateOrganizer(session, loginIdentity);

        boolean releasedSeat = registration.getStatus().isSeatOccupying();
        registration.reject();
        // 주최자는 Company 라 processedByUser(User 타입)에 넣을 수 없음 → null + 사유에 기업 id 기록
        recordHistory(registration, RegistrationStatus.REJECTED, null,
                "주최자 반려 (companyId=" + loginIdentity.getId() + ")");

        if (releasedSeat) {
            session.releaseSeat();
            promoteWaitingUsersIfAny(session);
        }

        int refundAmount = RegistrationRefundPolicy.forCompanyReject(paidAmountOf(registration));
        return new RefundRequest(purchaseIdOf(registration), refundAmount);
    }

    // =========================================================
    // 대기열 승격 — 결제는 신청 시 이미 완료되었으므로 상태만 변경
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

    /**
     * 호출 전 반드시 회차 락을 잡고 있어야 한다
     */
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

    // [신청 전 확인] 신청 가능 여부와 포인트 충분 여부 — 조회 전용, 락 없음
    // (실제 신청 시 apply() 에서 락을 잡고 다시 검증한다)
    public RegistrationRes.CheckDto check(Long sessionId, LoginIdentity loginIdentity) {
        EventSession session = eventSessionRepository.findById(sessionId)
                .orElseThrow(() -> new Exception404("행사 회차가 존재하지 않습니다"));
        User user = userRepository.findById(loginIdentity.getId())
                .orElseThrow(() -> new Exception404("유저를 찾을 수 없습니다"));

        String blockedReason = getClosedReason(session);
        if (blockedReason == null && registrationRepository.existsByUserAndSessionAndStatusIn(
                user.getId(), sessionId, RegistrationStatus.ACTIVE)) {
            blockedReason = "이미 신청한 행사 회차입니다";
        }

        int remainingSeats = session.getCapacity() - session.getOccupiedCount();
        return RegistrationRes.CheckDto.of(sessionId, session.getFee(), user.getPoint(), remainingSeats, blockedReason);
    }

    // [마이페이지용] 유저 본인의 참가 신청 내역 최신순 조회
    public List<RegistrationRes.MyRegistrationDto> getMyRegistrations(LoginIdentity loginIdentity) {
        if (!loginIdentity.isUser()) {
            throw new RegistrationForbiddenException("개인 회원만 신청 내역을 조회할 수 있습니다");
        }
        LocalDateTime now = LocalDateTime.now(); // 취소 가능 여부 판단 기준 시각
        return registrationRepository.findAllByUserIdWithSession(loginIdentity.getId()).stream()
                .map(r -> RegistrationRes.MyRegistrationDto.of(r, waitingNumberOf(r), isCancelable(r, now)))
                .toList();
    }

    // [나의 신청 내역 /myhistory] 요약 카드·탭·목록 데이터 (머스태치는 조장님 담당, 여기서는 값만 계산)
    @Transactional(readOnly = true)
    public RegistrationRes.MyHistoryDto getMyHistory(LoginIdentity loginIdentity, String tab, String keyword) {
        if (!loginIdentity.isUser()) {
            throw new RegistrationForbiddenException("개인 회원만 신청 내역을 조회할 수 있습니다");
        }
        LocalDateTime now = LocalDateTime.now();
        String currentTab = (tab == null || tab.isBlank()) ? "all" : tab;
        String currentKeyword = keyword == null ? "" : keyword.trim();

        // 1. 내 신청 전체 → 카드 데이터 (요약 카드·탭 개수는 검색과 관계없이 전체 기준)
        List<RegistrationRes.MyHistoryItemDto> all = registrationRepository
                .findAllByUserIdForHistory(loginIdentity.getId()).stream()
                .map(r -> toHistoryItem(r, now))
                .toList();

        // 2. 탭 + 행사명 검색 적용
        List<RegistrationRes.MyHistoryItemDto> items = all.stream()
                .filter(i -> currentTab.equals("all") || currentTab.equals(i.getTab()))
                .filter(i -> currentKeyword.isEmpty() || i.getTitle().contains(currentKeyword))
                .toList();

        int upcoming = countTab(all, "upcoming");
        return RegistrationRes.MyHistoryDto.builder()
                .totalCount(all.size())
                .confirmedCount(upcoming)
                .waitingCount(countTab(all, "waiting"))
                .upcomingCount(upcoming)
                .pastCount(countTab(all, "past"))
                .currentTab(currentTab)
                .keyword(currentKeyword)
                .items(items)
                .empty(items.isEmpty())
                .build();
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

    /**
     * 회차가 지금 신청 불가능한 이유. 신청 가능하면 null.
     * [화면 제공] 행사 상세에서 버튼 상태 판단에 사용 (락 없이 조회한 회차로 호출 가능)
     */
    public String getClosedReason(EventSession session) {
        Event event = session.getEvent();
        LocalDateTime now = LocalDateTime.now();

        // 공개(승인 완료)된 행사만 신청 가능
        if (event.getStatus() != EventStatus.PUBLISHED) {
            return switch (event.getStatus()) {
                case CANCELLED -> "취소된 행사입니다";
                case COMPLETED -> "이미 종료된 행사입니다";
                default -> "아직 공개되지 않은 행사입니다"; // PENDING_REVIEW, FEEDBACK_REQUIRED
            };
        }
        if (event.getCancelledAt() != null) { // 상태값과 취소일이 어긋난 데이터 방어
            return "취소된 행사입니다";
        }
        // 행사 모집 기간 (Event.applyStartAt ~ applyEndAt)
        if (now.isBefore(event.getApplyStartAt())) {
            return "아직 모집 기간이 아닙니다";
        }
        if (event.isApplicationClosed()) {
            return "모집이 마감된 행사입니다";
        }
        // 회차 상태: 진행 예정(SCHEDULED) 회차만 신청 가능 (상태가 비어 있는 데이터는 허용)
        EventSessionStatus sessionStatus = session.getStatus();
        if (sessionStatus != null && sessionStatus != EventSessionStatus.SCHEDULED) {
            return switch (sessionStatus) {
                case IN_PROGRESS -> "이미 진행 중인 회차입니다";
                case COMPLETED -> "이미 종료된 회차입니다";
                case CANCELLED -> "취소된 회차입니다";
                default -> "신청할 수 없는 회차입니다";
            };
        }
        // 회차 시작 시각이 지났으면 신청 불가 (상태값이 아직 갱신되지 않은 경우 방어)
        if (!now.isBefore(session.getStartsAt())) {
            return "이미 시작된 회차입니다";
        }
        return null;
    }

    /**
     * 본인 취소가 불가능한 이유. 취소 가능하면 null.
     * 조장님 결정: 취소는 시작 전까지 언제든지 가능, 환불 금액만 차등.
     * [화면 제공] 구매내역의 취소 버튼 표시에도 같은 규칙을 사용한다.
     */
    public String getCancelBlockedReason(Registration registration, LocalDateTime now) {
        if (!registration.getStatus().isActive()) {
            return "이미 취소되었거나 반려된 신청입니다";
        }
        if (!now.isBefore(registration.getEventSession().getStartsAt())) {
            return "이미 시작된 회차는 취소할 수 없습니다";
        }
        return null;
    }

    public boolean isCancelable(Registration registration, LocalDateTime now) {
        return getCancelBlockedReason(registration, now) == null;
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
     * 회차 락을 먼저 잡고 신청을 조회한다 (락 이후 조회해야 승격 등 최신 상태가 반영됨)
     */
    private Registration findRegistrationWithSessionLock(Long registrationId) {
        Long sessionId = registrationRepository.findSessionIdById(registrationId)
                .orElseThrow(() -> new Exception404("신청 내역을 찾을 수 없습니다"));
        lockSession(sessionId);
        return findRegistration(registrationId);
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
     * 신청 시 검증 (apply 에서 락을 잡은 뒤 호출)
     */
    private void validateApplicable(EventSession session) {
        String closedReason = getClosedReason(session);
        if (closedReason != null) {
            throw new Exception400(closedReason);
        }
    }

    /**
     * 신청에 연결된 실제 결제 금액 (결제 연결 전 기존 데이터면 0)
     */
    private int paidAmountOf(Registration registration) {
        return registration.getPurchase() == null ? 0 : registration.getPurchase().getPrice();
    }

    /**
     * 신청에 연결된 결제 id (결제 연결 전 기존 데이터면 null → 환불 생략)
     */
    private Long purchaseIdOf(Registration registration) {
        return registration.getPurchase() == null ? null : registration.getPurchase().getId();
    }

    private Integer waitingNumberOf(Registration registration) {
        if (registration.getStatus() != RegistrationStatus.WAITING) {
            return null;
        }
        return (int) registrationRepository.countWaitingAhead(
                registration.getEventSession().getId(), registration.getReceptionNumber()) + 1;
    }

    // [나의 신청 내역] 신청 한 건 → 카드 (상태 라벨·상황별 문구·예상 환불액 계산)
    private RegistrationRes.MyHistoryItemDto toHistoryItem(Registration r, LocalDateTime now) {
        EventSession session = r.getEventSession();
        Event event = session.getEvent();
        RegistrationStatus status = r.getStatus();
        int paid = paidAmountOf(r);
        boolean cancelable = isCancelable(r, now);

        // 1. 지금 취소하면 받을 환불액 (취소 가능할 때만 계산, 규칙은 RegistrationRefundPolicy)
        int expectedRefund = cancelable
                ? RegistrationRefundPolicy.forUserCancel(paid, status, now.toLocalDate(), session.getStartsAt().toLocalDate())
                : 0;

        // 2. 표시 상태 결정 — 우선순위: 거절 > 취소 > 시작됨(행사 종료) > 대기 > 확정
        String tab, label, cssClass, message;
        Integer waitingNumber = null;
        if (status == RegistrationStatus.REJECTED) {
            tab = "past";
            label = "참가 거절";
            cssClass = "status-rejected";
            message = "주최자가 참가를 거절했습니다. 결제한 포인트는 전액 환불됩니다.";
        } else if (status == RegistrationStatus.CANCELLED) {
            tab = "past";
            label = "참가 취소";
            cssClass = "status-cancelled";
            message = "참가 신청을 취소했습니다.";
        } else if (!now.isBefore(session.getStartsAt())) {
            tab = "past";
            label = "행사 종료";
            cssClass = "status-ended";
            message = "회차가 시작되어 더 이상 취소할 수 없습니다.";
        } else if (status == RegistrationStatus.WAITING) {
            waitingNumber = waitingNumberOf(r);
            tab = "waiting";
            label = "대기열 등록";
            cssClass = "status-waiting";
            message = "대기 " + waitingNumber + "번입니다. 앞선 신청이 취소되면 자동으로 참가 확정됩니다."
                    + (paid > 0 ? " 대기 중에 취소하면 " + point(paid) + "가 전액 환불됩니다." : "");
        } else {
            tab = "upcoming";
            label = "참가 확정";
            cssClass = "status-confirmed";
            message = "참가가 확정되었습니다. " + refundGuide(paid, expectedRefund);
        }

        // 3. 취소 버튼 확인 창 문구 — 환불 예정 금액을 미리 보여줌
        String cancelConfirm;
        if (!cancelable) cancelConfirm = "";
        else if (paid == 0) cancelConfirm = "참가 신청을 취소하시겠습니까?";
        else if (expectedRefund > 0)
            cancelConfirm = "참가 신청을 취소하시겠습니까?\n결제한 " + point(paid) + " 중 " + point(expectedRefund) + "가 환불됩니다.";
        else cancelConfirm = "참가 신청을 취소하시겠습니까?\n회차 1일 전부터는 환불되지 않습니다. (환불 0P)";

        return RegistrationRes.MyHistoryItemDto.builder()
                .registrationId(r.getId())
                .eventId(event.getId())
                .category(event.getCategory().getName())
                .title(event.getTitle())
                .sessionName(session.getName())
                .place(event.getAddress())
                .scheduleText(RegistrationRes.formatSchedule(session.getStartsAt(), session.getEndsAt()))
                .tab(tab)
                .statusLabel(label)
                .statusClass(cssClass)
                .statusMessage(message)
                .waiting(waitingNumber != null)
                .waitingRank(waitingNumber == null ? 0 : waitingNumber)
                .paidPointText(point(paid))
                .purchasedAtText(r.getPurchase() == null || r.getPurchase().getPurchasedAt() == null
                        ? "" : r.getPurchase().getPurchasedAt().toLocalDateTime().format(HISTORY_DATE))
                .expectedRefundText(cancelable ? point(expectedRefund) : "")
                .refundText("") // TODO step 8: 포인트 파트 환불 필드(refundAmount) 연결 → "환불 완료 16,666P"
                .cancelable(cancelable)
                .cancelConfirmText(cancelConfirm)
                .cancelUrl(cancelable ? "/registrations/cancel/" + r.getId() : "")
                .detailUrl("/event/" + event.getId())
                .build();
    }

    // [나의 신청 내역] 확정 건의 "지금 취소하면" 환불 안내 문구
    private String refundGuide(int paid, int expectedRefund) {
        if (paid == 0) return "무료 회차라 취소해도 환불할 금액이 없습니다.";
        if (expectedRefund == paid) return "지금 취소하면 결제한 " + point(paid) + "가 전액 환불됩니다.";
        if (expectedRefund > 0) return "지금 취소하면 " + point(paid) + " 중 " + point(expectedRefund) + "만 환불됩니다.";
        return "회차 1일 전부터는 취소해도 환불되지 않습니다.";
    }

    // [나의 신청 내역] 탭별 개수
    private int countTab(List<RegistrationRes.MyHistoryItemDto> items, String tab) {
        return (int) items.stream().filter(i -> tab.equals(i.getTab())).count();
    }

    // [나의 신청 내역] 포인트 표시 "25,000P"
    private static String point(int amount) {
        return String.format("%,dP", amount);
    }

    private void validateOrganizer(EventSession session, LoginIdentity loginIdentity) {
        boolean isOrganizer = loginIdentity.isCompany()
                && session.getEvent().isOwner(loginIdentity.getId());
        if (!isOrganizer) {
            throw new RegistrationForbiddenException("행사 주최자만 처리할 수 있습니다");
        }
    }
}