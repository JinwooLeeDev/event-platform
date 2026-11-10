package com.busan.eventplatform.user;


import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.Exception400;
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

    // private final PointHistoryRepository pointHistoryRepository; // 추후 포인트 차감 내역 엔티티용 레포지토리

    /**
     * 행사 참가 신청 서비스
     * 1. 유료 행사 포인트 선차감 및 잔액 검증 로직
     * 2. 비관적 락을 통한 동시성 제어 및 정원/대기열(WAITING) 분기 처리
     */

    @Transactional
    public Registration applyForEvent(User user , EventSession session) {

        // 1. [동시성 제어] 비관적 락을 걸어 세션 조회 (다른 트랜잭션 접근 대기)
        // 비관적 락을 적용하여 세션 조회 및 정원 보호
        EventSession lockedSession = registrationRepository.findSessionWithPessimisticLock(session.getId());

        // 2. 유무료 여부 확인(fee가 0보다 크면 유료 행사로 간주)
        // todo - EventSession 기존 다이어그램의 participation_fee_point를 fee 로 필드명 맞춤 (윤아씨께 공유 완료)
        boolean isPaidEvent = lockedSession.getFee() != null && lockedSession.getFee() > 0;

        if(registrationRepository.existsBySessionIdAndUserId(lockedSession.getId() , user.getId())) {
            throw new Exception400("이미 이 행사에 등록되었습니다");
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

}
