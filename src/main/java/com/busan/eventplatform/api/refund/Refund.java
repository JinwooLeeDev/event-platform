package com.busan.eventplatform.api.refund;


import com.busan.eventplatform.api.purchase.Deposit;
import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.global.error.exception.Exception400;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.sql.Timestamp;

@Entity
@Table(name = "refund_tb") // 테이블명 변경
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @CreationTimestamp
    private Timestamp createdAt;

    @Enumerated(EnumType.STRING)   @Column(nullable = false)
    private RefundStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_id" , unique = true)
    private Deposit deposit;

    @Column(length = 800) // 유저는 사유 없어도 되고 기업은 필요하므로 null -- true.
    private String reason; // 환불 사유

    @Column(nullable = false)
    private int amount; // 환불된 금액

    // User전용 생성자.
    @Builder
    public Refund(User user , Purchase purchase , int amount) {
        this.user = user;
        this.purchase = purchase;
        this.status = RefundStatus.REFUND; // User는 "환불완료"만 사용.
        this.amount = amount;
    }

    // 기업전용 생성자.
    // 취소 승인 시 기업 환불 내역이 없으면 이 생성자로 생성한다.
    @Builder(builderMethodName = "companyBuilder" , builderClassName = "companyBuilder")
    public Refund(Company company , Deposit deposit , String reason) {
        this.company = company;
        this.deposit = deposit;
        this.status = RefundStatus.PENDING; // Company만 "환불 대기" 필요. 유저는 환불되면 DB에 반영되므로 필요없음.
        this.reason = reason;
        this.amount = Deposit.SESSION_DEPOSIT;
    }

    public void setCompanyRefund(int amount , String reason) {
        if(amount < 0 || this.deposit == null) throw new Exception400("환불을 진행할 수 없습니다");
        this.amount = amount;
        this.status = RefundStatus.REFUND;
        this.reason = reason;
    }
}
