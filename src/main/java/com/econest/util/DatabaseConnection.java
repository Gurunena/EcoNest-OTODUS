package com.econest.util;

import java.sql.Connection;
import java.sql.SQLException;

/** Central place to obtain JDBC connections for the DAO layer. */
public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        // TODO: add a JDBC driver to pom.xml and connect using AppConfig db.* settings
        throw new UnsupportedOperationException("Database not configured yet");
    }
}
