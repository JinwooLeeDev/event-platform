package com.busan.eventplatform.company;

import com.busan.eventplatform.company.businessverification.BusinessVerificationSessionService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.login.emailverification.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private BusinessVerificationSessionService businessVerificationSessionService;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private PlatformTransactionManager platformTransactionManager;

    @InjectMocks
    private CompanyService companyService;

    // 테스트마다 독립적인 기업 객체를 생성한다.
    private Company createCompany(CompanyStatus status) {
        Company company = Company.builder()
                .name("테스트기업")
                .email("company@example.com")
                .password("encoded-password")
                .businessNumber("1234567890")
                .representativeName("대표자")
                .openedOn(LocalDate.of(2020, 1, 1))
                .address("부산광역시 부산진구")
                .businessCertificateFileName("files/old.pdf")
                .contactPersonName("기존담당자")
                .contactPersonPhone("010-1234-5678")
                .status(status)
                .build();

        ReflectionTestUtils.setField(company, "id", 1L);
        ReflectionTestUtils.setField(company, "point", 0);

        return company;
    }

    // 이 단계에서는 실제 DB 대신 서비스의 분기와 호출을 검증한다.
    private void prepareTransaction() {
        when(platformTransactionManager.getTransaction(
                any(TransactionDefinition.class)
        )).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void 기업_상세정보를_조회한다() {
        Company company = createCompany(CompanyStatus.APPROVED);
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        CompanyResponse.DetailDTO response =
                companyService.findByDetail(1L);

        assertThat(response.getName()).isEqualTo("테스트기업");
        assertThat(response.getEmail()).isEqualTo("company@example.com");
        assertThat(response.getContactPersonName()).isEqualTo("기존담당자");
        assertThat(response.getContactPersonPhone())
                .isEqualTo("010-1234-5678");
        assertThat(response.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }

    @Test
    void 없는_기업의_상세조회는_거부한다() {
        when(companyRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.findByDetail(1L))
                .isInstanceOf(Exception404.class);
    }

    @Test
    void 담당자만_수정하면_기존정보와_승인상태를_유지한다() {
        prepareTransaction();

        Company company = createCompany(CompanyStatus.APPROVED);
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        CompanyRequest.UpdateDTO request = new CompanyRequest.UpdateDTO();
        request.setContactPersonName("새담당자");
        request.setContactPersonPhone("010-9876-5432");

        CompanyResponse.DetailDTO response =
                companyService.update(1L, request, new MockHttpSession());

        assertThat(response.getContactPersonName()).isEqualTo("새담당자");
        assertThat(response.getContactPersonPhone())
                .isEqualTo("010-9876-5432");

        // 전달하지 않은 필드는 유지되어야 한다.
        assertThat(company.getName()).isEqualTo("테스트기업");
        assertThat(company.getEmail()).isEqualTo("company@example.com");
        assertThat(company.getBusinessNumber()).isEqualTo("1234567890");
        assertThat(company.getPassword()).isEqualTo("encoded-password");
        assertThat(company.getBusinessCertificateFileName())
                .isEqualTo("files/old.pdf");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);

        verifyNoInteractions(
                fileStorage,
                emailVerificationService,
                businessVerificationSessionService,
                passwordEncoder
        );
    }

    @Test
    void 심사중이어도_담당자정보는_수정할_수_있다() {
        prepareTransaction();

        Company company = createCompany(CompanyStatus.PENDING);
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        CompanyRequest.UpdateDTO request = new CompanyRequest.UpdateDTO();
        request.setContactPersonName("새담당자");

        companyService.update(1L, request, new MockHttpSession());

        assertThat(company.getContactPersonName()).isEqualTo("새담당자");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 심사중에는_기업인증정보를_변경할_수_없다() {
        prepareTransaction();

        Company company = createCompany(CompanyStatus.PENDING);
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        CompanyRequest.UpdateDTO request = new CompanyRequest.UpdateDTO();
        request.setName("변경기업명");

        assertThatThrownBy(() ->
                companyService.update(1L, request, new MockHttpSession())
        )
                .isInstanceOf(Exception400.class)
                .hasMessage("검토 중에는 기업 인증 정보 수정이 불가합니다.");

        assertThat(company.getName()).isEqualTo("테스트기업");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.PENDING);
    }

    @Test
    void 기업인증정보_변경시_새_등록증이_없으면_거부한다() {
        prepareTransaction();

        Company company = createCompany(CompanyStatus.APPROVED);
        when(companyRepository.findById(1L))
                .thenReturn(Optional.of(company));

        CompanyRequest.UpdateDTO request = new CompanyRequest.UpdateDTO();
        request.setAddress("부산광역시 해운대구");

        assertThatThrownBy(() ->
                companyService.update(1L, request, new MockHttpSession())
        )
                .isInstanceOf(Exception400.class)
                .hasMessageContaining("새 사업자 등록증");

        assertThat(company.getAddress()).isEqualTo("부산광역시 부산진구");
        assertThat(company.getStatus()).isEqualTo(CompanyStatus.APPROVED);
    }
}