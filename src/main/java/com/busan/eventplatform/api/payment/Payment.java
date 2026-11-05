package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "payment_tb")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id" , nullable = true)  // TODO 추후 false로 변경. 잠시 테스트때문에 false 상태.
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private int amount;

    @CreationTimestamp
    private Timestamp paidAt;

    @Column(unique = true , nullable = false)
    private String paymentId;

    @Builder
    public Payment(User user , int amount , String paymentId) {
        this.user = user;
        this.status = PaymentStatus.READY;
        this.amount = amount;
        this.paymentId = paymentId;
    }

    // 받은 유저가 현재 세션의 유저와 같은지?
    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }

    // 현재 결제상태가 "결제완료" 인지?
    public boolean isPaid() {
        return this.status == PaymentStatus.PAID;
    }

    // 결제완료로 처리.
    public void confirmPay() {this.status = PaymentStatus.PAID;}

    //
}
