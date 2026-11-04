package com.busan.eventplatform.company;

import lombok.Data;

public class CompanyResponse {
    @Data
    public static class UpdateDTO {
        private String email; //기업 이메일
        private String contactPersonName; // 담당자 명
        private String contactPersonPhone; // 담당자 연락처

        //        이 부분 수정 시, 사업자등록증 파일 제출 및 status가 PENDING(관리자 인증 필요) 상태로 바뀜.
        private String name; // 기업명
        private String businessNumber; // 사업자번호
        private String representativeName; // 대표자 명
        private String address; // 기업 주소

        private CompanyStatus status;

        public UpdateDTO(Company company) {
            this.email = company.getEmail();
            this.contactPersonName = company.getContactPersonName();
            this.contactPersonPhone = company.getContactPersonPhone();
            this.name = company.getName();
            this.businessNumber = company.getBusinessNumber();
            this.representativeName = company.getRepresentativeName();
            this.address = company.getAddress();
            this.status = company.getStatus();
        }
    }
}
