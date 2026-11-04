package com.busan.eventplatform.company;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

//    기업 가입절차
    @Transactional
    public void join(CompanyRequest.JoinDTO joinDTO) {
        companyRepository.findByEmail(joinDTO.getEmail()).ifPresent(company -> {
            log.warn("회원가입 실패 중복된 이메일 : {}", joinDTO.getEmail());
//            TODO 글로벌 예외 적용
            throw new RuntimeException("이미 가입되어있는 이메일입니다.");
        });

        companyRepository.findByBusinessNumber(joinDTO.getBusinessNumber()).ifPresent(company -> {
            log.warn("회원가입 실패 중복된 사업자 등록번호 : {}", joinDTO.getBusinessNumber());
//            TODO 글로벌 예외 적용
            throw new RuntimeException("이미 등록되어 있는 사업자 번호입니다.");
        });

        Company company = joinDTO.toEntity();

        companyRepository.save(company);
    }

//    기업 회원 정보 수정
    @Transactional
    public void update(Long id, CompanyRequest.UpdateDTO updateDTO) {
//        TODO 글로벌 예외 적용
        Company company = companyRepository.findById(id).orElseThrow(() -> new RuntimeException("해당 기업을 찾을 수 없습니다."));

        boolean requiresReview = updateDTO.requiresReview(company);

//        TODO 비밀번호 해싱처리
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

//        PENDING 전환이 필요한 데이터가 수정되면 해당 기업 계정을 PENDING 상태로 바꿈
        if (requiresReview) {
            company.requestReview();
        }
    }



//    TODO 기업 회원 탈퇴 (보류)

}
