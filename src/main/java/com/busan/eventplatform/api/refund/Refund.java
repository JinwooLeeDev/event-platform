package com.busan.eventplatform.api.refund;


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

    private int amount; // 환불된 금액
    private String reason; // 환불 사유

    @CreationTimestamp
    private Timestamp createdAt;

    @Builder
    public Refund(User user, int amount, String reason) {
        this.user = user;
        this.amount = amount;
        this.reason = reason;
    }
}
