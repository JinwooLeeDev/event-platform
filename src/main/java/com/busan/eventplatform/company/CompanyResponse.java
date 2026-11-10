package com.busan.eventplatform.company;

import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import lombok.Data;

import java.time.LocalDate;

public class CompanyResponse {

    @Data
    public static class UpdateDTO {
        private String email; //기업 이메일
        private String contactPersonName; // 담당자 명
        private String contactPersonPhone; // 담당자 연락처
        private String reviewReason; // 반려 사유

        //        이 부분 수정 시, 사업자등록증 파일 제출 및 status가 PENDING(관리자 인증 필요) 상태로 바뀜.
        private String name; // 기업명
        private String businessNumber; // 사업자번호
        private String representativeName; // 대표자 명
        private LocalDate openedOn; // 개업일자
        private String address; // 기업 주소

        private CompanyStatus status;

        public UpdateDTO(Company company) {
            this.email = company.getEmail();
            this.contactPersonName = company.getContactPersonName();
            this.contactPersonPhone = company.getContactPersonPhone();
            this.name = company.getName();
            this.businessNumber = company.getBusinessNumber();
            this.representativeName = company.getRepresentativeName();
            this.openedOn = company.getOpenedOn();
            this.address = company.getAddress();
            this.status = company.getStatus();

            if (company.getStatus() == CompanyStatus.FEEDBACK_NEEDED) {
                if (company.getBusinessVerificationStatus() == BusinessVerificationStatus.MISMATCHED) {
                    this.reviewReason = "국세청 확인 결과, 제출한 사업자 정보가 일치하지 않습니다. "
                            + "사업자등록번호·대표자명·개업일자를 확인해주세요.";
                } else {
                    this.reviewReason = "기업 인증 정보의 보완이 필요합니다.";
                }
            }
        }

        public boolean isPending() {
            return status == CompanyStatus.PENDING;
        }

        public boolean isApproved() {
            return status == CompanyStatus.APPROVED;
        }

        public boolean isFeedbackNeeded() {
            return status == CompanyStatus.FEEDBACK_NEEDED;
        }

        public boolean isRejected() {
            return status == CompanyStatus.REJECTED;
        }

        public boolean isSuspended() {
            return status == CompanyStatus.SUSPENDED;
        }
    }

    @Data
    public static class DetailDTO {
        private String email; //기업 이메일
        private String contactPersonName; // 담당자 명
        private String contactPersonPhone; // 담당자 연락처

        private String name; // 기업명
        private String businessNumber; // 사업자번호
        private String representativeName; // 대표자 명
        private String address; // 기업 주소

        private CompanyStatus status;

        public DetailDTO(Company company) {
            this.email = company.getEmail();
            this.contactPersonName = company.getContactPersonName();
            this.contactPersonPhone = company.getContactPersonPhone();
            this.name = company.getName();
            this.businessNumber = company.getBusinessNumber();
            this.representativeName = company.getRepresentativeName();
            this.address = company.getAddress();
            this.status = company.getStatus();
        }

        public boolean isPending() {
            return status == CompanyStatus.PENDING;
        }

        public boolean isApproved() {
            return status == CompanyStatus.APPROVED;
        }

        public boolean isFeedbackNeeded() {
            return status == CompanyStatus.FEEDBACK_NEEDED;
        }

        public boolean isRejected() {
            return status == CompanyStatus.REJECTED;
        }

        public boolean isSuspended() {
            return status == CompanyStatus.SUSPENDED;
        }
    }
}
