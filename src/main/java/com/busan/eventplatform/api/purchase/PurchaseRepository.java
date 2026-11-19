package com.busan.eventplatform.api.purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface PurchaseRepository extends JpaRepository<Purchase , Long>{

    // 해당 이벤트를 해당 유저가 이미 구매 결제했는지?
    @Query("""
            select case when count(p.user) > 0 then true else false end
            from Purchase p 
            where p.user.id = :userId and p.eventSession.id = :sessionId and p.status = :status
            """
            )
    boolean existsEventByUser (@Param("userId") Long userId ,
                               @Param("sessionId") Long sessionId ,
                                @Param("status") PurchaseStatus status);


    // purchase , event , eventSession 가져오기.
    @Query("""
    select p from Purchase p join fetch p.eventSession es join fetch es.event e 
    where p.user.id = :userId order by p.purchasedAt desc
    """)
    List<Purchase> findByUserIdWithPurchaseList(@Param("userId") Long userId);


}
