
package com.busan.eventplatform.login.companylogin;

import java.time.LocalDate;

public interface BusinessVerificationPort {

    void requireVerified(
            String businessNumber,
            String representativeName,
            LocalDate openedOn
    );
}

// Todo - 사업자 정보 일치 여부 확인용 메서드
//      - 실제 설계 코드는 나중에 작성