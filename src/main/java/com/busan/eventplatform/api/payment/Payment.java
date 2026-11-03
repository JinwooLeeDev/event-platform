package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
    @Id     @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)      @JoinColumn(name = "user_id" , nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)      @JoinColumn(name = "company_id" , nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false)
    private Long amount;

    @CreationTimestamp
    private Timestamp paidAt;

    private String pgTxId;
}
