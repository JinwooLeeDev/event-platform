package com.busan.eventplatform.company;

import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.global.error.exception.Exception400;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company_tb")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //    기업명
    @Column(nullable = false, length = 50)
    private String name;

    //    기업 이메일
    @Column(nullable = false, unique = true)
    private String email;

    //    비밀번호
    @Column(nullable = false)
    private String password;

    //    사업자 번호
    @Column(name = "business_number", nullable = false, length = 10, unique = true)
    private String businessNumber;

    //    대표자 이름
    @Column(name = "representative_name", nullable = false, length = 50)
    private String representativeName;

    //    개업일자. (LocalDate가 현대적 API라 사용하기 좀 더 편함)
    @Column(name = "opened_on", nullable = false)
    private LocalDate openedOn;

    //    기업 주소
    @Column(nullable = false)
    private String address;

    //    사업자 등록증 파일 제출
    @Column(name = "business_certificate_file_name", nullable = true) // TODO 일단 true, 파일 처리로직이 구현되면 false로 사업자 등록증을 무조건 제출해야 함.
    private String businessCertificateFileName;

    //    담당자 이름
    @Column(name = "contact_person_name", nullable = false, length = 20)
    private String contactPersonName;

    //    담당자 연락처
    @Column(name = "contact_person_phone", nullable = false, length = 13)
    private String contactPersonPhone;

    //    관리자 인증 상태
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    private CompanyStatus status;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int point;

    //    계정 생성일
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

//    사업자 등록증 검사
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NOT_CHECKED'")
    private BusinessVerificationStatus businessVerificationStatus;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Builder
    public Company(String name,
                   String email,
                   String password,
                   String businessNumber,
                   String representativeName,
                   LocalDate openedOn,
                   String address,
                   String businessCertificateFileName,
                   String contactPersonName,
                   String contactPersonPhone,
                   CompanyStatus status) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.businessNumber = businessNumber;
        this.representativeName = representativeName;
        this.openedOn = openedOn;
        this.address = address;
        this.businessCertificateFileName = businessCertificateFileName;
        this.contactPersonName = contactPersonName;
        this.contactPersonPhone = contactPersonPhone;
        this.status = (status == null) ? CompanyStatus.PENDING : status;
        this.businessVerificationStatus = BusinessVerificationStatus.NOT_CHECKED;
    }

    public void update(
            String name,
            String email,
            String representativeName,
            String businessNumber,
            String address,
            String contactPersonName,
            String contactPersonPhone,
            LocalDate openedOn,
            String businessCertificateFileName
    ) {

        if (name != null) {
            this.name = name;
        }

        if (email != null) {
            this.email = email;
        }

        if (representativeName != null) {
            this.representativeName = representativeName;
        }

        if (businessNumber != null) {
            this.businessNumber = businessNumber;
        }

        if (address != null) {
            this.address = address;
        }

        if (contactPersonName != null) {
            this.contactPersonName = contactPersonName;
        }

        if (contactPersonPhone != null) {
            this.contactPersonPhone = contactPersonPhone;
        }

        if (openedOn != null) {
            this.openedOn = openedOn;
        }

        if (businessCertificateFileName != null) {
            this.businessCertificateFileName = businessCertificateFileName;
        }
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

//    인증 요구상태로 전환
    public void requestReview() {
        this.status = CompanyStatus.PENDING;
    }

    public void requestCorrection() {
        this.status = CompanyStatus.FEEDBACK_NEEDED;
    }

    public void verifyEmail(Instant verifiedAt) {
        this.emailVerifiedAt =
                java.util.Objects.requireNonNull(verifiedAt);
    }

    // 기업이 포인트 충전시 사용할 메서드.
    public void chargePoint(int amount) {if(amount > 0)  this.point += amount;}

    //    사업자 번호 검증결과를 기록하는 메서드
    public void recordBusinessVerification(
            BusinessVerificationStatus businessVerificationStatus
    ) {
        this.businessVerificationStatus = java.util.Objects.requireNonNull(businessVerificationStatus); //requireNonNull은 null값 입력을 막아줌
    }


    // 기업 포인트 결제시 사용할 메서드.
    public void deductPoint(Integer fee) {
        if (this.point < fee) {
            throw new Exception400("잔액이 부족합니다.(현재잔액: " + this.point + "원)"
            );
        }
        this.point -= fee;
    }
}
