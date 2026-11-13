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

    @Column(length = 800) // 유저는 사유 없어도 되고 기업은 필요하므로 null -- true.
    private String reason; // 환불 사유

    @Column(nullable = false)
    private int amount; // 환불된 금액

    // TODO 추후) 관리자 승인 컬럼 추가.

    @Builder
    public Refund(User user , Company company , Purchase purchase , String reason, int amount , RefundStatus status) {
        this.user = user;
        this.company = company;
        this.purchase = purchase;
        this.status = status; // Company만 "환불 대기" 필요. 유저는 환불되면 DB에 반영되므로 필요없음.
        this.reason = reason;
        this.amount = amount;
    }
}
