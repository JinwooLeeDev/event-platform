package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment , Long> {

    @Query("select p from Payment p where p.paymentId = :paymentId")
    Optional<Payment> findByPayment(@Param("paymentId") String paymentId);
}
