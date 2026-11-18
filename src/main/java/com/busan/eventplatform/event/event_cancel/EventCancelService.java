package com.busan.eventplatform.event.event_cancel;

import com.busan.eventplatform.api.refund.RefundService;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCancelService {

    private final EventCancelRepository eventCancelRepository;
    private final EventSessionRepository eventSessionRepository;
    private final RefundService refundService;

    // 회차 취소 신청
    @Transactional
    public void save (Long companyId, Long sessionId, String reason) {

        // 기업이 소유한 회차 조회
        EventSession eventSession = eventSessionRepository
                .findByCompanyIdAndSessionId(companyId, sessionId)
                .orElseThrow(() -> new Exception400("회차를 찾을 수 없습니다."));

        // 이미 검토 대기 중인 취소 신청이 있는지 확인
        boolean exists = eventCancelRepository.existsBySessionIdAndStatus(
                sessionId,
                EventCancelStatus.PENDING
        );

        if (exists) {
            throw new Exception400("이미 취소 신청이 접수된 회차입니다.");
        }

        // 취소 신청서 생성
        EventCancel eventCancel = EventCancel.builder()
                .eventSession(eventSession)
                .reason(reason)
                .status(EventCancelStatus.PENDING)
                .build();

        eventCancelRepository.save(eventCancel);
    }

    // 운영자 - 취소 신청 승인
    @Transactional
    public void approve(Long cancelId) {

        EventCancel eventCancel = eventCancelRepository.findById(cancelId)
                .orElseThrow(() -> new Exception400("취소 신청을 찾을 수 없습니다."));

        if (eventCancel.getStatus() != EventCancelStatus.PENDING) {
            throw new Exception400("이미 처리된 취소 신청입니다.");
        }

        // 취소 신청과 연결된 회차 조회
        EventSession eventSession = eventCancel.getEventSession();

        Company company = eventSession.getEvent().getCompany();
        eventSession.setStatus(EventSessionStatus.CANCELLED);

        // 참가자 전액 환불 -- 반드시 신청 취소 처리보다 먼저 실행해야 함
        refundService.pointBackUser(eventSession);

        // 참가 신청 일괄 취소
        // Todo - 메서드 구현 후 연결 예정

        // 기업 보증금 환불 -- 승인일이 아니라 취소 신청일 기준으로 환불
        refundService.refundCompany(
                company,
                eventSession.getId(),
                eventCancel.getCreatedAt().toLocalDate(),
                eventCancel.getReason()
        );
        eventCancel.approve();
    }

    // 운영자 - 취소 신청 거절
    @Transactional
    public void reject(Long cancelId) {

        EventCancel eventCancel = eventCancelRepository.findById(cancelId)
                .orElseThrow(() -> new Exception400("취소 신청을 찾을 수 없습니다."));

        if (eventCancel.getStatus() != EventCancelStatus.PENDING) {
            throw new Exception400("이미 처리된 취소 신청입니다.");
        }
        eventCancel.reject();
    }
}
