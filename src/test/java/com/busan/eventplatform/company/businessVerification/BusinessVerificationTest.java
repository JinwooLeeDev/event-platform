package com.busan.eventplatform.company.businessVerification;

import com.busan.eventplatform.company.businessverification.BusinessVerificationRequest;
import com.busan.eventplatform.company.businessverification.BusinessVerificationResponse;
import com.busan.eventplatform.company.businessverification.BusinessVerificationService;
import com.busan.eventplatform.company.businessverification.BusinessVerificationSession;
import com.busan.eventplatform.company.businessverification.BusinessVerificationSessionService;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessVerificationSessionServiceTest {

    private static final String SESSION_KEY = "businessVerification";

    private static final String BUSINESS_NUMBER = "1234567890";
    private static final String REPRESENTATIVE_NAME = "홍길동";
    private static final LocalDate OPENED_ON =
            LocalDate.of(2020, 1, 1);

    @Mock
    private BusinessVerificationService businessVerificationService;

    private BusinessVerificationSessionService service;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        service = new BusinessVerificationSessionService(
                businessVerificationService
        );

        // 테스트마다 독립적인 세션을 사용한다.
        session = new MockHttpSession();
    }

    @Test
    void MATCHED이면_성공정보를_세션에_저장한다() {
        BusinessVerificationRequest.Check request = validRequest();

        when(businessVerificationService.verify(
                BUSINESS_NUMBER,
                REPRESENTATIVE_NAME,
                OPENED_ON
        )).thenReturn(BusinessVerificationStatus.MATCHED);

        Instant before = Instant.now();

        BusinessVerificationResponse.Check response =
                service.check(request, session);

        Instant after = Instant.now();

        assertThat(response.getStatus())
                .isEqualTo(BusinessVerificationStatus.MATCHED);
        assertThat(response.getMessage()).isNotBlank();

        BusinessVerificationSession saved = savedVerification();

        assertThat(saved).isNotNull();
        assertThat(saved.matches(
                BUSINESS_NUMBER,
                REPRESENTATIVE_NAME,
                OPENED_ON
        )).isTrue();

        assertThat(saved.getVerifiedAt())
                .isBetween(before, after);

        verify(businessVerificationService).verify(
                BUSINESS_NUMBER,
                REPRESENTATIVE_NAME,
                OPENED_ON
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = BusinessVerificationStatus.class,
            names = {"MISMATCHED", "ERROR"}
    )
    void 검증에_실패하면_이전_성공기록도_제거한다(
            BusinessVerificationStatus status
    ) {
        saveVerification(Instant.now());

        when(businessVerificationService.verify(
                BUSINESS_NUMBER,
                REPRESENTATIVE_NAME,
                OPENED_ON
        )).thenReturn(status);

        BusinessVerificationResponse.Check response =
                service.check(validRequest(), session);

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getMessage()).isNotBlank();
        assertThat(session.getAttribute(SESSION_KEY)).isNull();
    }

    @Test
    void 재검증에_성공하면_기존_기록을_새_정보로_교체한다() {
        saveVerification(Instant.now());

        BusinessVerificationSession previous = savedVerification();

        BusinessVerificationRequest.Check request = validRequest();
        request.setBusinessNumber("0987654321");

        when(businessVerificationService.verify(
                "0987654321",
                REPRESENTATIVE_NAME,
                OPENED_ON
        )).thenReturn(BusinessVerificationStatus.MATCHED);

        service.check(request, session);

        BusinessVerificationSession saved = savedVerification();

        assertThat(saved).isNotSameAs(previous);
        assertThat(saved.matches(
                "0987654321",
                REPRESENTATIVE_NAME,
                OPENED_ON
        )).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "123",
            "12345678901",
            "123-45-67890",
            "abcdefghij"
    })
    void 사업자번호가_잘못되면_API호출없이_차단한다(
            String businessNumber
    ) {
        saveVerification(Instant.now());

        BusinessVerificationRequest.Check request = validRequest();
        request.setBusinessNumber(businessNumber);

        assertThatThrownBy(
                () -> service.check(request, session)
        ).isInstanceOf(Exception400.class);

        // 잘못된 재검증 요청도 이전 성공 기록을 무효화한다.
        assertThat(session.getAttribute(SESSION_KEY)).isNull();
        verifyNoInteractions(businessVerificationService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void 대표자명이_없으면_API호출없이_차단한다(String name) {
        BusinessVerificationRequest.Check request = validRequest();
        request.setRepresentativeName(name);

        assertThatThrownBy(
                () -> service.check(request, session)
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(businessVerificationService);
    }

    @Test
    void 대표자명이_50자를_초과하면_차단한다() {
        BusinessVerificationRequest.Check request = validRequest();
        request.setRepresentativeName("가".repeat(51));

        assertThatThrownBy(
                () -> service.check(request, session)
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(businessVerificationService);
    }

    @Test
    void 개업일자가_없으면_API호출없이_차단한다() {
        BusinessVerificationRequest.Check request = validRequest();
        request.setOpenedOn(null);

        assertThatThrownBy(
                () -> service.check(request, session)
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(businessVerificationService);
    }

    @Test
    void 성공기록이_없으면_가입검증을_거부한다() {
        assertThatThrownBy(
                () -> requireValidBusiness(
                        BUSINESS_NUMBER,
                        REPRESENTATIVE_NAME,
                        OPENED_ON
                )
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(businessVerificationService);
    }

    @Test
    void 유효한_성공기록과_정보가_일치하면_통과한다() {
        saveVerification(Instant.now());

        BusinessVerificationSession saved = savedVerification();

        assertThatCode(
                () -> requireValidBusiness(
                        BUSINESS_NUMBER,
                        REPRESENTATIVE_NAME,
                        OPENED_ON
                )
        ).doesNotThrowAnyException();

        // 가입 완료 전이므로 검증 통과만으로 기록을 지우지 않는다.
        assertThat(session.getAttribute(SESSION_KEY))
                .isSameAs(saved);

        // 가입 검사에서는 국세청 API를 다시 호출하지 않는다.
        verifyNoInteractions(businessVerificationService);
    }

    @Test
    void 유효시간이_지나면_가입검증을_거부하고_기록을_제거한다() {
        // 11분 전에 성공한 기록을 넣어 만료 상황을 만든다.
        saveVerification(Instant.now().minusSeconds(660));

        assertThatThrownBy(
                () -> requireValidBusiness(
                        BUSINESS_NUMBER,
                        REPRESENTATIVE_NAME,
                        OPENED_ON
                )
        ).isInstanceOf(Exception400.class);

        assertThat(session.getAttribute(SESSION_KEY)).isNull();
        verifyNoInteractions(businessVerificationService);
    }

    @Test
    void 사업자번호가_달라지면_거부하고_기록을_제거한다() {
        saveVerification(Instant.now());

        assertThatThrownBy(
                () -> requireValidBusiness(
                        "0987654321",
                        REPRESENTATIVE_NAME,
                        OPENED_ON
                )
        ).isInstanceOf(Exception400.class);

        assertThat(session.getAttribute(SESSION_KEY)).isNull();
    }

    @Test
    void 대표자명이_달라지면_거부하고_기록을_제거한다() {
        saveVerification(Instant.now());

        assertThatThrownBy(
                () -> requireValidBusiness(
                        BUSINESS_NUMBER,
                        "김대표",
                        OPENED_ON
                )
        ).isInstanceOf(Exception400.class);

        assertThat(session.getAttribute(SESSION_KEY)).isNull();
    }

    @Test
    void 개업일자가_달라지면_거부하고_기록을_제거한다() {
        saveVerification(Instant.now());

        assertThatThrownBy(
                () -> requireValidBusiness(
                        BUSINESS_NUMBER,
                        REPRESENTATIVE_NAME,
                        OPENED_ON.plusDays(1)
                )
        ).isInstanceOf(Exception400.class);

        assertThat(session.getAttribute(SESSION_KEY)).isNull();
    }

    @Test
    void 검증기록은_다른_세션과_공유되지_않는다() {
        saveVerification(Instant.now());

        MockHttpSession anotherSession = new MockHttpSession();

        assertThatThrownBy(
                () -> service.requireVerifiedBusiness(
                        BUSINESS_NUMBER,
                        REPRESENTATIVE_NAME,
                        OPENED_ON,
                        anotherSession
                )
        ).isInstanceOf(Exception400.class);

        // 다른 사용자의 요청으로 원래 세션 기록이 지워지지 않는다.
        assertThat(session.getAttribute(SESSION_KEY)).isNotNull();
    }

    @Test
    void clear는_사업자검증_기록만_제거한다() {
        saveVerification(Instant.now());
        session.setAttribute("otherAttribute", "유지할 값");

        service.clear(session);

        assertThat(session.getAttribute(SESSION_KEY)).isNull();
        assertThat(session.getAttribute("otherAttribute"))
                .isEqualTo("유지할 값");

        // 이미 제거된 상태에서 다시 호출해도 오류가 없어야 한다.
        assertThatCode(
                () -> service.clear(session)
        ).doesNotThrowAnyException();
    }

    @Test
    void 정확히_10분이_되는_순간부터_만료된다() {
        // 현재 시각에 의존하지 않고 경계값을 정확하게 검사한다.
        Instant verifiedAt = Instant.parse("2026-11-13T00:00:00Z");

        BusinessVerificationSession verification =
                new BusinessVerificationSession(
                        BUSINESS_NUMBER,
                        REPRESENTATIVE_NAME,
                        OPENED_ON,
                        verifiedAt
                );

        Instant expiresAt = verifiedAt.plusSeconds(600);

        assertThat(verification.isExpired(
                expiresAt.minusNanos(1)
        )).isFalse();

        assertThat(verification.isExpired(expiresAt)).isTrue();

        assertThat(verification.isExpired(
                expiresAt.plusNanos(1)
        )).isTrue();
    }

    // 정상적인 검증 요청 DTO
    private BusinessVerificationRequest.Check validRequest() {
        BusinessVerificationRequest.Check request =
                new BusinessVerificationRequest.Check();

        request.setBusinessNumber(BUSINESS_NUMBER);
        request.setRepresentativeName(REPRESENTATIVE_NAME);
        request.setOpenedOn(OPENED_ON);

        return request;
    }

    // API 호출 없이 성공 기록이 있는 상황을 만든다.
    private void saveVerification(Instant verifiedAt) {
        BusinessVerificationSession verification =
                new BusinessVerificationSession(
                        BUSINESS_NUMBER,
                        REPRESENTATIVE_NAME,
                        OPENED_ON,
                        verifiedAt
                );

        session.setAttribute(SESSION_KEY, verification);
    }

    // 저장된 성공 기록을 꺼낸다.
    private BusinessVerificationSession savedVerification() {
        return (BusinessVerificationSession)
                session.getAttribute(SESSION_KEY);
    }

    // 반복되는 최종 가입 검사 호출을 묶는다.
    private void requireValidBusiness(
            String businessNumber,
            String representativeName,
            LocalDate openedOn
    ) {
        service.requireVerifiedBusiness(
                businessNumber,
                representativeName,
                openedOn,
                session
        );
    }
}