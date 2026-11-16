package com.busan.eventplatform.company.companyAdminReview;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.User;
import lombok.Data;

public class AdminReviewRequest {

    @Data
    public static class Correction {

//        관리자가 기업에 전달할 보완사유
        private String feedback;

        public void validate() {
            if (feedback == null || feedback.isBlank()) {
                throw new Exception400("보완 사유를 입력해주세요.");
            }

            feedback = feedback.strip();

            if (feedback.length() > 2000) {
                throw new Exception400("보완사유는 2000자까지 입력 가능합니다.");
            }
        }

        public CompanyReviewHistory toEntity(
                Company company,
                User admin,
                CompanyStatus previousStatus
        ) {
            return CompanyReviewHistory.builder()
                    .company(company)
                    .reviewer(admin)
                    .fromStatus(previousStatus)
                    .toStatus(company.getStatus())
                    .feedback(feedback)
                    .build();

        }
    }
}
