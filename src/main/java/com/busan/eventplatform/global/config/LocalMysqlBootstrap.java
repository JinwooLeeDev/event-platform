//package com.busan.eventplatform.global.config;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.URLDecoder;
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
//import java.util.HashSet;
//import java.util.HexFormat;
//import java.util.Properties;
//import java.util.Set;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.core.io.support.EncodedResource;
//import org.springframework.jdbc.datasource.init.ScriptUtils;
//
///** Local-only, one-time bootstrap, completed before the application's DataSource exists. */
//final class LocalMysqlBootstrap {
//    private static final Logger log = LoggerFactory.getLogger(LocalMysqlBootstrap.class);
//    private static final String VERSION = "2026-11-02-v1";
//    private static final String MARKER = "_local_dev_bootstrap";
//    private static final Set<String> DATABASES = Set.of("event_platform", "event_platform_dev", "event_platform_test");
//    private static final Set<String> OPTIONS = Set.of("connectionTimeZone", "characterEncoding", "serverTimezone", "useSSL",
//            "allowPublicKeyRetrieval", "forceConnectionTimeZoneToSession");
//
//    record Target(String database, String serverUrl) { }
//    enum Result { INITIALIZED, SKIPPED }
//
//    static Target parseTarget(String url) {
//        if (url == null || !url.startsWith("jdbc:mysql://")) {
//            throw new IllegalArgumentException("Local bootstrap requires a jdbc:mysql:// URL");
//        }
//        URI uri = URI.create(url.substring(5));
//        if (!Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost() == null ? "" : uri.getHost())
//                || uri.getUserInfo() != null || uri.getFragment() != null || uri.getPort() == 0 || uri.getPort() > 65535) {
//            throw new IllegalArgumentException("Local bootstrap only allows loopback MySQL hosts without URL credentials");
//        }
//        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
//        if (!DATABASES.contains(database)) {
//            throw new IllegalArgumentException("Use event_platform, event_platform_dev, or event_platform_test for local development");
//        }
//        if (uri.getRawQuery() != null) {
//            for (String pair : uri.getRawQuery().split("&")) {
//                String key = URLDecoder.decode(pair.split("=", 2)[0], StandardCharsets.UTF_8);
//                if (!OPTIONS.contains(key)) {
//                    throw new IllegalArgumentException("Unsupported local MySQL URL option");
//                }
//            }
//        }
//        return new Target(database, "jdbc:mysql://" + uri.getRawAuthority() + "/"
//                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()));
//    }
//
//    static Result initialize(String url, String username, String password) throws SQLException, IOException {
//        Target target = parseTarget(url); // Always validate BEFORE opening any connection.
//        String schema = resource("01-schema.sql");
//        String seed = resource("02-seed.sql");
//        String fingerprint = fingerprint(schema, seed);
//        var credentials = new Properties();
//        credentials.setProperty("user", username);
//        credentials.setProperty("password", password);
//        credentials.setProperty("connectTimeout", "10000");
//        credentials.setProperty("socketTimeout", "60000");
//        try (Connection server = DriverManager.getConnection(target.serverUrl(), credentials)) {
//            if (server.getMetaData().getDatabaseMajorVersion() < 8
//                    || server.getMetaData().getDatabaseProductVersion().contains("MariaDB")) {
//                throw new IllegalStateException("Local bootstrap requires MySQL 8.0+");
//            }
//            execute(server, "CREATE DATABASE IF NOT EXISTS `" + target.database() + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_bin");
//        }
//        try (Connection connection = DriverManager.getConnection(url, credentials)) {
//            execute(connection, "SET SESSION time_zone = '+00:00'");
//            String lock = target.database() + ":local-bootstrap";
//            try (var statement = connection.prepareStatement("SELECT GET_LOCK(?, 30)")) {
//                statement.setString(1, lock);
//                try (var result = statement.executeQuery()) {
//                    if (!result.next() || result.getInt(1) != 1) {
//                        throw new IllegalStateException("Timed out waiting for the local database bootstrap lock");
//                    }
//                }
//            }
//            try {
//                return initializeLocked(connection, schema, seed, fingerprint);
//            } finally {
//                try (var statement = connection.prepareStatement("DO RELEASE_LOCK(?)")) {
//                    statement.setString(1, lock);
//                    statement.execute();
//                }
//            }
//        }
//    }
//
//    private static Result initializeLocked(Connection connection, String schema, String seed, String fingerprint) throws SQLException {
//        var tables = new HashSet<String>();
//        try (var statement = connection.createStatement(); var result = statement.executeQuery("SHOW TABLES")) {
//            while (result.next()) {
//                tables.add(result.getString(1));
//            }
//        }
//        if (!tables.isEmpty() && !tables.contains(MARKER)) {
//            throw new IllegalStateException("Existing local tables have no bootstrap marker; refusing to overwrite. Use a new event_platform_dev database.");
//        }
//        if (!tables.contains(MARKER)) {
//            execute(connection, "CREATE TABLE " + MARKER + " (id INT NOT NULL PRIMARY KEY, version VARCHAR(40) NOT NULL, "
//                    + "fingerprint CHAR(64) NOT NULL, status VARCHAR(20) NOT NULL, completed_at DATETIME(6) NULL) ENGINE=InnoDB");
//            try (var statement = connection.prepareStatement("INSERT INTO " + MARKER + " (id,version,fingerprint,status) VALUES (1,?,?,'PENDING')")) {
//                statement.setString(1, VERSION);
//                statement.setString(2, fingerprint);
//                statement.executeUpdate();
//            }
//        }
//        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT version,fingerprint,status FROM " + MARKER + " WHERE id=1")) {
//            if (!result.next()) {
//                throw new IllegalStateException("Local bootstrap metadata is incomplete; inspect the local database");
//            }
//            if (!VERSION.equals(result.getString(1)) || !fingerprint.equals(result.getString(2))) {
//                throw new IllegalStateException("Local bootstrap SQL version/hash changed; use a reviewed migration or a new local database");
//            }
//            if ("COMPLETE".equals(result.getString(3))) {
//                log.info("Local MySQL already initialized; skipping schema and dummy data");
//                return Result.SKIPPED;
//            }
//            if (!"PENDING".equals(result.getString(3))) {
//                throw new IllegalStateException("Unknown local bootstrap status");
//            }
//        }
//        // MySQL DDL auto-commits. The PENDING marker permits retry of the same SQL.
//        script(connection, schema);
//        connection.setAutoCommit(false);
//        try {
//            script(connection, seed);
//            execute(connection, "UPDATE " + MARKER + " SET status='COMPLETE',completed_at=UTC_TIMESTAMP(6) WHERE id=1");
//            connection.commit();
//        } catch (RuntimeException | SQLException exception) {
//            connection.rollback();
//            throw exception;
//        } finally {
//            connection.setAutoCommit(true);
//        }
//        log.info("Local MySQL initialized: 27 application tables and dummy data committed");
//        return Result.INITIALIZED;
//    }
//
//    private static void execute(Connection connection, String sql) throws SQLException {
//        try (var statement = connection.createStatement()) {
//            statement.execute(sql);
//        }
//    }
//
//    private static void script(Connection connection, String sql) {
//        ScriptUtils.executeSqlScript(connection, new EncodedResource(
//                new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
//    }
//
//    private static String resource(String filename) throws IOException {
//        // Normalize Git's Windows checkout line endings to match the Python initializer.
//        return new ClassPathResource("db/local-mysql/" + filename).getContentAsString(StandardCharsets.UTF_8)
//                .replace("\r\n", "\n").replace("\r", "\n");
//    }
//
//    private static String fingerprint(String schema, String seed) {
//        try {
//            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
//                    .digest((schema + "\n" + seed).getBytes(StandardCharsets.UTF_8)));
//        } catch (NoSuchAlgorithmException exception) {
//            throw new IllegalStateException(exception);
//        }
//    }
//}
