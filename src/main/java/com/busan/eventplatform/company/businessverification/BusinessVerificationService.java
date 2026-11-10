package com.busan.eventplatform.company.businessverification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessVerificationService {
    private final BusinessVerificationClient client;

    public BusinessVerificationStatus verify(
            String businessNumber,
            String representativeName,
            LocalDate openedOn
    ) {
//        개업일자를 국세청 요청 형식으로 변환
        String startDate = openedOn.format(DateTimeFormatter.BASIC_ISO_DATE);

//        한 기업의 요청정보 생성
        BusinessVerificationRequest.Business business = new BusinessVerificationRequest.Business(
                businessNumber,
                startDate,
                representativeName
        );

//        해당 기업 정보를 목록으로 감싸 요청 DTO 생성
        BusinessVerificationRequest.Validate request = new BusinessVerificationRequest.Validate(List.of(business));

        BusinessVerificationResponse.Validate response;

//        API 호출. 통신 실패는 ERROR로 반환
        try {
            response = client.validate(request);
        } catch (RestClientException e) {
            log.warn("사업자 검증 API 호출 실패 : {}", e.getClass().getSimpleName());

            return BusinessVerificationStatus.ERROR;
        }

//        응답 존재 여부와 요청 처리 결과 확인
        if (response == null || !"OK".equals(response.getStatusCode())) {
            return BusinessVerificationStatus.ERROR;
        }

//        한 기업을 요청했으므로 결과도 1 건
        if (response.getData() == null || response.getData().size() != 1) {
            return BusinessVerificationStatus.ERROR;
        }

        BusinessVerificationResponse.Result result = response.getData().get(0);

//        요청한 사업자번호인지 확인
        if (result == null || !businessNumber.equals(result.getBusinessNumber())) {
            return BusinessVerificationStatus.ERROR;
        }

        // 8. 진위확인 결과를 enum으로 변환 국세청 응답이 01, 02이기 때문에 이 조건이 들어감
        if ("01".equals(result.getValidationCode())) {
            return BusinessVerificationStatus.MATCHED;
        }

        if ("02".equals(result.getValidationCode())) {
            return BusinessVerificationStatus.MISMATCHED;
        }

//        알 수 없는 결과는 에러
        return BusinessVerificationStatus.ERROR;
    }
}
