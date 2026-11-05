package com.busan.eventplatform.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 스펙을 위한 기본 생성자(외부 접근 차단)
@Getter
@Entity
@Table(name = "users") // PostgreSQL 예약어 충돌 방지를 위해 복수형 사용
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB 생성 bigint 의존
    private Long id; // 회원의 고유 식별 번호(Primary Key)

    @Column(name = "email_normalized", nullable = false, unique = true, length = 255)
    private String emailNormalized; // 정규화(예: 소문자 통일)된 이메일 주소

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; // 보안을 위해 평문이 아닌 해시(암호화) 처리된 비밀번호

    @Column(name = "display_name", nullable = false, length = 100, unique = true)
    private String displayName; // 플랫폼 서비스 화면에 노출되는 사용자의 닉네임

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role; // 회원의 역할 및 권한 (MEMBER, ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status; // 현재 계정의 상태(ACTIVE, SUSPENDED, WITHDRAWN)

    @Column(name = "preferred_locale", length = 10)
    private String preferredLocale; // 다국어 서비스를 위해 사용자가 선택한 선호 언어 코드(예: ko, en, ja, zh-CN)

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt; // 가입 절차 중 이메일 인증을 완료한 정확한 시각

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt; // 회원이 탈퇴한 시각 (데이터를 즉시 삭제하지 않고 보존 기간을 가질 때 활용)

    // 계정이 최초로 생성된(가입된) 시각
    // imestamptz는 타임존을 포함하는 데이터 타입
    // 기획안에서 요구한 'UTC 기준 시각 저장'을 완벽하게 지원
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;



    // 객체 생성 시 가독성과 안정성을 높이는 Builder 패턴 적용
    @Builder
    public User(String emailNormalized, String passwordHash, String displayName, Role role, UserStatus status, String preferredLocale) {
        this.emailNormalized = emailNormalized;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = (status != null) ? status : UserStatus.ACTIVE;
        this.preferredLocale = (preferredLocale != null) ? preferredLocale : "ko";
    }

    // 엔티티 영속화 전 UTC 시각 자동 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Column(nullable = false)
    @ColumnDefault("0")
    private int point;

    // 비즈니스 메서드
    // 1. 회원의 비밀번호를 새로운 암호화(해시) 문자열로 변경하기
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    // 2. 다국어 서비스를 위해 사용자의 선호 언어 (Locale)
    public void updatePreferredLocale(String preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    // 3. 계정 상태를 변경하며, 탈퇴(WITHDRAWN) 상태로 변경 시 현재 시각을 탈퇴 일시로 자동 기록
    public void changeStatus(UserStatus status) {
        this.status = status;
        if(status == UserStatus.WITHDRAWN) {
            this.withdrawnAt = Instant.now();
        }
    }

    // 세션에서 확인한 실제 이메일 인증 완료 시각을 기록합니다.
    public void verifyEmail(Instant verifiedAt) {
        this.emailVerifiedAt =
                java.util.Objects.requireNonNull(verifiedAt);
    }

    // 유저가 포인트 충전시 사용할 메서드.
    public void chargePoint(int amount) {if(amount > 0)  this.point += amount;}
  
    // displayName을 이름이 아닌 닉네임으로 변경해서 닉네임 변경을 하도록하는 메서드.
    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
