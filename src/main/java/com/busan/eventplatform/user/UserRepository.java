package com.busan.eventplatform.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailNormalized(String emailNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    @Query("""
            select u from User u
            where (:status is null or u.status = :status)
              and (lower(u.displayName) like :keyword escape '!'
                   or lower(u.emailNormalized) like :keyword escape '!')
            """)
    Page<User> findAdminMembers(@Param("keyword") String keyword,
                                @Param("status") UserStatus status, Pageable pageable);


    // 포인트 충전결제 요청을 동시에 두번 이상 시도할 경우 가장 마지막에 반영된 포인트충전 금액만 충전될 경우를 대비하여 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> LockFindById(@Param("id") Long id);
}
