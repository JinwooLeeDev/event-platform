package com.busan.eventplatform.login.companylogin;

import com.busan.eventplatform.company.businessverification.BusinessVerificationService;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.login.LoginReqDto;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.global.file.FileStorageResult;
import com.busan.eventplatform.login.emailverification.EmailVerificationService;
import jakarta.servlet.http.HttpSession;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyAuthService {

    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final FileStorage fileStorage;
    private final PlatformTransactionManager transactionManager;
    private final BusinessVerificationService businessVerificationService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void signup(CompanySignupReqDto request, HttpSession session) {
        request.validate();

        companyRepository.findByEmail(request.getEmail()).ifPresent(company -> {
            throw new Exception400("이미 가입되어있는 이메일입니다.");
        });

        Instant verifiedAt = emailVerificationService.requireVerifiedEmail(request.getEmail(), session);

        companyRepository.findByBusinessNumber(request.getBusinessNumber()).ifPresent(company -> {
            throw new Exception400("이미 등록되어 있는 사업자 번호입니다.");
        });

        BusinessVerificationStatus verificationStatus = businessVerificationService.verify(
                request.getBusinessNumber(),
                request.getRepresentativeName(),
                request.getOpenedOn()
        );

        if (verificationStatus == BusinessVerificationStatus.MISMATCHED) {
            throw new Exception400(
                    "사업자 정보가 일치하지 않습니다. "
                            + "사업자등록번호·대표자명·개업일자를 확인해주세요."
            );
        }

        // TODO: 외부 사업자 진위 확인 모듈을 파일 저장 전에 연결합니다.
        // 현재는 외부 검증 완료로 간주하지 않으며 관리자 승인 대기로 저장합니다.
        String passwordHash = passwordEncoder.encode(request.getPassword());
        FileStorageResult storedFile = fileStorage.upload(request.getBusinessCertificate());

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transaction.executeWithoutResult(status -> {
                Company company = Company.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordHash)
                .businessNumber(request.getBusinessNumber())
                .representativeName(request.getRepresentativeName())
                .openedOn(request.getOpenedOn())
                .address(request.getAddress())
                .contactPersonName(request.getContactPersonName())
                .contactPersonPhone(request.getContactPersonPhone())
                .status(CompanyStatus.PENDING)
                .businessCertificateFileName(storedFile.getStorageKey())
                .build();
                company.verifyEmail(verifiedAt);
                company.recordBusinessVerification(verificationStatus);
                companyRepository.saveAndFlush(company);
            });
        } catch (RuntimeException | Error failure) {
            try {
                fileStorage.delete(storedFile.getStorageKey());
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
                log.error("기업 가입 실패 후 업로드 파일 정리 실패: {}", storedFile.getStorageKey(), cleanupFailure);
            }
            throw failure;
        }
    }

    @Transactional(readOnly = true)
    public SessionCompanyDto login(LoginReqDto request) {
        request.validate();

        Company company = companyRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new Exception401(
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                company.getPassword()
        )) {
            throw new Exception401(
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        if (company.getStatus() == null) {
            throw new Exception403(
                    "기업 계정 상태를 확인할 수 없습니다."
            );
        }

        switch (company.getStatus()) {
            case PENDING ->
                    throw new Exception403(
                            "관리자 승인 대기 중인 기업입니다."
                    );

            case REJECTED ->
                    throw new Exception403(
                            "기업 가입 승인이 거부되었습니다."
                    );

            case FEEDBACK_NEEDED ->
                    throw new Exception403(
                            "기업 정보 보완이 필요합니다."
                    );

            case SUSPENDED ->
                    throw new Exception403(
                            "이용이 정지된 기업 계정입니다."
                    );

            case APPROVED -> {
                // 로그인 허용
            }
        }

        return SessionCompanyDto.builder()
                .id(company.getId())
                .displayName(company.getName())
                .build();
    }
}
