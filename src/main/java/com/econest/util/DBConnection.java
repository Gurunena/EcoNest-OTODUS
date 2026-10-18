package com.econest.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Shared JDBC entry point for every DAO in the project.
 *
 * <p>Settings are read once from {@code db.properties}, which is looked up in the
 * project root first and then on the classpath. The file is not in version
 * control; copy {@code db.properties.example} to {@code db.properties} and fill
 * in your own MySQL credentials.
 *
 * <p>Typical use in a DAO:
 * <pre>
 * try (Connection conn = DBConnection.getConnection();
 *      PreparedStatement ps = conn.prepareStatement(SQL)) {
 *     ...
 * }
 * </pre>
 * Each call returns a new connection, so always close it (try-with-resources).
 */
public final class DBConnection {

    /** Name of the credentials file, in the project root or on the classpath. */
    private static final String PROPERTIES_FILE = "db.properties";

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private static final Properties CONFIG = loadConfig();

    private DBConnection() {
    }

    /**
     * Opens a new connection to the EcoNest database.
     *
     * @return an open JDBC connection the caller is responsible for closing
     * @throws SQLException if the driver is missing or the connection fails
     */
    public static Connection getConnection() throws SQLException {
        String url = required("url");
        String user = required("user");
        String password = CONFIG.getProperty("password", "");

        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver (" + DRIVER_CLASS + ") not found on the classpath. "
                            + "Make sure the mysql-connector-j dependency is in pom.xml and the "
                            + "Maven dependencies have been downloaded.", e);
        }

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new SQLException(
                    "Could not connect to the EcoNest database at " + url + " as user '" + user
                            + "'. Check that the MySQL server is running, that the 'econest' "
                            + "database exists (run database/schema.sql), and that the user and "
                            + "password in " + PROPERTIES_FILE + " are correct. Cause: "
                            + e.getMessage(), e);
        }
    }

    /** Reads db.properties from the project root, falling back to the classpath. */
    private static Properties loadConfig() {
        Properties props = new Properties();
        Path localFile = Path.of(PROPERTIES_FILE);

        if (Files.isRegularFile(localFile)) {
            try (InputStream in = Files.newInputStream(localFile)) {
                props.load(in);
                return props;
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Could not read " + localFile.toAbsolutePath() + ": " + e.getMessage(), e);
            }
        }

        try (InputStream in = DBConnection.class.getResourceAsStream("/" + PROPERTIES_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        PROPERTIES_FILE + " not found. Copy db.properties.example to "
                                + PROPERTIES_FILE + " in the project root ("
                                + Path.of("").toAbsolutePath() + ") and set url, user and password.");
            }
            props.load(in);
            return props;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read " + PROPERTIES_FILE + " from the classpath: " + e.getMessage(), e);
        }
    }

    private static String required(String key) throws SQLException {
        String value = CONFIG.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SQLException(
                    "Missing '" + key + "' in " + PROPERTIES_FILE
                            + ". See db.properties.example for the expected keys.");
        }
        return value.trim();
    }

    /**
     * Quick connection check. Run it with:
     * <pre>mvn -q compile exec:java -Dexec.mainClass=com.econest.util.DBConnection</pre>
     * or straight from the IDE.
     */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Connected to: " + conn.getMetaData().getURL());
            System.out.println("Server:       " + conn.getMetaData().getDatabaseProductVersion());
            System.out.println("Catalog:      " + conn.getCatalog());
            System.out.println("Connection OK");
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Connection FAILED: " + e.getMessage());
        }
    }
}
