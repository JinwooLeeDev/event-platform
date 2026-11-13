package com.busan.eventplatform.user;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
