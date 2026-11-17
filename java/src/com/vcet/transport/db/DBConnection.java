package com.vcet.transport.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Creates JDBC connections. Reads db.properties; environment variables override it. */
public final class DBConnection {
    private static final Properties PROPS = new Properties();

    static {
        try (FileInputStream in = new FileInputStream("db.properties")) {
            PROPS.load(in);
        } catch (IOException e) {
            // fall back to defaults below
        }
    }

    private DBConnection() { }

    private static String get(String env, String key, String def) {
        String v = System.getenv(env);
        return (v != null && !v.isBlank()) ? v : PROPS.getProperty(key, def);
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                get("DB_URL", "db.url", "jdbc:mariadb://127.0.0.1:3306/transport_management?allowPublicKeyRetrieval=true"),
                get("DB_USER", "db.user", "transport_app"),
                get("DB_PASSWORD", "db.password", "transport123"));
    }
}