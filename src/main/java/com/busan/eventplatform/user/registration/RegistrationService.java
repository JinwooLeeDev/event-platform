package com.busan.eventplatform.user.registration;


import com.busan.eventplatform.api.payment.Payment;
import com.busan.eventplatform.api.payment.PaymentRepository;
import com.busan.eventplatform.api.refund.Refund;
import com.busan.eventplatform.api.refund.RefundRepository;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.eventSession.EventSessionRepository;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationRepository registrationRepository;

    private final RefundRepository refundRepository; // 추후 포인트 차감 내역 엔티티용 레포지토리

    private final PaymentRepository paymentRepository;

    // DB에서 ID로 유저와 행사 세션을 찾아오기 위한 레포지토리 주입
    private final UserRepository userRepository;
    private final EventSessionRepository eventSessionRepository;

    /**
     * [컨트롤러와 연결되는 어댑터 메서드]
     * ID를 받아 엔티티를 조회한 후, 핵심 비즈니스 로직(applyForEvent)을 호출
     */
    @Transactional
    public RegistrationRes apply (Long eventId, Long userId) {
        // 1. 전달받은 ID로 DB엣서 엔티티 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception400("존재하지 않는 유저입니다."));

        EventSession session = eventSessionRepository.findById(eventId)
                .orElseThrow(() -> new Exception400("존재하지 않는 행사입니다."));

        // 2. 기존에 작성하신 핵심 로직 호출
        Registration registration = applyForEvent(user, session);

        // 3. 화면(프론트엔트)에 내려줄 응답 DTO로 변환하여 반환
        return RegistrationRes.builder()
                .isSuccess(true)
                .message("신청이 완료되었습니다.")
                .queueNumber(registration.getQueueNumber())
                .status(registration.getStatus().name()) // Enum 이름을 String 으로 변환
                .build();
    }

    /**
     * 행사 참가 신청 서비스
     * 1. 유료 행사 포인트 선차감 및 잔액 검증 로직
     * 2. 비관적 락을 통한 동시성 제어 및 정원/대기열(WAITING) 분기 처리
     */

    @Transactional
    public Registration applyForEvent(User user, EventSession session) {

        // 1. [동시성 제어] 비관적 락을 걸어 세션 조회 (다른 트랜잭션 접근 대기)
        // 비관적 락을 적용하여 세션 조회 및 정원 보호
        EventSession lockedSession = registrationRepository.findSessionWithPessimisticLock(session.getId());

        // 2. 유무료 여부 확인(fee가 0보다 크면 유료 행사로 간주)
        boolean isPaidEvent = lockedSession.getFee() != null && lockedSession.getFee() > 0;

        if (isPaidEvent) {
            // 2-1. [유료] 사용자 지갑 잔액 확인 (잔액 부족 시 예외 발생 및 트랜잭션 롤백)
            if (user.getPoint() < lockedSession.getFee()) {
                // 잔액 부족 시 신청 중단 (Exception 발생 -> 트랜잭션 롤백)
                throw new Exception400("잔액이 부족합니다. (현재 잔액: " + user.getPoint() + ")");
            }

            // 2-2. [유료] 참가비만큼 선차감 처리
            user.deductPoint(lockedSession.getFee());

            // 2-3. 포인트 차감 기록
            // 고유한 결제 번호 생성(예시: REG-유저PK-세션PK-현재시간)
            String uniquePaymentId = "REG-" + user.getId() + "-" + lockedSession.getId() + "-" + System.currentTimeMillis();

            Payment payment = Payment.builder()
                    .user(user)
                    .company(null) // 일반 유저의 참가비 결제 이므로 company는 null
                    .amount(lockedSession.getFee())
                    .paymentId(uniquePaymentId)
                    .build();
            // Builder 의 기본값이 READY(대기)이므로, 포인트가 이미 차감되었으니 PAID(완료)
            payment.confirmPay();
            paymentRepository.save(payment);
        }

        // 3. 선행 대기자 우선 승격 처리
        // 누군가 취소하여 빈자리가 발생했는데 대기자가 있다면, 현재 유저 등록 전 대기자를 먼저 승격시킴
        promoteWaitingUsersIfAny(lockedSession, isPaidEvent);


        // 4. 승격 로직 반영 후 잔여 정원 재계산
        List<RegistrationStatus> activeStatuses = List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.PENDING_APPROVAL);
        long currentOccupancy = registrationRepository.countBySessionIdAndStatusIn(lockedSession.getId(), activeStatuses);

        RegistrationStatus initialStatus; // 유저가 가장 처음 가지게 될 참가 신청 상태를 담아두기 위한 변수
        Integer queueNumber = null; // 대기번호

        // 5. 현재 유저 정원(Capacity) 및 대기자 분기 처리
        if (currentOccupancy < lockedSession.getCapacity()) {
            // [분기 1: 잔여 정원 있음 & 선행 대기자 없음이 보장됨]
            initialStatus = isPaidEvent ? RegistrationStatus.PENDING_APPROVAL : RegistrationStatus.CONFIRMED;
            queueNumber = null;
        } else {
            // [분기 2: 만석] 신규 유저는 대기열 마지막으로 등록
            initialStatus = RegistrationStatus.WAITING;
            Integer maxQueue = registrationRepository.findMaxQueueNumberBySessionId(lockedSession.getId(), RegistrationStatus.WAITING);
            queueNumber = (maxQueue == null) ? 1 : maxQueue + 1;
        }

        // 6. 참가 신청 원장 생성 및 저장
        // 원장(Ledger): 누가 어느 행사에 신청했는지에 대한 기록과 참가 신청자의 참가 가능 여부 파악을 위한 공식적인 신청 명부
        Registration registration = Registration.builder()
                .user(user)
                .session(lockedSession)
                .status(initialStatus)
                .queueNumber(queueNumber)
                .build();

        return registrationRepository.save(registration);

    }

    /**
     * [분기 3] 선행 대기자 승격 로직
     * 빈자리가 있을 경우, 비어있는 수만큼 대기번호가 가장 앞선 유저들을 확정/ 승인대기 상태로 승격하기
     */
    private void promoteWaitingUsersIfAny(EventSession session, boolean isPaidEvent) {
        List<RegistrationStatus> activeStatuses = List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.PENDING_APPROVAL);;
        long currentOccupancy = registrationRepository.countBySessionIdAndStatusIn(session.getId(), activeStatuses);
        long availableSpots = session.getCapacity() - currentOccupancy;

        if(availableSpots > 0){
            // 빈자리수(availableSpots)만큼만 대기열 최상위 유저를 조회 (JPA Pageable 활용)
            //PageRequest.of(int page, int size) 메서드는 규격상 int 타입만 받을 수 있기에 long -> int로 다운캐스팅
            Pageable limit = PageRequest.of(0, (int) availableSpots);
            List<Registration> waitingUsers = registrationRepository.findBySessionIdAndStatusOrderByQueueNumberAsc(
                    session.getId(), RegistrationStatus.WAITING, limit
            );

            RegistrationStatus targetStatus = isPaidEvent ? RegistrationStatus.PENDING_APPROVAL : RegistrationStatus.CONFIRMED;

            // 조회된 대기자들을 순횐하며 상태를 승격하고 대기 번호를 회수
            for (Registration waitingUser : waitingUsers) {
                waitingUser.promoteTo(targetStatus);
            }


        }
    }

    /**
     * [Branch 5] 참가 취소 및 자동 환불 로직
     * - 본인 및 기취소 여부 검증
     * - Registration 상태 CANCELED 변경
     * - 유료 행사일 경우 포인트 원복 및 Refund 엔티티 저장
     * - 취소로 인해 발생한 빈자리를 채우기 위해 대기열 승격 로직 호출
     */
    @Transactional
    public void cancelAndRefund(Long registrationId, Long userId) {
        // 1. 내역 조회 및 예외 처리
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new Exception400("존재하지 않는 신청 내역입니다."));

        // 2. 본인 요청 검증
        if(!registration.getUser().getId().equals(userId)) {
            throw new Exception400("본인의 신청 내역만 취소할 수 있습니다.");
        }

        // 3. 기취소 상태 방어
        if(registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new Exception400("이미 취소된 내역입니다.");
        }

        EventSession session = registration.getSession();
        boolean isPaidEvent = session.getFee() != null && session.getFee() > 0;

        // 4. 상태를 '취소'로 변경
        registration.promoteTo(RegistrationStatus.CANCELLED);

        // 5. 포인트 환불 (유료 행사의 경우)
        if(isPaidEvent) {
            User user = registration.getUser();

            // 유저 엔티티 환불 메서드 호출 (더티 체킹을 통한 자동 UPDATE)
            user.refundPoint(session.getFee());

            // 환불 내역(Refund) 객체 생성 및 저장
            Refund refund = Refund.builder()
                    .user(user)
                    .amount(session.getFee())
                    .reason("유저 참가 취소 환불 - 세션 ID: " + session.getId())
                    .build();

            refundRepository.save(refund);
        }

        // 6. 취소로 인해 잔여 정원이 생겼을 수 있으므로 대기자 승격 로직 즉시 실행
        // 취소된 세션에 비관적 락이 걸린 상태가 아니지만 트랜잭션 내에서 처리됨
        promoteWaitingUsersIfAny(session, isPaidEvent);
    }



}
