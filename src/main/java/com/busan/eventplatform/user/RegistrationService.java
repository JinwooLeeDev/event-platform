package com.busan.eventplatform.user;


import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.RequiredArgsConstructor;
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
    public Registration applyForEvent(User user, EventSession session) {

        // 1. [동시성 제어] 비관적 락을 걸어 세션 조회 (다른 트랜잭션 접근 대기)
        // 비관적 락을 적용하여 세션 조회 및 정원 보호
        EventSession lockedSession = registrationRepository.findSessionWithPessimisticLock(session.getId());

        // 2. 유무료 여부 확인(fee가 0보다 크면 유료 행사로 간주)
        // todo - EventSession 기존 다이어그램의 participation_fee_point를 fee 로 필드명 맞춤 (윤아씨께 공유 완료)
        boolean isPaidEvent = lockedSession.getFee() != null && lockedSession.getFee() > 0;

        if(isPaidEvent) {
            // 2-1. [유료] 사용자 지갑 잔액 확인 (잔액 부족 시 예외 발생 및 트랜잭션 롤백)
            if(user.getPoint() < lockedSession.getFee()) {
                // 잔액 부족 시 신청 중단 (Exception 발생 -> 트랜잭션 롤백)
                throw new Exception400("잔액이 부족합니다. (현재 잔액: " + user.getPoint() + ")");
            }

            // 2-2. [유료] 참가비만큼 선차감 처리
            user.deductPoint(lockedSession.getFee());

            // 2-3. 포인트 차감 기록 (트랜잭션에 묶임) todo - 추후 구현
            // pointHistoryRepository.save(new PointHistory(user, -session.getFee(), "행사 참가비 선차감"));

        }

        // 3. 현재 확정 및 승인 대기중인 인원수 계산
        List<RegistrationStatus> activeStatuses = List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.PENDING_APPROVAL);
        long currentOccupancy = registrationRepository.countBySessionIdAndStatusIn(lockedSession.getId(), activeStatuses);

        RegistrationStatus initialStatus;
        Integer queueNumber = null;

        // 4. 정원(Capacity) 및 대기자 분기 처리
        if(currentOccupancy < lockedSession.getCapacity()) {

        }

        // 6. 참가 신청 원장 생성
        Registration registration = Registration.builder()
                .user(user)
                .session(session)
                .status(initialStatus)
                .queueNumber(1) // 임시 대기열 번호
                .build();

        return registrationRepository.save(registration);

    }

}
