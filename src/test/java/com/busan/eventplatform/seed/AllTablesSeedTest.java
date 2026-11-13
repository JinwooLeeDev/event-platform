package com.busan.eventplatform.seed;

import com.busan.eventplatform.company.CompanyStatus;
import com.busan.eventplatform.company.businessverification.BusinessVerificationStatus;
import com.busan.eventplatform.event.events.EventStatus;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import com.busan.eventplatform.user.UserStatus;
import com.busan.eventplatform.user.Role;
import com.busan.eventplatform.user.registration.RegistrationStatus;
import com.busan.eventplatform.api.payment.PaymentStatus;
import com.busan.eventplatform.api.refund.RefundStatus;
import jakarta.persistence.Table;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AllTablesSeedTest {
    @Test
    void executesAgainstCurrentJpaSchemaAndCoversStatesWithoutBrokenRelations() throws Exception {
        var configuration = new Configuration()
                .setPhysicalNamingStrategy(new PhysicalNamingStrategySnakeCaseImpl())
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:scenario-seed;MODE=MySQL;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        Set<String> tables = new HashSet<>();
        // 엔티티 추가 시 예시 데이터가 빠진 테이블도 검출합니다.
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (!Files.readString(path).contains("@Entity")) continue;
                String className = Path.of("src/main/java").relativize(path).toString()
                        .replace('\\', '.').replace('/', '.').replaceAll("\\.java$", "");
                Class<?> entity = Class.forName(className);
                configuration.addAnnotatedClass(entity);
                tables.add(entity.getAnnotation(Table.class).name());
            }
        }
        try (var factory = configuration.buildSessionFactory(); var session = factory.openSession()) {
            session.beginTransaction();
            session.doWork(connection -> {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("db/scenarios/all-tables-seed.sql"));
                assertEquals(13, tables.size());
                for (String table : tables) assertTrue(count(connection, "select count(*) from " + table) > 0, table);
                assertStates(connection,"users","status",UserStatus.values());
                assertStates(connection,"users","role",Role.values());
                assertStates(connection,"company_tb","status",CompanyStatus.values());
                assertStates(connection,"company_tb","business_verification_status",BusinessVerificationStatus.values());
                assertStates(connection,"events","status",EventStatus.values());
                assertStates(connection,"event_sessions","status",EventSessionStatus.values());
                assertStates(connection,"registrations","status",RegistrationStatus.values());
                assertStates(connection,"registration_history","changed_status",RegistrationStatus.values());
                for (String owner : Set.of("user_id", "company_id")) {
                    assertStates(connection,"payment_tb where " + owner + " is not null","status",PaymentStatus.values());
                    assertStates(connection,"refund_tb where " + owner + " is not null","status",RefundStatus.values());
                }
                assertEquals(0, count(connection,"""
                        select count(*) from event_sessions s where s.occupied_count !=
                        (select count(*) from registrations r where r.event_session_id=s.id and r.status in ('CONFIRMED','PENDING'))
                        or s.occupied_count > s.capacity or s.minimum_participants > s.capacity
                        or s.starts_at >= s.ends_at or s.minimum_check_at > s.starts_at
                        or s.next_reception_number <= coalesce((select max(r.reception_number) from registrations r where r.event_session_id=s.id),0)
                        """));
                assertEquals(0, count(connection,"""
                        select count(*) from registrations r where r.status='WAITING' and exists
                        (select 1 from event_sessions s where s.id=r.event_session_id and s.occupied_count<s.capacity)
                        """));
                assertEquals(0, count(connection,"""
                        select count(*) from (select user_id,event_session_id from registrations
                        where status in ('CONFIRMED','PENDING','WAITING') group by user_id,event_session_id having count(*)>1) d
                        """));
                assertEquals(0, count(connection,"""
                        select count(*) from (select event_session_id,reception_number from registrations
                        group by event_session_id,reception_number having count(*)>1) d
                        """));
                assertEquals(0, count(connection,"""
                        select count(*) from attendances a join registrations r on r.id=a.registration_id
                        join event_sessions s on s.id=r.event_session_id join events e on e.id=s.event_id
                        where r.status!='CONFIRMED' or a.attended_at<s.starts_at or a.attended_at>s.ends_at or a.checked_by!=e.company_id
                        """));
                assertEquals(0, count(connection,"""
                        select count(*) from registrations r where not exists
                        (select 1 from registration_history h where h.registration_id=r.id and h.changed_status=r.status)
                        """));
                for (String table : Set.of("payment_tb","purchase_tb","refund_tb")) {
                    assertEquals(0,count(connection,"select count(*) from " + table
                            + " where (user_id is null and company_id is null) or (user_id is not null and company_id is not null)"));
                }
                assertEquals(0, count(connection,"""
                        select count(*) from refund_tb f join purchase_tb p on p.id=f.purchase_id
                        where f.amount<0 or f.amount>p.price
                        or coalesce(f.user_id,-1)!=coalesce(p.user_id,-1) or coalesce(f.company_id,-1)!=coalesce(p.company_id,-1)
                        """));
                assertEquals(0, count(connection,"""
                        select count(*) from purchase_tb p where p.price<
                        (select coalesce(sum(f.amount),0) from refund_tb f where f.purchase_id=p.id)
                        """));
                assertEquals(0,count(connection,"select count(*) from events where apply_start_at>=apply_end_at or created_at>apply_start_at"));
                assertEquals(0,count(connection,"select count(*) from events e join company_tb c on c.id=e.company_id where c.status!='APPROVED'"));
                assertEquals(0,count(connection,"select count(*) from users where (status='WITHDRAWN' and withdrawn_at is null) or (status!='WITHDRAWN' and withdrawn_at is not null)"));
                assertTrue(count(connection,"select count(*) from users")>20);
                try (var statement=connection.createStatement(); var rows=statement.executeQuery("select password_hash from users where role='ADMIN'")) {
                    assertTrue(rows.next());
                    assertTrue(new BCryptPasswordEncoder().matches("1234",rows.getString(1)));
                }
            });
            session.getTransaction().rollback();
        }
    }

    private static int count(Connection connection, String sql) throws SQLException {
        try (var statement=connection.createStatement(); var result=statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private static void assertStates(Connection connection,String table,String column,Enum<?>[] values) throws SQLException {
        Set<String> actual=new HashSet<>();
        try (var statement=connection.createStatement();var result=statement.executeQuery("select distinct " + column + " from " + table)) {
            while (result.next()) actual.add(result.getString(1));
        }
        assertEquals(Arrays.stream(values).map(Enum::name).collect(Collectors.toSet()),actual,table+"."+column);
    }
}
