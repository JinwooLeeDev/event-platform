package com.busan.eventplatform.company.businessverification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
