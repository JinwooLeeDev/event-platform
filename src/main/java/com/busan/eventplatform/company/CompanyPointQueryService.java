package com.busan.eventplatform.company;

import com.busan.eventplatform.api.payment.PaymentRepository;
import com.busan.eventplatform.api.payment.PaymentStatus;
import com.busan.eventplatform.api.purchase.DepositRepository;
import com.busan.eventplatform.api.refund.RefundRepository;
import com.busan.eventplatform.global.error.exception.Exception404;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyPointQueryService {
    private final CompanyRepository companyRepository;
    private final PaymentRepository paymentRepository;
    private final DepositRepository depositRepository;
    private final RefundRepository refundRepository;

    public CompanyPointResponse.Detail findHistory(Long companyId) {
        // 1. 기업 존재 여부 확인 및 현재 잔액 조회
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));

        // 2. 충전 완료 내역을 조회하고 화면용 DTO로 변환
        List<CompanyPointResponse.Charge> charges =
                paymentRepository.findByCompanyIdWithPayment(
                                companyId,
                                PaymentStatus.PAID
                        )
                        .stream()
                        .map(CompanyPointResponse.Charge::new)
                        .toList();

        // 3. 보증금 납부·환불·정상 반환 상태 조회
        List<CompanyPointResponse.DepositHistory> deposits =
                depositRepository.findHistoryByCompanyId(companyId)
                        .stream()
                        .map(CompanyPointResponse.DepositHistory::new)
                        .toList();

        // 4. 취소 환급 대기·완료·거절 내역 조회
        List<CompanyPointResponse.RefundHistory> refunds =
                refundRepository.findHistoryByCompanyId(companyId)
                        .stream()
                        .map(CompanyPointResponse.RefundHistory::new)
                        .toList();

        // 5. 잔액을 표시 형식으로 변환
        String pointText = String.format(
                Locale.KOREA,
                "%,d",
                company.getPoint()
        );

        // 6. 페이지에 필요한 데이터를 하나로 묶어 반환
        return new CompanyPointResponse.Detail(
                pointText,
                charges,
                deposits,
                refunds
        );
    }
}
