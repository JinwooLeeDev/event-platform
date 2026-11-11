package com.busan.eventplatform.api.refund;


import com.busan.eventplatform.api.purchase.Purchase;
import com.busan.eventplatform.company.Company;
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

    @Column(nullable = false , length = 500)
    private String reason; // 환불 사유

    private int amount; // 환불된 금액

    @Builder
    public Refund(User user , Company company , Purchase purchase , String reason, int amount) {
        this.user = user;
        this.company = company;
        this.purchase = purchase;
        this.status = RefundStatus.READY;
        this.reason = reason;
        this.amount = amount;
    }
}
