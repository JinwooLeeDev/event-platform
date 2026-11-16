package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface PurchaseRepository extends JpaRepository<Purchase , Long>{

    // 해당 이벤트를 해당 유저가 이미 구매 결제했는지?
    @Query("""
            select case when count(p.user) > 0 then true else false end
            from Purchase p 
            where p.user.id = :userId and p.eventSession.id = :eventId 
            """
            )
    boolean existsEventByUser (@Param("userId") Long userId , @Param("eventId") Long eventId);


    // purchase , event , eventSession 가져오기.
    @Query("""
    select p from Purchase p join fetch p.eventSession es join fetch es.event e 
    where p.user.id = :userId order by p.purchasedAt desc
    """)
    List<Purchase> findByUserIdWithPurchaseList(@Param("userId") Long userId);


}
