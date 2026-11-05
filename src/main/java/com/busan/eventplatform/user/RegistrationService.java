package com.busan.eventplatform.user;


import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationRepository registrationRepository;

    // private final PointHistoryRepository pointHistoryRepository; // 추후 포인트 차감 내역 엔티티 용 레포지토리

    /**
     * 참가 신청 진입점 (포인트 선차감 로직 포함)
     * @Transactional 어노테이션을 통해 도중 에러(잔액 부족 등) 발생 시 모든 DB 작업이 롤백됨
     */

    @Transactional
    public Registration applyForEvent(User user, EventSession session) {

        // 1. 유무료 여부 확인(fee가 0보다 크면 유료 행사로 간주)
        // todo - EventSession 기존 다이어그램의 participation_fee_point를 fee 로 필드명 맞춤 (윤아씨께 공유 완료)
        boolean isPaidEvent = session.getFee() != null && session.getFee() > 0;

        RegistrationStatus initialStatus;

        if(isPaidEvent) {
            // 2. [유료] 사용자 지갑 잔액 확인
            if(user.getPoint() < session.getFee()) {
                // 잔액 부족 시 신청 중단 (Exception 발생 -> 트랜잭션 롤백)
                throw new Exception400("잔액이 부족합니다. (현재 잔액: " + user.getPoint() + ")");
            }

            // 3. [유료] 참가비만큼 선차감 처리
            user.deductPoint(session.getFee());

            // 4. 포인트 차감 기록 (트랜잭션에 묶임) todo - 추후 구현
            // pointHistoryRepository.save(new PointHistory(user, -session.getFee(), "행사 참가비 선차감"));

            // 5. 결제 완료 후 승인 대기 상태로 지정
            initialStatus = RegistrationStatus.PENDING_APPROVAL;

        } else {
            // [무료] 대기열 또는 확정 상태로 지정 (기획에 따라 다름)
            initialStatus = RegistrationStatus.WAITING;
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
