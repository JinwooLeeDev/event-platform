package com.busan.eventplatform.login.companylogin;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
public class CompanySignupReqDto {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private String name;
    private String email;
    private String password;
    private String passwordConfirm;

    private String businessNumber;
    private String representativeName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate openedOn;

    private String address;
    private String contactPersonName;
    private String contactPersonPhone;

    private MultipartFile businessCertificate;

    public void validate() {
        name = validateText(name, "기업명", 50);

        // 이메일은 변환하지 않고 입력값 그대로 사용
        if (email == null
                || email.length() > 254
                || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new Exception400(
                    "올바른 이메일 주소를 입력해주세요."
            );
        }

        if (password == null || password.isBlank()) {
            throw new Exception400("비밀번호를 입력해주세요.");
        }

        if (password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new Exception400(
                    "비밀번호는 8자 이상, UTF-8 기준 72바이트 이하로 입력해주세요."
            );
        }

        if (!password.equals(passwordConfirm)) {
            throw new Exception400(
                    "비밀번호 확인이 일치하지 않습니다."
            );
        }

        // 여기서는 형식만 검사하며 실제 진위는 외부 모듈에서 확인
        if (businessNumber == null
                || !businessNumber.matches("[0-9]{10}")) {
            throw new Exception400(
                    "사업자등록번호는 하이픈 없이 숫자 10자리로 입력해주세요."
            );
        }

        representativeName = validateText(
                representativeName,
                "대표자명",
                50
        );

        if (openedOn == null) {
            throw new Exception400("개업일을 입력해주세요.");
        }

        if (openedOn.isAfter(
                LocalDate.now(ZoneId.of("Asia/Seoul"))
        )) {
            throw new Exception400(
                    "개업일은 미래 날짜로 입력할 수 없습니다."
            );
        }

        address = validateText(address, "주소", 255);

        contactPersonName = validateText(
                contactPersonName,
                "담당자명",
                20
        );

        if (contactPersonPhone == null
                || !contactPersonPhone.matches("[0-9+\\- ]{7,13}")) {
            throw new Exception400(
                    "담당자 연락처는 숫자·하이픈·공백·+를 사용해 7~13자로 입력해주세요."
            );
        }

        if (businessCertificate == null
                || businessCertificate.isEmpty()) {
            throw new Exception400(
                    "사업자등록증 파일을 첨부해주세요."
            );
        }
    }

    private String validateText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new Exception400(
                    fieldName + " 항목을 입력해주세요."
            );
        }

        String result = value.strip();

        if (result.length() > maxLength) {
            throw new Exception400(
                    fieldName + " 항목은 "
                            + maxLength + "자 이하로 입력해주세요."
            );
        }

        return result;
    }
}