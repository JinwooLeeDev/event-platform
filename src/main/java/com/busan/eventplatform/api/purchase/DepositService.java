package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.global.error.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DepositService {
    private final EventSessionRepository eventSessionRepository;
    private final CompanyRepository companyRepository;
    private final DepositRepository depositRepository;

    @Transactional
    public void payDeposit(Long companyId , Long sessionId){
        // 1. 받은 eventId로 이벤트 조회.
        EventSession eventSession = eventSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new Exception404("해당 행사에 조회된 회차가 없습니다"));
        Event event = eventSession.getEvent();

        // 2. 조회된 회차가 현재 기업이 신청한 이벤트와 같은지.
        if(!eventSession.isOwner(companyId)) throw new Exception400("해당 이벤트 신청은 타기업 신청 이벤트입니다");

        // 3. 이벤트 현재 상태가 '결제대기' 이고 세션 상태가 "진행 예정"인지.
        if(!eventSession.forSessionDeposit()) throw new NotApproveException("이미 결제되었거나 아직 관리자에게 승인받지 못했습니다. 다시 확인해주세요");

        // 4. 통과되면 결제 진행.
        Company company = companyRepository.LockFindById(companyId).orElseThrow(() -> new Exception404("존재하지 않는 기업세션입니다"));

        // 기업 재심사·보완·정지 상태에서는 보증금 결제 및 행사 공개 차단
        if (company.getStatus() != CompanyStatus.APPROVED) {
            throw new Exception403(
                    "기업 승인 완료 후 보증금을 결제할 수 있습니다."
            );
        }

        Deposit deposit = Deposit.builder()
                .company(company)
                .eventSession(eventSession)
                .build();

        company.deductPoint(deposit.getAmount());

        // 5. '최종확정'으로 상태 변경 후 보증금 결제 DB 반영.
        deposit.setDepositPaid();

        // eventId에 해당하는 세션을 찾고 이 세션이 deposit에서 결제된 상태인지 아닌지 count. --> 이 count가 0이라면 결제되지 않은 세션이 없다는 뜻. -> 전부 결제됨.
        if(eventSessionRepository.countNotPaidSession(event.getId() , DepositStatus.PAID) == 0){
            event.setStatus(EventStatus.PUBLISHED); // 모든 세션이 결제됐다면 이벤트 상태 변경.
            event.setPublishedAt(LocalDateTime.now()); // 확정 시간 저장.
        }

        depositRepository.save(deposit);
    }
}
