package com.busan.eventplatform.admin;

import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.company.companyAdminReview.AdminReviewResponse;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

/** 기업 심사 DTO를 변경하지 않고 관리자 화면용 표시 정보를 구성합니다. */
public final class AdminCompanyView {
    private AdminCompanyView() {}

    public static String list(Model model, List<AdminReviewResponse.ListDTO> companies) {
        String view = AdminPageView.prepare(model, "기업 관리", "승인 대기 중인 기업의 사업자 정보와 증빙을 검토하세요.", "companies");
        model.addAttribute("companyListPage", true);
        model.addAttribute("companyDetailPage", false);
        model.addAttribute("companyCount", companies.size());
        model.addAttribute("companies", rows(companies));
        return view;
    }

    static List<Map<String, Object>> rows(List<AdminReviewResponse.ListDTO> companies) {
        return companies.stream().map(company -> Map.<String, Object>of(
                "id", company.getId(), "name", company.getName(), "businessNumber", company.getBusinessNumber(),
                "statusLabel", statusLabel(company.getStatus()),
                "verificationLabel", verificationLabel(company.getBusinessVerificationStatus()))).toList();
    }

    public static String detail(Model model, AdminReviewResponse.DetailDTO company) {
        String view = AdminPageView.prepare(model, "기업 심사 상세", "제출된 기업 정보와 증빙을 확인한 뒤 심사해주세요.", "companies");
        model.addAttribute("companyListPage", false);
        model.addAttribute("companyDetailPage", true);
        model.addAttribute("company", company);
        model.addAttribute("companyStatusLabel", statusLabel(company.getStatus()));
        model.addAttribute("companyVerificationLabel", verificationLabel(company.getVerificationStatus()));
        return view;
    }

    private static String statusLabel(CompanyStatus status) {
        return switch (status) {
            case PENDING -> "승인 대기";
            case APPROVED -> "승인 완료";
            case REJECTED -> "반려";
            case FEEDBACK_NEEDED -> "보완 요청";
            case SUSPENDED -> "정지";
        };
    }

    private static String verificationLabel(BusinessVerificationStatus status) {
        return switch (status) {
            case NOT_CHECKED -> "확인 전";
            case MATCHED -> "정보 일치";
            case MISMATCHED -> "정보 불일치";
            case ERROR -> "검증 오류";
        };
    }
}
