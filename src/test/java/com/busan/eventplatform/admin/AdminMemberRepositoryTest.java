package com.busan.eventplatform.admin;

import com.busan.eventplatform.user.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import static org.junit.jupiter.api.Assertions.*;

class AdminMemberRepositoryTest {
    @Test
    void searchesAndFiltersActualRowsWithLiteralWildcards() {
        try (var factory = new Configuration().addAnnotatedClass(User.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:admin-members;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop").buildSessionFactory();
             var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(User.builder().displayName("Member_One").emailNormalized("one@example.com")
                    .passwordHash("test").role(Role.MEMBER).status(UserStatus.ACTIVE).build());
            em.persist(User.builder().displayName("Member Two").emailNormalized("two@example.com")
                    .passwordHash("test").role(Role.MEMBER).status(UserStatus.SUSPENDED).build());
            em.flush();
            var repository = new JpaRepositoryFactory(em).getRepository(UserRepository.class);
            assertEquals(2, repository.findAdminMembers("%", null, PageRequest.of(0, 20)).getTotalElements());
            var active = repository.findAdminMembers("%example.com%", UserStatus.ACTIVE, PageRequest.of(0, 20));
            assertEquals(1, active.getTotalElements());
            assertEquals("Member_One", active.getContent().getFirst().getDisplayName());
            assertEquals(1, repository.findAdminMembers("%!_%", null, PageRequest.of(0, 20)).getTotalElements());
            assertEquals(0, repository.findAdminMembers("%missing%", null, PageRequest.of(0, 20)).getTotalElements());
            em.getTransaction().rollback();
        }
    }
}
