package com.econest.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Central place to obtain JDBC connections for the DAO layer.
 *
 * @deprecated use {@link DBConnection} instead; this class only forwards to it
 *         and is kept so existing references keep compiling.
 */
@Deprecated
public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }
}
