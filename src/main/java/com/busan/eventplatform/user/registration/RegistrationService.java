package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.event.eventCancel.EventCancelRepository;   // ★ [f/41] 회차 취소 검토 중 확인
import com.busan.eventplatform.event.eventCancel.EventCancelStatus;

import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.api.refund.RefundPolicy;
import com.busan.eventplatform.api.refund.RefundService;   // ★ 추가: 참가 취소 시 포인트 환불 (포인트 파트)
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
import com.busan.eventplatform.user.community.CommunityPolicy;   // ★ [f/40] 커뮤니티 운영 기간 규칙
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;   // ★ [f/40] 상태 변경 이벤트 발행
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 참가 신청 도메인.
 * <p>
 * 결제 정책: "신청 시 결제"
 * - CONFIRMED / PENDING / WAITING 모두 신청 시점에 결제 (RegistrationPurchaseService → PurchaseService.pay).
 * - 대기열 승격 시에는 추가 결제 없이 상태만 변경.
 * - 본인 취소 환불은 RefundService.refundUser() 에 위임, 기업 거절 환불은 RefundService.refundByCompany() 에 위임(f/41, B안)
 * - ★ [f/41] 회차 삭제 승인 시 참가자 환불은 RefundService.pointBackUser() (승인 메서드가 호출), 신청 정리는 cancelAllBySession()
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
    private final RefundService refundService;   // ★ 추가: 환불 금액 계산·포인트 반환은 포인트 파트가 담당

    private final EventCancelRepository eventCancelRepository;   // ★ [f/41] 회차 취소 검토 중 확인 (취소 신청 담당 제공)

    private final ApplicationEventPublisher eventPublisher;   // ★ [f/40] 상태 변경 알림 → 커뮤니티 입장·퇴장

    /**
     * 신청 검증 결과 (prepareApply → 흐름 클래스 → completeApply 로 전달)
     *
     * @param session 비관적 락으로 조회한 회차 (트랜잭션 끝까지 락 유지)
     * @param user    검증을 통과한 신청자
     */
    public record ApplyContext(EventSession session, User user) {
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
        User user = userRepository.LockFindById(loginIdentity.getId())
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
        publishStatusChanged(registration, null);  // ★ [f/40] 새 신청 (확정이면 커뮤니티 입장, 대기면 무시)


        Integer waitingNumber = waitingNumberOf(registration);
        return RegistrationRes.ApplyDto.of(registration, waitingNumber);
    }

    /**
     * 참가 신청 도메인 (신청 규칙만 담당).
     * - 결제는 직접 호출하지 않는다. 신청 + 결제 순서는 RegistrationPurchaseService 가 조율한다.
     *   검증(prepareApply) → 결제(PurchaseService.pay) → 저장(completeApply), 하나의 트랜잭션
     * - 대기열 승격 시에는 추가 결제 없이 상태만 변경 (신청 시점에 이미 결제)
     * - 본인 취소 환불: cancel() 에서 RefundService.refundUser() / 기업 거절 환불: reject()에서 RefundService.refundByCompany() (f/41)
     */

    // =========================================================
    // 유저 본인 취소
    // =========================================================

    /**
     * 본인 취소. 회차 시작 전이면 언제든지 취소 가능, 환불 금액은 날짜별 차등 (조장님 결정 D3·D5).
     * 상태를 CANCELLED 로 바꾼 뒤 RefundService.refundUser() 로 환불 (금액 계산은 포인트 파트 RefundPolicy).
     */
    @Transactional
    public void cancel(Long registrationId, LoginIdentity loginIdentity) {
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

        registration.cancel(); // 상태가 바뀜
        recordHistory(registration, RegistrationStatus.CANCELLED, registration.getUser(), "본인 취소");
        publishStatusChanged(registration, statusBefore); // ★ [f/40] 확정이었다면 커뮤니티 퇴장

        // ★ 추가: 포인트 환불
        //   - 상태를 CANCELLED로 바꾼 '후' 호출 (RefundService가 CANCELLED인지 검사함)
        //   - statusBefore가 WAITING이면 날짜와 상관없이 전액, CONFIRMED면 날짜별 정책 적용
        //   - 무료(0P)도 호출: 0P 환불 기록 + 결제 CANCELED 처리 → 같은 회차 재신청 가능
        //   - 결제 연결이 없는 예전 신청(purchase == null)은 환불할 결제가 없으므로 건너뜀
        //   - 같은 트랜잭션이라 환불에 실패하면 취소도 함께 되돌려짐
        if (registration.getPurchase() != null) {
            refundService.refundUser(registration.getUser().getId(), registration, statusBefore);
        }


        if (releasedSeat) {
            session.releaseSeat();
            promoteWaitingUsersIfAny(session);
        }

    }


    // =========================================================
    // 주최자 거절
    // =========================================================

    /**
     * 주최 기업의 참가 신청 거절. 기업 거절은 전액 환불 (조장님 결정 D5).
     * ★ [f/41] B안 — 신청 파트: 상태(REJECTED)·이력·이벤트(커뮤니티 퇴장) / 포인트 파트: 환불만 (refundByCompany)
     * refundByCompany 는 상태가 REJECTED 인지 검사하므로 registration.reject() '후'에 호출한다.
     * 같은 트랜잭션이라 환불이 실패하면 거절도 롤백된다 (커뮤니티 퇴장은 커밋 후에만 실행되므로 함께 취소됨).
     */
    @Transactional
    public void reject(Long registrationId, LoginIdentity loginIdentity) {
        Registration registration = findRegistrationWithSessionLock(registrationId);
        EventSession session = registration.getEventSession();

        validateOrganizer(session, loginIdentity);

        // ★ [f/41] 회차가 시작된 뒤에는 거절 불가 (D-C) — 본인 취소와 같은 기준(회차 시작 전까지)
        if (!LocalDateTime.now().isBefore(session.getStartsAt())) {
            throw new Exception400("이미 시작된 회차의 신청은 거절할 수 없습니다");
        }

        RegistrationStatus statusBefore = registration.getStatus(); // ★ [f/40] 거절 전 상태 기억
        boolean releasedSeat = statusBefore.isSeatOccupying();
        registration.reject();
        // 주최자는 Company 라 processedByUser(User 타입)에 넣을 수 없음 → null + 사유에 기업 id 기록
        recordHistory(registration, RegistrationStatus.REJECTED, null,
                "주최자 거절 (companyId=" + loginIdentity.getId() + ")");
        publishStatusChanged(registration, statusBefore);   // ★ [f/40] 확정이었다면 커뮤니티 퇴장

        if (releasedSeat) {
            session.releaseSeat();
            promoteWaitingUsersIfAny(session);
        }

        // ★ [f/41] 환불(전액)은 포인트 파트에 위임 — 결제 연결이 없는 신청은 건너뜀 (cancel() 과 같은 방식)
        if (registration.getPurchase() != null) {
            refundService.refundByCompany(registration);
        }
    }

    // =========================================================
    // ★ [f/41] 회차 삭제 승인 — 회차의 남은 신청 일괄 정리
    // =========================================================

    /**
     * ★ [f/41] 회차 삭제(취소) 승인 시 그 회차의 확정·대기 신청을 모두 정리한다.
     * 운영자 '삭제 승인' 메서드가 한 트랜잭션 안에서 아래 순서로 호출한다.
     * ① 회차 상태 CANCELLED
     * ② refundService.pointBackUser(session)   — 참가자 전원 전액 환불 (포인트 파트)
     * ③ registrationService.cancelAllBySession  — 이 메서드
     * ④ refundService.refundCompany(...)         — 기업 예약금 (포인트 파트, 삭제 신청일 기준)
     * <p>
     * ⚠ 반드시 pointBackUser '다음'에 호출한다.
     * pointBackUser 는 신청이 아직 확정·대기(isActive)일 때만 환불하므로,
     * 이 메서드를 먼저 부르면 예외 없이 아무도 환불받지 못한다. (다른 흐름과 순서가 반대인 이유)
     * <p>
     * - 상태: REJECTED (주최 측 사정) + 이력 "행사 회차 취소 (사유)"
     * - 환불: 하지 않음 (② 에서 이미 처리 → 이중 환불 방지)
     * - 좌석 반납·대기자 승격: 하지 않음 (회차 자체가 취소됨)
     * - 커뮤니티: 신청마다 이벤트를 보내지 않고 회차 단위 이벤트 1번 → 커밋 후 방 전체 종료·삭제
     * (신청마다 보내면 참가자마다 '주최자 거절' 퇴장 안내가 따로 나가고 메시지도 남는다)
     *
     * @param reason 운영자가 남긴 삭제 사유 (이력에 함께 기록, 없으면 생략)
     * @return 정리한 신청 수
     */
    @Transactional
    public int cancelAllBySession(Long sessionId, String reason) {
        EventSession session = lockSession(sessionId);
        if (session.getStatus() != EventSessionStatus.CANCELLED) {
            throw new Exception400("취소된 회차만 참가 신청을 일괄 정리할 수 있습니다");
        }

        String historyReason = "행사 회차 취소" + (reason == null || reason.isBlank() ? "" : " (" + reason.strip() + ")");
        List<Registration> actives = registrationRepository.findAllBySessionIdWithUser(sessionId).stream()
                .filter(r -> r.getStatus().isActive())
                .toList();

        for (Registration registration : actives) {
            registration.reject();
            recordHistory(registration, RegistrationStatus.REJECTED, null, historyReason);
        }

        eventPublisher.publishEvent(new RegistrationSessionCancelledEvent(sessionId));  // 커밋 후 커뮤니티 종료
        return actives.size();
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
            publishStatusChanged(next, RegistrationStatus.WAITING);       // ★ [f/40] 대기 → 확정 승격: 커뮤니티 입장
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
        // ★ [f/41] 취소·환불 탭(cancelled) 추가 — 그 외 값은 참가 확정 탭
        String currentTab = switch (tab == null ? "" : tab) {
            case "waiting", "past", "cancelled" -> tab;
            default -> "confirmed";
        };
        String currentKeyword = keyword == null ? "" : keyword.trim();

        // 1. 내 신청 전체 → 카드 데이터 (요약 카드·탭 개수는 검색과 관계없이 전체 기준)
        List<Registration> registrations = registrationRepository.findAllByUserIdForHistory(loginIdentity.getId());
        Map<Long, Integer> refundAmounts = refundAmountsOf(registrations); // ★ 추가: 결제 id → 실제 환불 금액
        List<RegistrationRes.MyHistoryItemDto> all = registrations.stream()
                // ★ [f/41] 취소·거절·취소된 회차도 '취소·환불' 탭에 표시 (환불 여부 확인용, 담당자 요청)
                //   승인 대기(PENDING)는 현재 쓰지 않는 상태라 제외
                .filter(r -> r.getStatus() != RegistrationStatus.PENDING)
                .map(r -> toHistoryItem(r, now, refundAmounts))            // ★ 변경: 환불 금액 전달
                .toList();

        // 2. 탭 + 행사명 검색 적용
        List<RegistrationRes.MyHistoryItemDto> items = all.stream()
                .filter(i -> currentTab.equals(i.getTab()))
                .filter(i -> currentKeyword.isEmpty() || i.getTitle().contains(currentKeyword))
                .toList();

        int upcoming = countTab(all, "confirmed");
        int cancelled = countTab(all, "cancelled");   // ★ [f/41]
        return RegistrationRes.MyHistoryDto.builder()
                .totalCount(all.size() - cancelled)    // ★ [f/41] 기존 의미 유지: 취소·거절 제외
                .confirmedCount(upcoming)
                .waitingCount(countTab(all, "waiting"))
                .upcomingCount(upcoming)
                .pastCount(countTab(all, "past"))
                .cancelledCount(cancelled)             // ★ [f/41]
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
        if (now.isBefore(session.getApplyStartAt())) {
            return "아직 모집 기간이 아닙니다";
        }
        if (session.isApplicationClosed()) {
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
        // ★ [f/41] 기업이 회차 취소를 신청해 운영자 검토 중이면 신규 신청 불가 (조장님 결정 D-R)
        if (isCancelPending(session.getId())) {
            return "회차 취소 검토 중이라 지금은 신청할 수 없습니다";
        }

        // 회차 시작 시각이 지났으면 신청 불가 (상태값이 아직 갱신되지 않은 경우 방어)
        if (!now.isBefore(session.getStartsAt())) {
            return "이미 시작된 회차입니다";
        }
        return null;
    }

    // [행사 상세 화면용] 행사의 회차별 신청 불가 사유 — key: 회차 id, value: 사유 (신청 가능하면 "")
    // 회차의 행사(LAZY) 정보를 읽으므로 트랜잭션 안에서 계산해 결과만 넘김 (컨트롤러에서 직접 부르면 LazyInitializationException)
    @Transactional(readOnly = true)
    public Map<Long, String> getClosedReasonsByEventId(Long eventId) {
        Map<Long, String> reasons = new HashMap<>();
        for (EventSession session : eventSessionRepository.findByEventId(eventId)) {
            String reason = getClosedReason(session);
            reasons.put(session.getId(), reason == null ? "" : reason);
        }
        return reasons;
    }


    /**
     * 본인 취소가 불가능한 이유. 취소 가능하면 null.
     * 조장님 결정: 취소는 시작 전까지 언제든지 가능, 환불 금액만 차등.
     * [화면 제공] 구매내역의 취소 버튼 표시에도 같은 규칙을 사용한다.
     */
    public String getCancelBlockedReason(Registration registration, LocalDateTime now) {
        if (!registration.getStatus().isActive()) {
            return "이미 취소되었거나 거절된 신청입니다";
        }
        // ★ [f/41] 회차 취소 검토 중이면 본인 취소 불가 (D-R) — 지금 취소하면 부분 환불, 승인되면 전액 환불
        if (isCancelPending(registration.getEventSession().getId())) {
            return "회차 취소 검토 중이라 지금은 취소할 수 없습니다. 승인되면 결제한 포인트가 전액 환불됩니다.";
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

    /**
     * ★ [f/40] 상태 변경 이벤트 발행 — 상태를 바꾸고 이력을 남긴 '직후'에 호출
     * 실제 처리(커뮤니티 입장·퇴장)는 트랜잭션 커밋 후 리스너가 한다.
     */
    private void publishStatusChanged(Registration registration, RegistrationStatus before) {
        eventPublisher.publishEvent(new RegistrationStatusChangedEvent(
                registration.getId(),
                registration.getUser().getId(),
                registration.getEventSession().getId(),
                before,
                registration.getStatus()));
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
     * ★ [f/41] 기업이 회차 취소를 신청해 운영자 검토(PENDING) 중인지
     */
    private boolean isCancelPending(Long sessionId) {
        return eventCancelRepository.existsBySessionIdAndStatus(sessionId, EventCancelStatus.PENDING);
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

    /**
     * ★ 추가: [나의 신청 내역] 신청 목록의 실제 환불 금액 — key: 결제 id, value: 환불 금액 (refund_tb)
     * 카드마다 조회하지 않고 목록 전체를 쿼리 1번으로 가져온다
     */
    private Map<Long, Integer> refundAmountsOf(List<Registration> registrations) {
        List<Long> purchaseIds = registrations.stream()
                .map(this::purchaseIdOf)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, Integer> amounts = new HashMap<>();
        if (purchaseIds.isEmpty()) {
            return amounts; // 결제 연결된 신청이 없으면 조회 생략 (빈 IN 절 방지)
        }
        for (Object[] row : registrationRepository.findRefundAmountsByPurchaseIds(purchaseIds)) {
            amounts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return amounts;
    }

    /**
     * ★ 추가: [나의 신청 내역] '환불 내역' 문구 — 환불 기록이 있을 때만 "환불 완료 7,000P", 없으면 null (제목 숨김)
     * 무료 취소는 0P 기록이 남으므로 "환불 완료 0P"로 표시된다
     */
    private String refundTextOf(Registration r, Map<Long, Integer> refundAmounts) {
        Long purchaseId = purchaseIdOf(r);
        Integer refunded = purchaseId == null ? null : refundAmounts.get(purchaseId);
        return refunded == null ? null : "환불 완료 " + point(refunded);
    }

    private Integer waitingNumberOf(Registration registration) {
        if (registration.getStatus() != RegistrationStatus.WAITING) {
            return null;
        }
        return (int) registrationRepository.countWaitingAhead(
                registration.getEventSession().getId(), registration.getReceptionNumber()) + 1;
    }

    // [나의 신청 내역] 신청 한 건 → 카드 (상태 라벨·상황별 문구·예상 환불액 계산)
    private RegistrationRes.MyHistoryItemDto toHistoryItem(Registration r, LocalDateTime now,
                                                           Map<Long, Integer> refundAmounts) { // ★ 추가: 실제 환불 금액
        EventSession session = r.getEventSession();
        Event event = session.getEvent();
        RegistrationStatus status = r.getStatus();
        int paid = paidAmountOf(r);
        boolean ended = session.getStatus() == EventSessionStatus.COMPLETED
                || event.getStatus() == EventStatus.COMPLETED
                || (session.getEndsAt() != null && !now.isBefore(session.getEndsAt()));
        // ★ [f/41] 회차·행사가 취소됐으면 신청 상태와 관계없이 '행사 취소' (취소 버튼·커뮤니티 버튼 숨김)
        boolean eventCancelled = session.getStatus() == EventSessionStatus.CANCELLED
                || event.getStatus() == EventStatus.CANCELLED;
        boolean cancelable = !ended && !eventCancelled && isCancelable(r, now);

        //  1. 지금 취소하면 받을 환불액 (취소 가능할 때만 계산, 규칙은 RefundPolicy)
        int expectedRefund = cancelable
                ? RefundPolicy.forUserCancel(paid, status, now.toLocalDate(), session.getStartsAt().toLocalDate())
                : 0;

        // 2. 표시 상태 결정 — 종료 시각·종료 상태를 기준으로 분류 (진행 중은 확정 탭에 유지)
        String tab, label, cssClass, message;
        Integer waitingNumber = null;
        if (status == RegistrationStatus.CANCELLED) {
            // 본인이 먼저 취소한 건은 나중에 회차가 취소돼도 '참가 취소'로 표시
            tab = "cancelled";   // ★ [f/41] past → cancelled (취소·환불 탭)
            label = "참가 취소";
            cssClass = "status-cancelled";
            message = "참가 신청을 취소했습니다. 환불 금액은 결제·환불 정보에서 확인할 수 있습니다.";
        } else if (eventCancelled) {
            // ★ [f/41] 회차 삭제 승인·행사 취소로 정리된 신청 — '거절'로 오해하지 않게 따로 표시
            tab = "cancelled";
            label = "행사 취소";
            cssClass = "status-rejected";
            message = "주최 측 사정으로 행사 회차가 취소되었습니다. 결제한 포인트는 전액 환불됩니다.";
        } else if (status == RegistrationStatus.REJECTED) {
            tab = "cancelled";   // ★ [f/41] past → cancelled
            label = "참가 거절";
            cssClass = "status-rejected";
            message = "주최자가 참가를 거절했습니다. 결제한 포인트는 전액 환불됩니다.";
        } else if (ended) {
            tab = "past";
            label = "행사 종료";
            cssClass = "status-ended";
            message = "종료된 회차입니다. 참가한 행사 정보를 확인할 수 있습니다.";
        } else if (status == RegistrationStatus.WAITING) {
            waitingNumber = waitingNumberOf(r);
            tab = "waiting";
            label = "대기열 등록";
            cssClass = "status-waiting";
            message = "대기 " + waitingNumber + "번입니다. 앞선 신청이 취소되면 자동으로 참가 확정됩니다."
                    + (paid > 0 ? " 대기 중에 취소하면 " + point(paid) + "가 전액 환불됩니다." : "");
        } else {
            tab = "confirmed";
            label = "참가 확정";
            cssClass = "status-confirmed";
            message = !now.isBefore(session.getStartsAt())
                    ? "현재 진행 중인 회차입니다."
                    : "참가가 확정되었습니다. " + refundGuide(paid, expectedRefund);
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
                .purchasedAtText(blankToNull(r.getPurchase() == null || r.getPurchase().getPurchasedAt() == null
                        ? "" : r.getPurchase().getPurchasedAt().toLocalDateTime().format(HISTORY_DATE))) // ★ 빈 값 → null (제목만 나오는 것 방지)
                .expectedRefundText(blankToNull(cancelable ? point(expectedRefund) : "")) // ★ 취소 불가 건은 '취소 시 예상 환불' 숨김
                .refundText(refundTextOf(r, refundAmounts)) // ★ 실제 환불 기록(refund_tb)이 있으면 "환불 완료 7,000P", 없으면 null
                .cancelable(cancelable)
                .cancelConfirmText(cancelConfirm)
                .refundPolicyText(cancelable && paid > 0 ? RefundPolicy.REGISTRATION_TEXT : "") // 무료·취소 불가면 정책 버튼 숨김
                .cancelUrl(cancelable ? "/registrations/cancel/" + r.getId() : "")
                .detailUrl("/event/" + event.getId()) // 행사 상세 주소 (행사 파트 확정: /event/{id})
                // ★ [f/40] 참가 확정 + 커뮤니티 운영 중(회차 종료일 24시 전)일 때만 입장 주소, 아니면 null → 버튼 숨김
                .communityUrl(status == RegistrationStatus.CONFIRMED
                        && !eventCancelled                                                      // ★ [f/41] 취소된 회차·행사는 입장 버튼 숨김
                        && CommunityPolicy.isOpen(session.getEndsAt(), CommunityPolicy.now())  // ★ 한국 시각 기준
                        ? "/community/" + session.getId() : null)
                .closed("cancelled".equals(tab))   // ★ [f/41] 취소·환불 카드 → '취소 불가' 버튼 숨김
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

    // ★ 추가: 빈 문자열이면 null로 변환
    //   Mustache는 ""도 '값 있음'으로 보고 {{#refundText}} 블록을 그려서
    //   내용 없이 "환불 내역" 제목만 나오던 문제를 막는다
    private static String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text;
    }
}
