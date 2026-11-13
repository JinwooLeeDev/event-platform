package com.busan.eventplatform.company.businessverification;

import com.busan.eventplatform.global.error.exception.Exception400;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class BusinessVerificationRequest {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Validate {
        private List<Business> businesses;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Business {

        @JsonProperty("b_no")
        private String businessNumber; // 사업자번호

        @JsonProperty("start_dt")
        private String openedOn; // 사업개시일

        @JsonProperty("p_nm")
        private String representativeName; // 대표자 이름
    }

    @Data
    @NoArgsConstructor
    public static class Check {
        private String businessNumber;
        private String representativeName;
        private LocalDate openedOn;

        public void validate() {
            if (businessNumber == null
                    || !businessNumber.matches("[0-9]{10}")) {
                throw new Exception400(
                        "사업자등록번호는 숫자 10자리로 입력해주세요."
                );
            }

            if (representativeName == null
                    || representativeName.isBlank()) {
                throw new Exception400("대표자명을 입력해주세요.");
            }

            if (representativeName.length() > 50) {
                throw new Exception400("대표자명은 50자까지 입력 가능합니다.");
            }

            if (openedOn == null) {
                throw new Exception400("개업일자를 입력해주세요.");
            }
        }
    }
}
