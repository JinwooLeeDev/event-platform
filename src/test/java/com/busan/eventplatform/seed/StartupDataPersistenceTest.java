package com.busan.eventplatform.seed;

import com.busan.eventplatform.company.Company;
import com.busan.eventplatform.event.eventCategory.EventCategory;
import com.busan.eventplatform.user.User;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class StartupDataPersistenceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void everyStartupProfilePreservesTablesAndUsesOnlyMinimalDemoData() {
        for (String file : new String[]{"application.yml", "application-supabase.yml", "application-mysql.yml", "application-h2.yml"}) {
            Properties properties = yaml(file);
            assertEquals("update", properties.getProperty("spring.jpa.hibernate.ddl-auto"), file);
            if (!file.equals("application.yml")) {
                assertEquals("always", properties.getProperty("spring.sql.init.mode"), file);
                assertEquals("classpath:db/demo-data.sql", properties.getProperty("spring.sql.init.data-locations"), file);
            }
        }
        assertTrue(yaml("application-h2.yml").getProperty("spring.datasource.url").startsWith("jdbc:h2:file:"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ";MODE=PostgreSQL", ";MODE=MySQL"})
    void restartKeepsAccountsBalancesAndCategoriesWithoutDuplicatingDemoData(String mode) throws Exception {
        String url = "jdbc:h2:file:" + temporaryDirectory.resolve("persistent-db").toString().replace('\\', '/') + mode;
        var script = new FileSystemResource("src/main/resources/db/demo-data.sql");
        for (int startup = 0; startup < 2; startup++) {
            var configuration = new Configuration()
                    .addAnnotatedClass(User.class).addAnnotatedClass(Company.class).addAnnotatedClass(EventCategory.class)
                    .setPhysicalNamingStrategy(new PhysicalNamingStrategySnakeCaseImpl())
                    .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.connection.username", "sa")
                    .setProperty("hibernate.hbm2ddl.auto", "update");
            try (var factory = configuration.buildSessionFactory(); var session = factory.openSession()) {
                int iteration = startup;
                session.doWork(connection -> {
                    ScriptUtils.executeSqlScript(connection, script);
                    assertEquals(2, number(connection, "SELECT COUNT(*) FROM users"));
                    assertEquals(1, number(connection, "SELECT COUNT(*) FROM company_tb"));
                    assertEquals(iteration == 0 ? 4 : 5, number(connection, "SELECT COUNT(*) FROM event_category_tb"));
                    assertEquals(1, number(connection, "SELECT COUNT(*) FROM users WHERE role='ADMIN' AND status='ACTIVE'"));
                    assertEquals(1, number(connection, "SELECT COUNT(*) FROM users WHERE role='MEMBER' AND status='ACTIVE'"));
                    assertEquals(1, number(connection, "SELECT COUNT(*) FROM company_tb WHERE status='APPROVED' AND business_verification_status='MATCHED'"));
                    try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT password_hash FROM users UNION ALL SELECT password FROM company_tb")) {
                        while (rows.next()) assertTrue(new BCryptPasswordEncoder().matches("1234", rows.getString(1)));
                    }
                    if (iteration == 0) {
                        try (var statement = connection.createStatement()) {
                            statement.executeUpdate("UPDATE users SET point=777, display_name='changed-member' WHERE email_normalized='member@example.test'");
                            statement.executeUpdate("UPDATE company_tb SET point=888 WHERE email='company@example.test'");
                            statement.executeUpdate("INSERT INTO event_category_tb(name) VALUES ('custom-category')");
                        }
                    } else {
                        assertEquals(777, number(connection, "SELECT point FROM users WHERE display_name='changed-member'"));
                        assertEquals(888, number(connection, "SELECT point FROM company_tb WHERE email='company@example.test'"));
                        assertEquals(1, number(connection, "SELECT COUNT(*) FROM event_category_tb WHERE name='custom-category'"));
                    }
                    connection.commit();
                });
            }
        }
    }

    private static long number(Connection connection, String sql) throws java.sql.SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getLong(1);
        }
    }

    private static Properties yaml(String file) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(new FileSystemResource("src/main/resources/" + file));
        return factory.getObject();
    }
}
