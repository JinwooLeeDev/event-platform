package com.busan.eventplatform.company.businessverification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class BusinessVerificationResponse {

    @NoArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Validate {
        // 	API 요청 처리 결과 코드
        @JsonProperty("status_code")
        private String statusCode;

        // 확인을 요청한 사업자 건수
        @JsonProperty("request_cnt")
        private Integer requestCount;

        // 진위확인에서 유효하다고 확인된 건수
        @JsonProperty("valid_cnt")
        private Integer matchedCount;

        // 사업자별 확인 결과 목록
        private List<Result> data;

    }

    @NoArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        // 사업자번호
        @JsonProperty("b_no")
        private String businessNumber;

        // 제출한 사업자 정보의 진위확인 결과 코드
        @JsonProperty("valid")
        private String validationCode;

        // 진위확인 결과 설명
        @JsonProperty("valid_msg")
        private String validationMessage;

        // 	해당 사업자의 영업 상태 정보 객체
        private Status status;
    }

    @NoArgsConstructor
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        // 사업자 상태
        @JsonProperty("b_stt")
        private String businessStatusName;

        // 사업자 상태 코드
        @JsonProperty("b_stt_cd")
        private String businessStatusCode;

        // 폐업일자
        @JsonProperty("end_dt")
        private String closedOn;
    }
}
