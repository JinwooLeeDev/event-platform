package com.busan.eventplatform.login.companylogin;

import com.busan.eventplatform.login.LoginReqDto;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.global.error.exception.Exception401;
import com.busan.eventplatform.global.error.exception.Exception403;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyAuthService {

    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

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