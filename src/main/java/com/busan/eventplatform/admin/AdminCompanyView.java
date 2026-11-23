package com.busan.eventplatform.admin;

import com.busan.eventplatform.global.i18n.LocaleMessages;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.company.companyAdminReview.AdminReviewResponse;
import com.busan.eventplatform.global.util.MyDateUtil;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

/** 기업 심사 DTO를 변경하지 않고 관리자 화면용 표시 정보를 구성합니다. */
public final class AdminCompanyView {
    private AdminCompanyView() {}

    public static String list(Model model, List<AdminReviewResponse.ListDTO> companies) {
        String view = AdminPageView.prepare(model, LocaleMessages.display("기업 관리"), LocaleMessages.display("승인 대기 중인 기업의 사업자 정보와 증빙을 검토하세요."), "companies");
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
        String view = AdminPageView.prepare(model, LocaleMessages.display("기업 심사 상세"), LocaleMessages.display("제출된 기업 정보와 증빙을 확인한 뒤 심사해주세요."), "companies");
        model.addAttribute("companyListPage", false);
        model.addAttribute("companyDetailPage", true);
        model.addAttribute("company", company);
        model.addAttribute("companyStatusLabel", statusLabel(company.getStatus()));
        model.addAttribute("companyVerificationLabel", verificationLabel(company.getVerificationStatus()));
        model.addAttribute("reviewHistoryRows", company.getReviewHistory().stream().map(history -> Map.of(
                "fromLabel", statusLabel(history.getFromStatus()), "toLabel", statusLabel(history.getToStatus()),
                "reviewerName", history.getReviewerName(),
                "feedback", history.getFeedback() == null ? "" : history.getFeedback(),
                "hasFeedback", history.getFeedback() != null && !history.getFeedback().isBlank(),
                "createdAtText", history.getCreatedAt() == null ? LocaleMessages.display("기록 시각 없음") : MyDateUtil.timestampFormat(history.getCreatedAt())
        )).toList());
        return view;
    }

    private static String statusLabel(CompanyStatus status) {
        return switch (status) {
            case PENDING -> LocaleMessages.display("승인 대기");
            case APPROVED -> LocaleMessages.display("승인 완료");
            case REJECTED -> LocaleMessages.display("반려");
            case FEEDBACK_NEEDED -> LocaleMessages.display("보완 요청");
            case SUSPENDED -> LocaleMessages.display("정지");
        };
    }

    private static String verificationLabel(BusinessVerificationStatus status) {
        return switch (status) {
            case NOT_CHECKED -> LocaleMessages.display("확인 전");
            case MATCHED -> LocaleMessages.display("정보 일치");
            case MISMATCHED -> LocaleMessages.display("정보 불일치");
            case ERROR -> LocaleMessages.display("검증 오류");
        };
    }
}
