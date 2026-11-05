package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment , Long> {

    // TODO  추후 삭제 예정. 지금은 userRepository를 못쓰는 상황이라 임시로 만듦.
    @Query("select u from User u where u.id = :id")
    Optional<User> findUser(@Param("id") Long userId);

    @Query("select p from Payment p where p.paymentId = :paymentId")
    Optional<Payment> findByPayment(@Param("paymentId") String paymentId);
}
