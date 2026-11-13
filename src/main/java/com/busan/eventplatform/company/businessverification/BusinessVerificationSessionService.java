package com.busan.eventplatform.company.businessverification;

import com.busan.eventplatform.global.error.exception.Exception400;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BusinessVerificationSessionService {

    private static final String SESSION_KEY = "businessVerification";

    private final BusinessVerificationService businessVerificationService;

//    검증 버튼에서 호출함
    public BusinessVerificationResponse.Check check(
            BusinessVerificationRequest.Check check,
            HttpSession session
    ) {
        // 재검증에 실패해도 이전 성공 기록이 남지 않도록 먼저 제거
        session.removeAttribute(SESSION_KEY);

        check.validate();

        BusinessVerificationStatus status = businessVerificationService.verify(
                check.getBusinessNumber(),
                check.getRepresentativeName(),
                check.getOpenedOn()
        );

        if (status == BusinessVerificationStatus.MATCHED) {
            BusinessVerificationSession verificationSession =
                    new BusinessVerificationSession(
                            check.getBusinessNumber(),
                            check.getRepresentativeName(),
                            check.getOpenedOn(),
                            Instant.now()
                    );

            session.setAttribute(SESSION_KEY, verificationSession);

            return new BusinessVerificationResponse.Check(status, "사업자 정보가 확인되었습니다.");
        }

        if (status == BusinessVerificationStatus.MISMATCHED) {
            return new BusinessVerificationResponse.Check(status, "사업자 정보가 일치하지 않습니다. 입력 정보를 확인해주세요.");
        }

        return new BusinessVerificationResponse.Check(
                BusinessVerificationStatus.ERROR,
                "사업자 확인 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해주세요."
        );
    }

//    최종 회원가입에서 호출
    public void requireVerifiedBusiness (
            String businessNumber,
            String representativeName,
            LocalDate openedOn,
            HttpSession session
    ) {
        BusinessVerificationSession verificationSession = (BusinessVerificationSession) session.getAttribute(SESSION_KEY);

        if (verificationSession == null) {
            throw new Exception400("사업자 검증을 먼저 해주세요.");
        }

        if (verificationSession.isExpired(Instant.now())) {
            session.removeAttribute(SESSION_KEY);

            throw new Exception400("사업자 검증 유효기간이 지났습니다. 다시 검증해주세요.");
        }

        if (!verificationSession.matches(businessNumber, representativeName, openedOn)) {
            session.removeAttribute(SESSION_KEY);

            throw new Exception400("사업자 정보가 변경되었습니다. 다시 검증해주세요.");
        }
    }

//    회원가입이 완료되면 삭제
    public void clear(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }
}

