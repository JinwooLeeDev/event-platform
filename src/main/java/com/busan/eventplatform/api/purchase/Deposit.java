package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.events.Event;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/*
 기업 보증금 결제를 위한 테이블.
 purchase테이블에 User , 기업의 결제건을 통합하고 이 둘을 구분해주는 type컬럼 하나 생성해서 설계하려 했으나 제약조건이 복잡해질 수 있고
 테이블의 명확한 역할이 애매모호해질 수 있음.
 --> 차라리 기업 보증금 결제를 위한 Deposit 테이블을 생성해서 User , 기업의 결제건을 나눠서 관리하고 제약조건이 꼬이지 않고 테이블 역할을
 명확하게 명시.
 */

@Data
@Entity
@Table(name = "deposit_tb")
@NoArgsConstructor
@AllArgsConstructor
public class Deposit {
    // 회차당 보증금 15만 포인트.
    public static final int SESSION_DEPOSIT = 150000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DepositStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id" , nullable = false)
    private Company company;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id" , nullable = false , unique = true)
    private Event event;

    @CreationTimestamp
    private Timestamp paidAt;

    @Column(nullable = false)
    private int amount;

    @Builder
    public Deposit(Company company, Event event , int sessionCount) {
        this.status = DepositStatus.READY;
        this.company = company;
        this.event = event;
        this.amount = sessionCount * SESSION_DEPOSIT;
    }
}
