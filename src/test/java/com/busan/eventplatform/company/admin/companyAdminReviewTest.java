package com.busan.eventplatform.company.admin;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.company.companyAdminReview.*;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.user.Role;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyReviewServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final String FILE_KEY = "files/test-certificate.pdf";

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyReviewHistoryRepository companyReviewHistoryRepository;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private CompanyReviewService companyReviewService;

    private User admin;
    private Company company;

    @BeforeEach
    void setUp() {
        // 공통 데이터만 생성한다.
        // Repository 응답은 각 테스트에서 필요한 것만 설정한다.
        admin = User.builder()
                .emailNormalized("admin@example.com")
                .passwordHash("test-password-hash")
                .displayName("관리자")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .preferredLocale("ko")
                .build();

        ReflectionTestUtils.setField(admin, "id", ADMIN_ID);

        company = createCompany(
                COMPANY_ID,
                CompanyStatus.PENDING,
                BusinessVerificationStatus.MATCHED
        );
    }

    @Test
    void 검증이_일치한_기업을_승인하고_이력을_저장한다() {
        givenAdmin();
        givenCompany();

        // 재심사 전의 보완 사유가 남아 있는 상황
        ReflectionTestUtils.setField(
                company,
                "reviewFeedback",
                "이전 보완 사유"
        );

        companyReviewService.approve(COMPANY_ID, ADMIN_ID);

        assertThat(company.getStatus())
                .isEqualTo(CompanyStatus.APPROVED);
        assertThat(company.getReviewFeedback()).isNull();

        CompanyReviewHistory history = captureSavedHistory();

        assertThat(history.getCompany()).isSameAs(company);
        assertThat(history.getReviewer()).isSameAs(admin);
        assertThat(history.getFromStatus())
                .isEqualTo(CompanyStatus.PENDING);
        assertThat(history.getToStatus())
                .isEqualTo(CompanyStatus.APPROVED);
        assertThat(history.getFeedback()).isNull();
    }

    @ParameterizedTest
    @EnumSource(
            value = BusinessVerificationStatus.class,
            names = {"ERROR", "MISMATCHED", "NOT_CHECKED"}
    )
    void 검증이_일치하지_않으면_승인할_수_없다(
            BusinessVerificationStatus verificationStatus
    ) {
        givenAdmin();
        company.recordBusinessVerification(verificationStatus);
        givenCompany();

        assertThatThrownBy(
                () -> companyReviewService.approve(COMPANY_ID, ADMIN_ID)
        ).isInstanceOf(Exception400.class);

        assertThat(company.getStatus())
                .isEqualTo(CompanyStatus.PENDING);
        verifyNoInteractions(companyReviewHistoryRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = CompanyStatus.class,
            names = "PENDING",
            mode = EnumSource.Mode.EXCLUDE
    )
    void 심사대기가_아니면_승인과_보완요청을_할_수_없다(
            CompanyStatus status
    ) {
        givenAdmin();
        ReflectionTestUtils.setField(company, "status", status);
        givenCompany();

        assertThatThrownBy(
                () -> companyReviewService.approve(COMPANY_ID, ADMIN_ID)
        ).isInstanceOf(Exception400.class);

        assertThatThrownBy(
                () -> companyReviewService.requestCorrection(
                        COMPANY_ID,
                        ADMIN_ID,
                        correction("사업자등록증을 다시 제출해주세요.")
                )
        ).isInstanceOf(Exception400.class);

        assertThat(company.getStatus()).isEqualTo(status);
        verifyNoInteractions(companyReviewHistoryRepository);
    }

    @ParameterizedTest
    @EnumSource(
            value = BusinessVerificationStatus.class,
            names = {"MATCHED", "ERROR"}
    )
    void 관리자_심사대상에_보완을_요청하고_사유와_이력을_저장한다(
            BusinessVerificationStatus verificationStatus
    ) {
        givenAdmin();
        company.recordBusinessVerification(verificationStatus);
        givenCompany();

        companyReviewService.requestCorrection(
                COMPANY_ID,
                ADMIN_ID,
                correction("  사업자등록증을 선명하게 제출해주세요.  ")
        );

        assertThat(company.getStatus())
                .isEqualTo(CompanyStatus.FEEDBACK_NEEDED);
        assertThat(company.getReviewFeedback())
                .isEqualTo("사업자등록증을 선명하게 제출해주세요.");

        CompanyReviewHistory history = captureSavedHistory();

        assertThat(history.getCompany()).isSameAs(company);
        assertThat(history.getReviewer()).isSameAs(admin);
        assertThat(history.getFromStatus())
                .isEqualTo(CompanyStatus.PENDING);
        assertThat(history.getToStatus())
                .isEqualTo(CompanyStatus.FEEDBACK_NEEDED);
        assertThat(history.getFeedback())
                .isEqualTo("사업자등록증을 선명하게 제출해주세요.");
    }

    @ParameterizedTest
    @EnumSource(
            value = BusinessVerificationStatus.class,
            names = {"MISMATCHED", "NOT_CHECKED"}
    )
    void 관리자_심사대상이_아니면_보완을_요청할_수_없다(
            BusinessVerificationStatus verificationStatus
    ) {
        givenAdmin();
        company.recordBusinessVerification(verificationStatus);
        givenCompany();

        assertThatThrownBy(
                () -> companyReviewService.requestCorrection(
                        COMPANY_ID,
                        ADMIN_ID,
                        correction("사업자등록증을 다시 제출해주세요.")
                )
        ).isInstanceOf(Exception400.class);

        assertThat(company.getStatus())
                .isEqualTo(CompanyStatus.PENDING);
        assertThat(company.getReviewFeedback()).isNull();
        verifyNoInteractions(companyReviewHistoryRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t\n"})
    void 보완사유가_없으면_처리를_거부한다(String feedback) {
        givenAdmin();

        assertThatThrownBy(
                () -> companyReviewService.requestCorrection(
                        COMPANY_ID,
                        ADMIN_ID,
                        correction(feedback)
                )
        ).isInstanceOf(Exception400.class);

        // 사유 검사에서 차단되므로 기업 조회까지 진행하지 않는다.
        verifyNoInteractions(
                companyRepository,
                companyReviewHistoryRepository
        );
    }

    @Test
    void 보완사유가_2000자를_초과하면_거부한다() {
        givenAdmin();

        assertThatThrownBy(
                () -> companyReviewService.requestCorrection(
                        COMPANY_ID,
                        ADMIN_ID,
                        correction("가".repeat(2001))
                )
        ).isInstanceOf(Exception400.class);

        verifyNoInteractions(
                companyRepository,
                companyReviewHistoryRepository
        );
    }

    @Test
    void 보완사유가_2000자면_처리할_수_있다() {
        givenAdmin();
        givenCompany();

        String feedback = "가".repeat(2000);

        companyReviewService.requestCorrection(
                COMPANY_ID,
                ADMIN_ID,
                correction(feedback)
        );

        assertThat(company.getReviewFeedback()).isEqualTo(feedback);
        assertThat(captureSavedHistory().getFeedback())
                .isEqualTo(feedback);
    }

    @Test
    void 일반회원은_관리자_기능을_사용할_수_없다() {
        ReflectionTestUtils.setField(admin, "role", Role.MEMBER);
        givenAdmin();

        assertThatThrownBy(
                () -> companyReviewService.approve(COMPANY_ID, ADMIN_ID)
        ).isInstanceOf(Exception403.class);

        verifyNoInteractions(
                companyRepository,
                companyReviewHistoryRepository
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = UserStatus.class,
            names = {"SUSPENDED", "WITHDRAWN"}
    )
    void 정지되거나_탈퇴한_관리자는_심사할_수_없다(
            UserStatus status
    ) {
        admin.changeStatus(status);
        givenAdmin();

        assertThatThrownBy(
                () -> companyReviewService.approve(COMPANY_ID, ADMIN_ID)
        ).isInstanceOf(Exception403.class);

        verifyNoInteractions(
                companyRepository,
                companyReviewHistoryRepository
        );
    }

    @Test
    void 활성상태라도_탈퇴시각이_있으면_관리자_접근을_거부한다() {
        ReflectionTestUtils.setField(
                admin,
                "withdrawnAt",
                Instant.now()
        );
        givenAdmin();

        assertThatThrownBy(
                () -> companyReviewService.findReviewTargets(ADMIN_ID)
        ).isInstanceOf(Exception403.class);

        verifyNoInteractions(companyRepository);
    }

    @Test
    void 관리자_계정이_없으면_404가_발생한다() {
        when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> companyReviewService.findReviewTargets(ADMIN_ID)
        ).isInstanceOf(Exception404.class);

        verifyNoInteractions(companyRepository);
    }

    @Test
    void 심사할_기업이_없으면_404가_발생한다() {
        givenAdmin();

        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> companyReviewService.approve(COMPANY_ID, ADMIN_ID)
        ).isInstanceOf(Exception404.class);

        verifyNoInteractions(companyReviewHistoryRepository);
    }

    @Test
    void 심사목록은_대기상태와_일치_또는_오류_조건으로_조회한다() {
        givenAdmin();

        Company errorCompany = createCompany(
                11L,
                CompanyStatus.PENDING,
                BusinessVerificationStatus.ERROR
        );

        List<BusinessVerificationStatus> verificationStatuses = List.of(
                BusinessVerificationStatus.MATCHED,
                BusinessVerificationStatus.ERROR
        );

        when(companyRepository.findReviewTargets(
                CompanyStatus.PENDING,
                verificationStatuses
        )).thenReturn(List.of(company, errorCompany));

        List<AdminReviewResponse.ListDTO> result =
                companyReviewService.findReviewTargets(ADMIN_ID);

        assertThat(result)
                .extracting(AdminReviewResponse.ListDTO::getId)
                .containsExactly(COMPANY_ID, 11L);

        assertThat(result)
                .extracting(
                        AdminReviewResponse.ListDTO::getBusinessVerificationStatus
                )
                .containsExactly(
                        BusinessVerificationStatus.MATCHED,
                        BusinessVerificationStatus.ERROR
                );

        verify(companyRepository).findReviewTargets(
                CompanyStatus.PENDING,
                verificationStatuses
        );
    }

    @Test
    void 심사대상이_없으면_빈_목록을_반환한다() {
        givenAdmin();

        when(companyRepository.findReviewTargets(
                CompanyStatus.PENDING,
                List.of(
                        BusinessVerificationStatus.MATCHED,
                        BusinessVerificationStatus.ERROR
                )
        )).thenReturn(List.of());

        assertThat(companyReviewService.findReviewTargets(ADMIN_ID))
                .isEmpty();
    }

    @Test
    void 상세조회에_기업정보와_담당자정보를_정확하게_전달한다() {
        givenAdmin();
        givenCompany();

        AdminReviewResponse.DetailDTO result =
                companyReviewService.findDetail(COMPANY_ID, ADMIN_ID);

        assertThat(result.getId()).isEqualTo(COMPANY_ID);
        assertThat(result.getName()).isEqualTo(company.getName());
        assertThat(result.getEmail()).isEqualTo(company.getEmail());
        assertThat(result.getBusinessNumber())
                .isEqualTo(company.getBusinessNumber());

        // 현재 DTO에서 두 필드가 반대로 매핑되어 있으면 실패한다.
        assertThat(result.getContactPersonName()).isEqualTo("김담당");
        assertThat(result.getContactPersonPhone())
                .isEqualTo("010-1234-5678");

        assertThat(result.isReviewable()).isTrue();
        assertThat(result.isApprovable()).isTrue();
        assertThat(result.isVerificationError()).isFalse();
    }

    @Test
    void API오류_기업은_보완처리는_가능하지만_승인은_불가능하다() {
        givenAdmin();
        company.recordBusinessVerification(
                BusinessVerificationStatus.ERROR
        );
        givenCompany();

        AdminReviewResponse.DetailDTO result =
                companyReviewService.findDetail(COMPANY_ID, ADMIN_ID);

        assertThat(result.isReviewable()).isTrue();
        assertThat(result.isApprovable()).isFalse();
        assertThat(result.isVerificationError()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = CompanyStatus.class,
            names = "PENDING",
            mode = EnumSource.Mode.EXCLUDE
    )
    void 심사처리가_끝나도_상세조회는_가능하다(CompanyStatus status) {
        givenAdmin();
        ReflectionTestUtils.setField(company, "status", status);
        givenCompany();

        AdminReviewResponse.DetailDTO result =
                companyReviewService.findDetail(COMPANY_ID, ADMIN_ID);

        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.isReviewable()).isFalse();
        assertThat(result.isApprovable()).isFalse();
    }

    @Test
    void 등록된_사업자등록증을_조회한다() {
        givenAdmin();
        givenCompany();

        Resource certificate = new ByteArrayResource(
                new byte[]{1, 2, 3}
        );

        when(fileStorage.load(FILE_KEY)).thenReturn(certificate);

        Resource result = companyReviewService.findCertificate(
                COMPANY_ID,
                ADMIN_ID
        );

        assertThat(result).isSameAs(certificate);
        verify(fileStorage).load(FILE_KEY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void 사업자등록증_키가_없으면_404가_발생한다(String storageKey) {
        givenAdmin();
        ReflectionTestUtils.setField(
                company,
                "businessCertificateFileName",
                storageKey
        );
        givenCompany();

        assertThatThrownBy(
                () -> companyReviewService.findCertificate(
                        COMPANY_ID,
                        ADMIN_ID
                )
        ).isInstanceOf(Exception404.class);

        verifyNoInteractions(fileStorage);
    }

    @Test
    void 저장소에_파일이_없으면_404를_전달한다() {
        givenAdmin();
        givenCompany();

        when(fileStorage.load(FILE_KEY))
                .thenThrow(new Exception404("파일을 찾을 수 없습니다."));

        assertThatThrownBy(
                () -> companyReviewService.findCertificate(
                        COMPANY_ID,
                        ADMIN_ID
                )
        ).isInstanceOf(Exception404.class);
    }

    @Test
    void 일반회원은_사업자등록증을_조회할_수_없다() {
        ReflectionTestUtils.setField(admin, "role", Role.MEMBER);
        givenAdmin();

        assertThatThrownBy(
                () -> companyReviewService.findCertificate(
                        COMPANY_ID,
                        ADMIN_ID
                )
        ).isInstanceOf(Exception403.class);

        verifyNoInteractions(companyRepository, fileStorage);
    }

    // 정상적인 관리자 조회 응답을 설정한다.
    private void givenAdmin() {
        when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(admin));
    }

    // 현재 테스트 기업의 조회 응답을 설정한다.
    private void givenCompany() {
        when(companyRepository.findById(COMPANY_ID))
                .thenReturn(Optional.of(company));
    }

    // 요청 DTO를 만든다.
    private AdminReviewRequest.Correction correction(String feedback) {
        AdminReviewRequest.Correction request =
                new AdminReviewRequest.Correction();

        request.setFeedback(feedback);
        return request;
    }

    // 실제로 저장 요청된 이력을 꺼내서 내용을 검사한다.
    private CompanyReviewHistory captureSavedHistory() {
        ArgumentCaptor<CompanyReviewHistory> captor =
                ArgumentCaptor.forClass(CompanyReviewHistory.class);

        verify(companyReviewHistoryRepository).save(captor.capture());

        return captor.getValue();
    }

    // 테스트용 기업을 생성한다. DB에는 저장하지 않는다.
    private Company createCompany(
            Long id,
            CompanyStatus status,
            BusinessVerificationStatus verificationStatus
    ) {
        Company target = Company.builder()
                .name("테스트기업")
                .email("company" + id + "@example.com")
                .password("test-password-hash")
                .businessNumber("0000000010")
                .representativeName("김대표")
                .openedOn(LocalDate.of(2020, 1, 1))
                .address("부산광역시 남구 테스트로 1")
                .businessCertificateFileName(FILE_KEY)
                .contactPersonName("김담당")
                .contactPersonPhone("010-1234-5678")
                .status(status)
                .build();

        ReflectionTestUtils.setField(target, "id", id);
        target.recordBusinessVerification(verificationStatus);

        return target;
    }
}