package com.busan.eventplatform.company;

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

//    기업 가입절차
    @Transactional
    public CompanyResponse.JoinDTO join(CompanyRequest.JoinDTO joinDTO, HttpSession session) {

        companyRepository.findByEmail(joinDTO.getEmail()).ifPresent(company -> {
            log.warn("회원가입 실패 중복된 이메일 : {}", joinDTO.getEmail());
            throw new Exception400("이미 가입되어있는 이메일입니다.");
        });

        Instant verifiedAt = emailVerificationService.requireVerifiedEmail(joinDTO.getEmail(), session);

        companyRepository.findByBusinessNumber(joinDTO.getBusinessNumber()).ifPresent(company -> {
            log.warn("회원가입 실패 중복된 사업자 등록번호 : {}", joinDTO.getBusinessNumber());
            throw new Exception400("이미 등록되어 있는 사업자 번호입니다.");
        });

        String encodedPassword = passwordEncoder.encode(joinDTO.getPassword());

        Company company = joinDTO.toEntity(encodedPassword);
        company.verifyEmail(verifiedAt);

        companyRepository.save(company);

        return new CompanyResponse.JoinDTO(company);
    }

    @Transactional
    public SessionCompanyDTO login(CompanyRequest.LoginDTO loginDTO) {
        Company company = companyRepository.findByEmail(loginDTO.getEmail()).orElseThrow(
                () -> new Exception404("이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(loginDTO.getPassword(), company.getPassword())) {
            throw new Exception400("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        if (company.getStatus() == CompanyStatus.SUSPENDED) {
            throw new Exception400("현재 로그인할 수 없는 계정입니다.");
        }

        return new SessionCompanyDTO(company);
    }

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
    public SessionCompanyDTO update(Long id, CompanyRequest.UpdateDTO updateDTO, HttpSession session) {
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

        return new SessionCompanyDTO(company);
    }

//    TODO 기업 회원 탈퇴 (임시보류)

}
