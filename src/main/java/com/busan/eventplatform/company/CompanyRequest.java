package com.busan.eventplatform.company;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Objects;

public class CompanyRequest {

    @Data
    public static class UpdateDTO {
        private String email; //기업 이메일
        private String password; //기업 비밀번호
        private String contactPersonName; // 담당자 명
        private String contactPersonPhone; // 담당자 연락처

        //        이 부분 수정 시, 사업자등록증 파일 제출 및 status가 PENDING(관리자 인증 필요) 상태로 바뀜.
        private String name; // 기업명
        private String businessNumber; // 사업자번호
        private String representativeName; // 대표자 명
        private String address; // 기업 주소
        private LocalDate openedOn; // 개업일자
        private MultipartFile businessCertificate; // 사업자등록증 파일

        //        수정 시 PENDING으로 변경할지 여부를 정하는 메서드
        public boolean requiresReview(Company company) {
            return (name != null && !name.trim().isBlank() && !Objects.equals(name, company.getName()))
                    || (businessNumber != null && !businessNumber.trim().isBlank() && !Objects.equals(businessNumber, company.getBusinessNumber()))
                    || (representativeName != null && !representativeName.trim().isBlank() && !Objects.equals(representativeName, company.getRepresentativeName()))
                    || (address != null && !address.trim().isBlank() && !Objects.equals(address, company.getAddress()))
                    || (openedOn != null && !Objects.equals(openedOn, company.getOpenedOn()));
        }

        public boolean hasBusinessCertificate() {
            return businessCertificate != null && !businessCertificate.isEmpty();
        }

        public void validate() {
            // 1. 전부 미전달이면 거부
            if (email == null
                    && password == null
                    && contactPersonName == null
                    && contactPersonPhone == null
                    && name == null
                    && businessNumber == null
                    && representativeName == null
                    && address == null
                    && (businessCertificate == null || businessCertificate.isEmpty())
                    && openedOn == null) {
                throw new Exception400("수정할 정보를 입력해주세요.");
            }

            // 2. 전달된 값이 공백이면 거부
            if ((name != null && name.isBlank())
                    || (businessNumber != null && businessNumber.isBlank())
                    || (representativeName != null && representativeName.isBlank())
                    || (address != null && address.isBlank())
                    || (contactPersonName != null && contactPersonName.isBlank())
                    || (contactPersonPhone != null && contactPersonPhone.isBlank())) {
                throw new Exception400("입력한 수정 정보는 공백일 수 없습니다.");
            }

            if (email != null) {
                if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                    throw new Exception400("이메일 형식이 올바르지 않습니다.");
                }
            }

            if (password != null) {
                if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{8,16}$")) {
                    throw new Exception400("비밀번호 형식이 올바르지 않습니다.");
                }
            }

            if (businessNumber != null) {
                if (!businessNumber.matches("^[0-9]{10}$")) {
                    throw new Exception400("사업자번호는 숫자 10자리여야 합니다.");
                }
            }
        }
    }


}
