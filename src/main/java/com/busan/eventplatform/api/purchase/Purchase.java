package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.util.MyDateUtil;
import com.busan.eventplatform.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.time.LocalDate;

@Table(name = "purchase_tb")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventSession_id" , nullable = false)
    private EventSession eventSession; // 유저가 재신청할 우려가 있으므로 user와 unique는 걸지말자.

    @CreationTimestamp
    private Timestamp purchasedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PurchaseStatus status;

    private int price; // 이 결제내역에 저장될 결제한 포인트액.

    @Builder
    public Purchase(User user , EventSession eventSession , int price) {
        this.user = user;
        this.eventSession = eventSession;
        this.price = price;
        this.status = PurchaseStatus.PURCHASED;
    }

}
