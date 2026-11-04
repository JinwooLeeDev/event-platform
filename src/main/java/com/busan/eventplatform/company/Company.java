package com.busan.eventplatform.company;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
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
    @Column(nullable = false, length = 10, unique = true)
    private String businessNumber;

//    대표자 이름
    @Column(nullable = false, length = 50)
    private String representativeName;

//    개업일자. (LocalDate가 현대적 API라 사용하기 좀 더 편함)
    @Column(nullable = false)
    private LocalDate openedOn;

//    기업 주소
    @Column(nullable = false)
    private String address;

//    사업자 등록증 파일 제출
    @Column(nullable = true) // TODO 일단 true, 파일 처리로직이 구현되면 false로 사업자 등록증을 무조건 제출해야 함.
    private String businessCertificateFileName;

//    담당자 이름
    @Column(nullable = false, length = 20)
    private String contactPersonName;

//    담당자 연락처
    @Column(nullable = false, length = 13)
    private String contactPersonPhone;

//    관리자 인증 상태
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CompanyStatus status = CompanyStatus.PENDING;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int point;

//    계정 생성일
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Timestamp createdAt;
}
