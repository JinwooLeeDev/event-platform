//package com.busan.eventplatform.global.config;
//
//import com.zaxxer.hikari.HikariDataSource;
//import java.io.IOException;
//import java.sql.SQLException;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//import org.springframework.core.env.Environment;
//import org.springframework.core.env.Profiles;
//
//@Configuration(proxyBeanMethods = false)
//@Profile("mysql")
//public class LocalMysqlDataSourceConfiguration {
////    @Bean(destroyMethod = "close")
////    @ConfigurationProperties("spring.datasource.hikari")
////    HikariDataSource dataSource(Environment environment) throws SQLException, IOException {
////        if (environment.acceptsProfiles(Profiles.of("supabase", "h2"))) {
////            throw new IllegalStateException("Select only one database profile: mysql, h2, or supabase");
////        }
////        String url = environment.getRequiredProperty("spring.datasource.url");
////        String username = environment.getRequiredProperty("spring.datasource.username");
////        String password = environment.getRequiredProperty("spring.datasource.password");
////        LocalMysqlBootstrap.initialize(url, username, password);
////        var dataSource = new HikariDataSource();
////        dataSource.setJdbcUrl(url);
////        dataSource.setUsername(username);
////        dataSource.setPassword(password);
////        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
////        return dataSource;
////    }
//}
