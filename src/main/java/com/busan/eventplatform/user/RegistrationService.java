package com.busan.eventplatform.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final RegistrationHistoryRepository historyRepository;
    private final PointService pointService;

    @Transactional
    public Registration applyForEvent(Long sessionId, User user, boolean isPaidEvent, int eventPrice, String policySnapshot) {

        RegistrationStatus initialStatus;

        if (isPaidEvent) {
            // 1. 유료 행사: 결제(포인트 차감) 시도. 잔액 부족 시 Exception 발생하여 롤백 및 신청 불가
            pointService.deductPoints(user.getId(), eventPrice);
            // 2. 결제 성공 시 상태를 심사 대기로 지정
            initialStatus = RegistrationStatus.PENDING_REVIEW;
        } else {
            // 3. 무료 행사: 결제 없이 즉시 확정
            initialStatus = RegistrationStatus.CONFIRMED;
        }

        // 4. 신청 데이터 생성 (임시 대기열 번호는 로직에 따라 할당)
        Registration registration = Registration.builder()
                .sessionId(sessionId)
                .user(user)
                .status(initialStatus)
                .queueNumber(1L) // 실제 큐 로직 적용 필요
                .policySnapshot(policySnapshot)
                .build();

        Registration savedRegistration = registrationRepository.save(registration);

        // 5. 이력 기록 생성
        RegistrationHistory history = new RegistrationHistory(
                savedRegistration,
                null,
                initialStatus,
                user.getId(),
                "NEW_APPLICATION"
        );
        historyRepository.save(history);

        return savedRegistration;
    }

    // 주최자 심사 승인/반려 로직
    @Transactional
    public void reviewRegistration(Long registrationId, boolean isApproved, Long actorId) {
        Registration registration = registrationRepository.findByIdWithPessimisticLock(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));

        if (registration.getStatus() != RegistrationStatus.PENDING_REVIEW) {
            throw new IllegalStateException("심사 대기 상태가 아닙니다.");
        }

        RegistrationStatus oldStatus = registration.getStatus();
        RegistrationStatus newStatus = isApproved ? RegistrationStatus.CONFIRMED : RegistrationStatus.CANCELLED;

        registration.updateStatus(newStatus, isApproved ? "APPROVED_BY_HOST" : "REJECTED_BY_HOST");

        // 반려 시 포인트 환불 로직 호출
        if (!isApproved) {
            // pointService.refundPoints(...)
        }

        historyRepository.save(new RegistrationHistory(registration, oldStatus, newStatus, actorId, "REVIEW_COMPLETED"));
    }

}
