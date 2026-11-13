package com.busan.eventplatform.api.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment , Long> {

    @Query("select p from Payment p where p.paymentId = :paymentId")
    Optional<Payment> findByPaymentId(@Param("paymentId") String paymentId);

    @Query("select p from Payment p where p.user.id = :userId")
    List<Payment> findByUserIdWithPayment(@Param("userId") Long userId);
}
