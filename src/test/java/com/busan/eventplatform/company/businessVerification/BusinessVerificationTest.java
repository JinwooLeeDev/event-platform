package com.busan.eventplatform.company.businessVerification;

import com.busan.eventplatform.company.businessverification.BusinessVerificationClient;
import com.busan.eventplatform.company.businessverification.BusinessVerificationRequest;
import com.busan.eventplatform.company.businessverification.BusinessVerificationResponse;
import com.busan.eventplatform.company.businessverification.BusinessVerificationService;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessVerificationServiceTest {

    @Mock
    private BusinessVerificationClient client;

    private BusinessVerificationService service;

    private static final String BUSINESS_NUMBER = "1234567890";
    private static final String REPRESENTATIVE_NAME = "홍길동";
    private static final LocalDate OPENED_ON = LocalDate.of(2020, 1, 1);

    @BeforeEach
    void setUp() {
        service = new BusinessVerificationService(client);
    }

    @Test
    void 정보가_일치하면_MATCHED를_반환한다() {
        // given
        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response("01"));

        // when
        BusinessVerificationStatus status = callVerify();

        // then
        assertThat(status).isEqualTo(BusinessVerificationStatus.MATCHED);
    }

    @Test
    void 정보가_불일치하면_MISMATCHED를_반환한다() {
        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response("02"));

        BusinessVerificationStatus status = callVerify();

        assertThat(status).isEqualTo(BusinessVerificationStatus.MISMATCHED);
    }

    @Test
    void 통신에_실패하면_예외를_전파하지_않고_ERROR를_반환한다() {
        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenThrow(new ResourceAccessException("테스트용 연결 실패"));

        BusinessVerificationStatus status = callVerify();

        assertThat(status).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 응답이_null이면_ERROR를_반환한다() {
        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(null);

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 요청_처리_결과가_OK가_아니면_ERROR를_반환한다() {
        BusinessVerificationResponse.Validate response = response("01");
        response.setStatusCode("UNEXPECTED");

        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response);

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 결과_목록이_null이면_ERROR를_반환한다() {
        BusinessVerificationResponse.Validate response = response("01");
        response.setData(null);

        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response);

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 결과_목록이_비어있으면_ERROR를_반환한다() {
        BusinessVerificationResponse.Validate response = response("01");
        response.setData(List.of());

        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response);

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 결과가_여러_건이면_ERROR를_반환한다() {
        BusinessVerificationResponse.Validate response = response("01");
        BusinessVerificationResponse.Result result =
                response.getData().get(0);

        response.setData(List.of(result, result));

        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response);

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 결과_객체가_null이면_ERROR를_반환한다() {
        BusinessVerificationResponse.Validate response = response("01");
        response.setData(Collections.singletonList(null));

        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response);

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 응답의_사업자번호가_다르면_ERROR를_반환한다() {
        BusinessVerificationResponse.Validate response = response("01");
        response.getData().get(0).setBusinessNumber("9999999999");

        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response);

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 알_수_없는_검증코드는_ERROR를_반환한다() {
        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response("99"));

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 검증코드가_null이면_ERROR를_반환한다() {
        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response(null));

        assertThat(callVerify()).isEqualTo(BusinessVerificationStatus.ERROR);
    }

    @Test
    void 요청에_사업자정보와_변환된_개업일자를_전달한다() {
        when(client.validate(any(BusinessVerificationRequest.Validate.class)))
                .thenReturn(response("01"));

        callVerify();

        ArgumentCaptor<BusinessVerificationRequest.Validate> captor =
                ArgumentCaptor.forClass(
                        BusinessVerificationRequest.Validate.class
                );

        verify(client).validate(captor.capture());

        List<BusinessVerificationRequest.Business> businesses =
                captor.getValue().getBusinesses();

        assertThat(businesses).hasSize(1);

        BusinessVerificationRequest.Business business = businesses.get(0);

        assertThat(business.getBusinessNumber()).isEqualTo(BUSINESS_NUMBER);
        assertThat(business.getRepresentativeName()).isEqualTo(REPRESENTATIVE_NAME);
        assertThat(business.getOpenedOn()).isEqualTo("20200101");
    }

    // 동일한 입력으로 Service 호출
    private BusinessVerificationStatus callVerify() {
        return service.verify(
                BUSINESS_NUMBER,
                REPRESENTATIVE_NAME,
                OPENED_ON
        );
    }

    // 국세청 응답 대신 사용할 테스트용 응답 생성
    private BusinessVerificationResponse.Validate response(
            String validationCode
    ) {
        BusinessVerificationResponse.Result result =
                new BusinessVerificationResponse.Result();

        result.setBusinessNumber(BUSINESS_NUMBER);
        result.setValidationCode(validationCode);

        BusinessVerificationResponse.Validate response =
                new BusinessVerificationResponse.Validate();

        response.setStatusCode("OK");
        response.setRequestCount(1);
        response.setMatchedCount("01".equals(validationCode) ? 1 : 0);
        response.setData(List.of(result));

        return response;
    }
}