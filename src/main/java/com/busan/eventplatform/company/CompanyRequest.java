package com.busan.eventplatform.company;

import lombok.Data;

import java.time.LocalDate;
import java.util.Objects;

public class CompanyRequest {

    @Data
    public static class JoinDTO {

        private String name;
        private String email;
        private String password;
        private String businessNumber;
        private String representativeName;
        private LocalDate openedOn;
        private String address;
        private String businessCertificateFileName;
        private String contactPersonName;
        private String contactPersonPhone;

        //        TODO 글로벌 예외 적용
        public void validate() {
            if (name == null || name.isBlank()) {
                throw new RuntimeException("기업명을 입력해주세요.");
            }

            if (email == null || email.isBlank()) {
                throw new RuntimeException("이메일을 입력해주세요.");
            }

            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new RuntimeException("이메일 형식으로 입력해주세요.");
            }

            if (password == null || password.isBlank()) {
                throw new RuntimeException("비밀번호를 입력해주세요.");
            }

            if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{8,16}$")) {
                throw new RuntimeException("비밀번호는 영문자, 숫자, 특수문자($@!%*#?&)를 모두 포함한 8자~16자여야 합니다.");
            }

            if (businessNumber == null || businessNumber.isBlank()) {
                throw new RuntimeException("사업자 번호를 입력해주세요.");
            }

            if (representativeName == null || representativeName.isBlank()) {
                throw new RuntimeException("대표자 명을 입력해주세요.");
            }

            if (openedOn == null) {
                throw new RuntimeException("개업일자를 입력해주세요.");
            }

            if (address == null || address.isBlank()) {
                throw new RuntimeException("기업 주소를 입력해주세요.");
            }

            // TODO: 파일 처리 구현 후 활성화
            // if (businessCertificateFileName == null
            //         || businessCertificateFileName.isBlank()) {
            //     throw new RuntimeException("사업자 등록증을 제출해주세요.");
            // }

            if (contactPersonName == null || contactPersonName.isBlank()) {
                throw new RuntimeException("담당자 명을 입력해주세요.");
            }

            if (contactPersonPhone == null || contactPersonPhone.isBlank()) {
                throw new RuntimeException("담당자 전화번호를 입력해주세요.");
            }
        }

        public Company toEntity() {
            return Company.builder()
                    .name(name)
                    .email(email)
                    .password(password)
                    .businessNumber(businessNumber)
                    .representativeName(representativeName)
                    .openedOn(openedOn)
                    .address(address)
                    .businessCertificateFileName(businessCertificateFileName)
                    .contactPersonName(contactPersonName)
                    .contactPersonPhone(contactPersonPhone)
                    .build();
        }
    }

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

        private String businessCertificateFileName; // 사업자등록증 파일

        //        수정 시 PENDING으로 변경할지 여부를 정하는 메서드
        public boolean requiresReview(Company company) {
            return name != null && !name.trim().isBlank() && !Objects.equals(name, company.getName())
                    || businessNumber != null && !businessNumber.trim().isBlank() && !Objects.equals(businessNumber, company.getBusinessNumber())
                    || representativeName != null && !representativeName.trim().isBlank() && !Objects.equals(representativeName, company.getRepresentativeName())
                    || address != null && !address.trim().isBlank() && !Objects.equals(address, company.getAddress());
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
                    && businessCertificateFileName == null) {
                throw new RuntimeException("수정할 정보를 입력해주세요.");
            }

            // 2. 전달된 값이 공백이면 거부
            if ((name != null && name.isBlank())
                    || (businessNumber != null && businessNumber.isBlank())
                    || (representativeName != null && representativeName.isBlank())
                    || (address != null && address.isBlank())
                    || (contactPersonName != null && contactPersonName.isBlank())
                    || (contactPersonPhone != null && contactPersonPhone.isBlank())) {
                throw new RuntimeException("입력한 수정 정보는 공백일 수 없습니다.");
            }

            if (email != null) {
                if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                    throw new RuntimeException("이메일 형식이 올바르지 않습니다.");
                }
            }

            if (password != null) {
                if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@!%*#?&])[A-Za-z\\d$@!%*#?&]{8,16}$")) {
                    throw new RuntimeException("비밀번호 형식이 올바르지 않습니다.");
                }
            }
        }
    }
}
