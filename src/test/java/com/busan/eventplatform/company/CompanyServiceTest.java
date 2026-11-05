package com.busan.eventplatform.company;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.login.emailverification.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    CompanyRepository companyRepository;

    @Mock
    EmailVerificationService emailVerificationService;

    PasswordEncoder passwordEncoder;
    CompanyService companyService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        companyService = new CompanyService(
                companyRepository,
                passwordEncoder,
                emailVerificationService
        );
    }

    @Test
    void 가입하면_비밀번호를_해시하고_인증시각을_저장한다() {
        var request = joinRequest();
        var session = new MockHttpSession();
        Instant verifiedAt = Instant.parse("2026-11-05T01:00:00Z");

        request.validate();

        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session
        )).thenReturn(verifiedAt);

        when(companyRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        when(companyRepository.findByBusinessNumber(
                request.getBusinessNumber()
        )).thenReturn(Optional.empty());

        companyService.join(request, session);

        ArgumentCaptor<Company> captor =
                ArgumentCaptor.forClass(Company.class);

        verify(companyRepository).save(captor.capture());

        Company saved = captor.getValue();

        assertThat(saved.getPassword()).isNotEqualTo(request.getPassword());
        assertThat(passwordEncoder.matches(
                request.getPassword(), saved.getPassword()
        )).isTrue();
        assertThat(saved.getEmailVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(saved.getStatus()).isEqualTo(CompanyStatus.PENDING);
        assertThat(saved.getPoint()).isZero();
    }

    @Test
    void 이메일_인증이_없으면_가입하지_않는다() {
        var request = joinRequest();
        var session = new MockHttpSession();

        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session
        )).thenThrow(new Exception400("이메일 인증을 먼저 완료해주세요."));

        assertThatThrownBy(() -> companyService.join(request, session))
                .isInstanceOf(Exception400.class);

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void 이메일이_중복이면_가입하지_않는다() {
        var request = joinRequest();
        var session = new MockHttpSession();

        when(companyRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(company(CompanyStatus.APPROVED)));

        assertThatThrownBy(() -> companyService.join(request, session))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("이메일");

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void 사업자번호가_중복이면_가입하지_않는다() {
        var request = joinRequest();
        var session = new MockHttpSession();

        when(companyRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        when(companyRepository.findByBusinessNumber(
                request.getBusinessNumber()
        )).thenReturn(Optional.of(company(CompanyStatus.APPROVED)));

        assertThatThrownBy(() -> companyService.join(request, session))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("사업자");

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void 올바른_비밀번호로_심사대기_기업도_로그인한다() {
        Company company = company(CompanyStatus.PENDING);

        when(companyRepository.findByEmail(company.getEmail()))
                .thenReturn(Optional.of(company));

        SessionCompanyDTO result =
                companyService.login(loginRequest("Company123!"));

        assertThat(result.getName()).isEqualTo(company.getName());
        assertThat(result.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 틀린_비밀번호면_로그인을_거부한다() {
        Company company = company(CompanyStatus.APPROVED);

        when(companyRepository.findByEmail(company.getEmail()))
                .thenReturn(Optional.of(company));

        assertThatThrownBy(() ->
                companyService.login(loginRequest("Wrong123!"))
        ).isInstanceOf(Exception400.class);
    }

    @Test
    void 없는_이메일이면_로그인을_거부한다() {
        when(companyRepository.findByEmail("company@example.test"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                companyService.login(loginRequest("Company123!"))
        ).isInstanceOf(Exception404.class);
    }

    @Test
    void 정지된_기업은_로그인할_수_없다() {
        Company company = company(CompanyStatus.SUSPENDED);

        when(companyRepository.findByEmail(company.getEmail()))
                .thenReturn(Optional.of(company));

        assertThatThrownBy(() ->
                companyService.login(loginRequest("Company123!"))
        ).isInstanceOf(Exception400.class)
                .hasMessageContaining("로그인할 수 없는");
    }

    @Test
    void 담당자만_수정하면_승인상태와_기존정보를_유지한다() {
        Company company = company(CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setContactPersonName("새담당자");
        request.validate();

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        companyService.update(1L, request);

        assertThat(company.getContactPersonName()).isEqualTo("새담당자");
        assertThat(company.getContactPersonPhone()).isEqualTo("010-0000-0001");
        assertThat(company.getName()).isEqualTo("테스트기업");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 기업주소가_변경되면_심사대기로_전환한다() {
        Company company = company(CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setAddress("부산광역시 해운대구");
        request.validate();

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        companyService.update(1L, request);

        assertThat(company.getAddress()).isEqualTo("부산광역시 해운대구");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 동일한_기업정보면_승인상태를_유지한다() {
        Company company = company(CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setName(company.getName());
        request.validate();

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        companyService.update(1L, request);

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 비밀번호만_변경하면_새해시를_저장하고_승인상태를_유지한다() {
        Company company = company(CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setPassword("Changed123!");
        request.validate();

        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        companyService.update(1L, request);

        assertThat(passwordEncoder.matches(
                "Changed123!", company.getPassword()
        )).isTrue();
        assertThat(passwordEncoder.matches(
                "Company123!", company.getPassword()
        )).isFalse();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 존재하지_않는_기업은_수정할_수_없다() {
        when(companyRepository.findById(1L))
                .thenReturn(Optional.empty());

        var request = new CompanyRequest.UpdateDTO();
        request.setContactPersonName("새담당자");

        assertThatThrownBy(() -> companyService.update(1L, request))
                .isInstanceOf(Exception404.class);
    }

    private Company company(CompanyStatus status) {
        return Company.builder()
                .name("테스트기업")
                .email("company@example.test")
                .password(passwordEncoder.encode("Company123!"))
                .businessNumber("0000000001")
                .representativeName("테스트대표")
                .openedOn(LocalDate.of(2020, 1, 1))
                .address("부산광역시")
                .contactPersonName("테스트담당자")
                .contactPersonPhone("010-0000-0001")
                .status(status)
                .build();
    }

    private CompanyRequest.JoinDTO joinRequest() {
        var request = new CompanyRequest.JoinDTO();
        request.setName("테스트기업");
        request.setEmail("company@example.test");
        request.setPassword("Company123!");
        request.setBusinessNumber("0000000001");
        request.setRepresentativeName("테스트대표");
        request.setOpenedOn(LocalDate.of(2020, 1, 1));
        request.setAddress("부산광역시");
        request.setContactPersonName("테스트담당자");
        request.setContactPersonPhone("010-0000-0001");
        return request;
    }

    private CompanyRequest.LoginDTO loginRequest(String password) {
        var request = new CompanyRequest.LoginDTO();
        request.setEmail("company@example.test");
        request.setPassword(password);
        return request;
    }
}