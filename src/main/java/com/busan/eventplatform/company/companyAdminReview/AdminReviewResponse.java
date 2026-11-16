package com.busan.eventplatform.company.companyAdminReview;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import lombok.Getter;

import java.time.LocalDate;

public class AdminReviewResponse {

    @Getter
    public static class ListDTO {
        private final Long id;
        private final String name;
        private final String businessNumber;
        private final BusinessVerificationStatus businessVerificationStatus;
        private final CompanyStatus status;

        public ListDTO(Company company) {
            this.id = company.getId();
            this.name = company.getName();
            this.businessNumber = company.getBusinessNumber();
            this.businessVerificationStatus = company.getBusinessVerificationStatus();
            this.status = company.getStatus();
        }
    }

    @Getter
    public static class DetailDTO {
        private final Long id;
        private final String name;
        private final String email;
        private final String businessNumber;
        private final String representativeName;
        private final LocalDate openedOn;
        private final String address;
        private final String contactPersonName;
        private final String contactPersonPhone;
        private final CompanyStatus status;
        private final BusinessVerificationStatus verificationStatus;
        private final String reviewFeedback;

        public DetailDTO(Company company) {
            this.id = company.getId();
            this.name = company.getName();
            this.email = company.getEmail();
            this.businessNumber = company.getBusinessNumber();
            this.representativeName = company.getRepresentativeName();
            this.openedOn = company.getOpenedOn();
            this.address = company.getAddress();
            this.contactPersonName = company.getContactPersonName();
            this.contactPersonPhone = company.getContactPersonPhone();
            this.status = company.getStatus();
            this.verificationStatus = company.getBusinessVerificationStatus();
            this.reviewFeedback = company.getReviewFeedback();
        }

//        관리자 요청이 가능한 상태
        public boolean isReviewable() {
            return status == CompanyStatus.PENDING
                    && (verificationStatus == BusinessVerificationStatus.MATCHED
                    || verificationStatus == BusinessVerificationStatus.ERROR);
        }

        public boolean isApprovable() {
            return isReviewable()
                    && verificationStatus == BusinessVerificationStatus.MATCHED;
        }

        public boolean isVerificationError() {
            return verificationStatus == BusinessVerificationStatus.ERROR;
        }
    }
}
