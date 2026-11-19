package com.busan.eventplatform.company.companyAdminReview;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyRepository;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.global.error.exception.Exception404;
import com.busan.eventplatform.global.file.FileStorage;
import com.busan.eventplatform.user.Role;
import com.busan.eventplatform.user.User;
import com.busan.eventplatform.user.UserRepository;
import com.busan.eventplatform.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyReviewService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyReviewHistoryRepository companyReviewHistoryRepository;
    private final FileStorage fileStorage;

    private Company findCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다."));

        return company;
    }

    private Company findReviewableCompany(Long companyId) {

        Company company = findCompany(companyId);

        if (company.getStatus() != CompanyStatus.PENDING) {
            throw new Exception400("심사 대기중인 기업만 처리할 수 있습니다.");
        }

        BusinessVerificationStatus verificationStatus =
                company.getBusinessVerificationStatus();

        if (verificationStatus != BusinessVerificationStatus.MATCHED
                && verificationStatus != BusinessVerificationStatus.ERROR) {
            throw new Exception400(
                    "관리자 심사 대상인 기업만 보완 요청할 수 있습니다."
            );
        }

        return company;
    }

    private User findAdmin(Long adminId) {

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new Exception404("관리자 계정을 확인할 수 없습니다."));

        if (admin.getRole() != Role.ADMIN
                || admin.getStatus() != UserStatus.ACTIVE
                || admin.getWithdrawnAt() != null) {
            throw new Exception403("현재 이용 가능한 관리자 계정만 처리할 수 있습니다.");
        }

        return admin;
    }

    public List<AdminReviewResponse.ListDTO> findReviewTargets(Long adminId) {
        findAdmin(adminId);

        return companyRepository.findReviewTargets(CompanyStatus.PENDING, List.of(BusinessVerificationStatus.MATCHED, BusinessVerificationStatus.ERROR))
                .stream()
                .map(AdminReviewResponse.ListDTO::new)
                .toList();
    }

    @Transactional
    public void approve(Long companyId, Long adminId) {

        User admin = findAdmin(adminId);

        Company company = findReviewableCompany(companyId);
        if (company.getBusinessVerificationStatus() != BusinessVerificationStatus.MATCHED) {
            throw new Exception400("사업자 정보 확인이 완료된 기업만 처리할 수 있습니다.");
        }

//        이전 상태를 먼저 불러옴
        CompanyStatus previousStatus = company.getStatus();

//        엔티티 메서드 승인
        company.approve();

//        이번 관리자 처리 내용을 새 이력으로 기록
        CompanyReviewHistory history = CompanyReviewHistory.builder()
                .company(company)
                .reviewer(admin)
                .fromStatus(previousStatus)
                .toStatus(company.getStatus())
                .feedback(null)
                .build();

        companyReviewHistoryRepository.save(history);
    }

    @Transactional
    public void requestCorrection(Long companyId, Long adminId, AdminReviewRequest.Correction correction) {
        User admin = findAdmin(adminId);

        correction.validate();

        Company company = findReviewableCompany(companyId);

        //        이전 상태를 먼저 불러옴
        CompanyStatus previousStatus = company.getStatus();
        String feedback = correction.getFeedback();

        // 현재 기업 상태와 관리자 사유 변경
        company.requestCorrection(feedback);

        CompanyReviewHistory history = correction.toEntity(company, admin, previousStatus);
        companyReviewHistoryRepository.save(history);
    }

    public AdminReviewResponse.DetailDTO findDetail(
            Long companyId,
            Long adminId
    ) {
        findAdmin(adminId);

//        findReviewableCompany 써선 안 됨. 승인·보완 처리 후에도 상세 조회가 가능하므로 심사 가능 상태는 검사하지 않음
        Company company = findCompany(companyId);

        return new AdminReviewResponse.DetailDTO(company,
                companyReviewHistoryRepository.findByCompanyIdOrderByCreatedAtDesc(companyId));
    }

    public Resource findCertificate(Long companyId, Long adminId) {
        findAdmin(adminId);

        Company company = findCompany(companyId);
        String storageKey = company.getBusinessCertificateFileName();

        if (storageKey == null || storageKey.isBlank()) {
            throw new Exception404("등록된 사업자등록증이 없습니다.");
        }

        return fileStorage.load(storageKey);
    }




}
