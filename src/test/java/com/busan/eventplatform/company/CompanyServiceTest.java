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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository repository;

    @Mock
    private EmailVerificationService emailVerificationService;

    private PasswordEncoder passwordEncoder;
    private CompanyService service;
    private MockHttpSession session;

    private static final Instant VERIFIED_AT =
            Instant.parse("2026-11-06T01:00:00Z");

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        session = new MockHttpSession();

        service = new CompanyService(
                repository,
                passwordEncoder,
                emailVerificationService
        );
    }

    // ---------------- 가입 ----------------

    @Test
    void 가입하면_해시와_이메일인증시각을_저장한다() {
        var request = joinRequest();
        request.validate();

        when(repository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session))
                .thenReturn(VERIFIED_AT);

        when(repository.findByBusinessNumber(request.getBusinessNumber()))
                .thenReturn(Optional.empty());

        service.join(request, session);

        ArgumentCaptor<Company> captor =
                ArgumentCaptor.forClass(Company.class);

        verify(repository).save(captor.capture());

        Company saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo(request.getEmail());
        assertThat(saved.getPassword()).isNotEqualTo(request.getPassword());
        assertThat(passwordEncoder.matches(
                request.getPassword(), saved.getPassword())).isTrue();
        assertThat(saved.getEmailVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(saved.getStatus()).isEqualTo(CompanyStatus.PENDING);
        assertThat(saved.getPoint()).isZero();
    }

    @Test
    void 중복된_이메일이면_가입을_거부한다() {
        var request = joinRequest();

        when(repository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(company(1L, CompanyStatus.APPROVED)));

        assertThatThrownBy(() -> service.join(request, session))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("이메일");

        verifyNoInteractions(emailVerificationService);
        verify(repository, never()).save(any(Company.class));
    }

    @Test
    void 이메일_미인증이면_가입을_거부한다() {
        var request = joinRequest();

        when(repository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session))
                .thenThrow(new Exception400("이메일 인증을 먼저 완료해주세요."));

        assertThatThrownBy(() -> service.join(request, session))
                .isInstanceOf(Exception400.class);

        verify(repository, never()).save(any(Company.class));
    }

    @Test
    void 중복된_사업자번호면_가입을_거부한다() {
        var request = joinRequest();

        when(repository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session))
                .thenReturn(VERIFIED_AT);

        when(repository.findByBusinessNumber(request.getBusinessNumber()))
                .thenReturn(Optional.of(company(1L, CompanyStatus.APPROVED)));

        assertThatThrownBy(() -> service.join(request, session))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("사업자");

        verify(repository, never()).save(any(Company.class));
    }

    // ---------------- 로그인 ----------------

    @Test
    void 올바른_비밀번호로_심사대기_기업도_로그인한다() {
        Company company = company(1L, CompanyStatus.PENDING);

        when(repository.findByEmail(company.getEmail()))
                .thenReturn(Optional.of(company));

        SessionCompanyDTO result = service.login(loginRequest("Company123!"));

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(company.getName());
        assertThat(result.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 틀린_비밀번호면_로그인을_거부한다() {
        Company company = company(1L, CompanyStatus.APPROVED);

        when(repository.findByEmail(company.getEmail()))
                .thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.login(loginRequest("Wrong123!")))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    @Test
    void 없는_이메일이면_로그인을_거부한다() {
        when(repository.findByEmail("company@example.test"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginRequest("Company123!")))
                .isInstanceOf(Exception404.class);
    }

    @Test
    void 정지된_기업은_로그인을_거부한다() {
        Company company = company(1L, CompanyStatus.SUSPENDED);

        when(repository.findByEmail(company.getEmail()))
                .thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.login(loginRequest("Company123!")))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("로그인할 수 없는");
    }

    // ---------------- 조회 ----------------

    @Test
    void 상세정보를_조회한다() {
        Company company = company(1L, CompanyStatus.APPROVED);

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        CompanyResponse.DetailDTO result = service.findByDetail(1L);

        assertThat(result.getName()).isEqualTo(company.getName());
        assertThat(result.getEmail()).isEqualTo(company.getEmail());
        assertThat(result.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 수정화면의_기존정보를_조회한다() {
        Company company = company(1L, CompanyStatus.APPROVED);

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        CompanyResponse.UpdateDTO result = service.findByUpdate(1L);

        assertThat(result.getName()).isEqualTo(company.getName());
        assertThat(result.getContactPersonName()).isEqualTo("기존담당자");
    }

    @Test
    void 없는_기업의_조회는_거부한다() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByDetail(999L))
                .isInstanceOf(Exception404.class);

        assertThatThrownBy(() -> service.findByUpdate(999L))
                .isInstanceOf(Exception404.class);
    }

    // ---------------- 부분 수정·심사 상태 ----------------

    @Test
    void 담당자만_수정하면_다른정보와_승인상태를_유지한다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setContactPersonName("새담당자");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        SessionCompanyDTO result = service.update(1L, request, session);

        assertThat(company.getContactPersonName()).isEqualTo("새담당자");
        assertThat(company.getContactPersonPhone()).isEqualTo("010-0000-0001");
        assertThat(company.getName()).isEqualTo("테스트기업");
        assertThat(company.getEmail()).isEqualTo("company@example.test");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
        assertThat(result.getId()).isEqualTo(1L);

        verifyNoInteractions(emailVerificationService);
    }

    @Test
    void 기업주소가_변경되면_심사대기로_전환한다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setAddress("부산광역시 해운대구");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        SessionCompanyDTO result = service.update(1L, request, session);

        assertThat(company.getAddress()).isEqualTo("부산광역시 해운대구");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
        assertThat(result.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 동일한_기업정보면_승인상태를_유지한다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setName(company.getName());
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        service.update(1L, request, session);

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 심사대기중_기업인증정보_변경은_거부한다() {
        Company company = company(1L, CompanyStatus.PENDING);
        var request = new CompanyRequest.UpdateDTO();
        request.setAddress("새로운 주소");
        request.setContactPersonName("변경예정담당자");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.update(1L, request, session))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("검토 중");

        assertThat(company.getAddress()).isEqualTo("부산광역시");
        assertThat(company.getContactPersonName()).isEqualTo("기존담당자");
    }

    @Test
    void 심사대기중에도_담당자정보는_수정한다() {
        Company company = company(1L, CompanyStatus.PENDING);
        var request = new CompanyRequest.UpdateDTO();
        request.setContactPersonPhone("010-1234-5678");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        service.update(1L, request, session);

        assertThat(company.getContactPersonPhone()).isEqualTo("010-1234-5678");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 심사대기중_동일한_기업정보는_허용한다() {
        Company company = company(1L, CompanyStatus.PENDING);
        var request = new CompanyRequest.UpdateDTO();
        request.setName(company.getName());
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        assertThatCode(() -> service.update(1L, request, session))
                .doesNotThrowAnyException();

        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 비밀번호만_변경하면_새해시를_저장한다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setPassword("Changed123!");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        service.update(1L, request, session);

        assertThat(passwordEncoder.matches(
                "Changed123!", company.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(
                "Company123!", company.getPassword())).isFalse();
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 없는_기업은_수정할_수_없다() {
        var request = new CompanyRequest.UpdateDTO();
        request.setContactPersonName("새담당자");

        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, request, session))
                .isInstanceOf(Exception404.class);
    }

    // ---------------- 이메일 변경 ----------------

    @Test
    void 인증된_새이메일로_변경하고_인증시각을_갱신한다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setEmail("changed@example.test");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));
        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session)).thenReturn(VERIFIED_AT);
        when(repository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        service.update(1L, request, session);

        assertThat(company.getEmail()).isEqualTo("changed@example.test");
        assertThat(company.getEmailVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 새이메일_인증실패시_다른정보도_수정하지_않는다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        var request = new CompanyRequest.UpdateDTO();
        request.setEmail("changed@example.test");
        request.setContactPersonName("변경예정담당자");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));
        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session))
                .thenThrow(new Exception400("이메일 인증을 먼저 완료해주세요."));

        assertThatThrownBy(() -> service.update(1L, request, session))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("인증");

        assertThat(company.getEmail()).isEqualTo("company@example.test");
        assertThat(company.getContactPersonName()).isEqualTo("기존담당자");
        assertThat(company.getEmailVerifiedAt()).isNull();

        verify(repository, never()).findByEmail(anyString());
    }

    @Test
    void 다른기업의_이메일과_중복되면_변경하지_않는다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        Company other = company(2L, CompanyStatus.APPROVED);

        var request = new CompanyRequest.UpdateDTO();
        request.setEmail("other@example.test");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));
        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session)).thenReturn(VERIFIED_AT);
        when(repository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L, request, session))
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("이미 사용중");

        assertThat(company.getEmail()).isEqualTo("company@example.test");
        assertThat(company.getEmailVerifiedAt()).isNull();
    }

    @Test
    void 같은_이메일이면_재인증없이_기존인증시각을_유지한다() {
        Company company = company(1L, CompanyStatus.APPROVED);
        company.verifyEmail(VERIFIED_AT);

        var request = new CompanyRequest.UpdateDTO();
        request.setEmail(company.getEmail());
        request.setContactPersonName("새담당자");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));

        service.update(1L, request, session);

        assertThat(company.getContactPersonName()).isEqualTo("새담당자");
        assertThat(company.getEmailVerifiedAt()).isEqualTo(VERIFIED_AT);

        verifyNoInteractions(emailVerificationService);
        verify(repository, never()).findByEmail(anyString());
    }

    @Test
    void 심사대기중에도_인증된_새이메일로_변경한다() {
        Company company = company(1L, CompanyStatus.PENDING);
        var request = new CompanyRequest.UpdateDTO();
        request.setEmail("changed@example.test");
        request.validate();

        when(repository.findById(1L)).thenReturn(Optional.of(company));
        when(emailVerificationService.requireVerifiedEmail(
                request.getEmail(), session)).thenReturn(VERIFIED_AT);
        when(repository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        service.update(1L, request, session);

        assertThat(company.getEmail()).isEqualTo("changed@example.test");
        assertThat(company.getEmailVerifiedAt()).isEqualTo(VERIFIED_AT);
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    // ---------------- 테스트 데이터 ----------------

    private Company company(Long id, CompanyStatus status) {
        Company company = Company.builder()
                .name("테스트기업")
                .email("company@example.test")
                .password(passwordEncoder.encode("Company123!"))
                .businessNumber("0000000001")
                .representativeName("테스트대표")
                .openedOn(LocalDate.of(2020, 1, 1))
                .address("부산광역시")
                .contactPersonName("기존담당자")
                .contactPersonPhone("010-0000-0001")
                .status(status)
                .build();

        ReflectionTestUtils.setField(company, "id", id);
        return company;
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
        request.setContactPersonName("기존담당자");
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