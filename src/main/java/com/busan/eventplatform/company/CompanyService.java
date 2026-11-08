package com.busan.eventplatform.company;

import com.busan.eventplatform.company.businessverification.BusinessVerificationService;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.login.emailverification.EmailVerificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final BusinessVerificationService businessVerificationService;

    public CompanyResponse.DetailDTO findByDetail(Long id) {
        Company company = companyRepository.findById(id).orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));
        return new CompanyResponse.DetailDTO(company);
    }

    public CompanyResponse.UpdateDTO findByUpdate(Long id) {
        Company company = companyRepository.findById(id).orElseThrow(() -> new Exception404("기업을 찾을 수 없습니다."));
        return new CompanyResponse.UpdateDTO(company);
    }


//    기업 회원 정보 수정
    @Transactional
    public CompanyResponse.DetailDTO update(Long id, CompanyRequest.UpdateDTO updateDTO, HttpSession session) {
        Company company = companyRepository.findById(id).orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다."));

        boolean requiresReview = updateDTO.requiresReview(company);

        if (company.getStatus() == CompanyStatus.PENDING && requiresReview) {
            throw new Exception400("검토 중에는 기업 인증 정보 수정이 불가합니다.");
        }

        boolean emailChanged = updateDTO.getEmail() != null && !company.getEmail().equals(updateDTO.getEmail());
        Instant verifiedAt = null;

        if (emailChanged) {
            verifiedAt = emailVerificationService.requireVerifiedEmail(updateDTO.getEmail(), session);

            companyRepository.findByEmail(updateDTO.getEmail()).ifPresent(c -> {
                if (!c.getId().equals(company.getId())) {
                    throw new Exception400("이미 사용중인 이메일입니다.");
                }
            });
        }

        company.update(
                updateDTO.getName(),
                updateDTO.getEmail(),
                updateDTO.getRepresentativeName(),
                updateDTO.getBusinessNumber(),
                updateDTO.getAddress(),
                updateDTO.getContactPersonName(),
                updateDTO.getContactPersonPhone(),
                updateDTO.getBusinessCertificateFileName()
        );

        if (updateDTO.getPassword() != null) {
            String encodedPassword = passwordEncoder.encode(updateDTO.getPassword());
            company.changePassword(encodedPassword);
        }

        if (emailChanged) {
            company.verifyEmail(verifiedAt);
        }

//        PENDING 전환이 필요한 데이터가 수정되면 해당 기업 계정을 PENDING 상태로 바꿈
        if (requiresReview) {
            company.requestReview();
        }

        return new CompanyResponse.DetailDTO(company);
    }

//    TODO 기업 회원 탈퇴 (임시보류)

}
