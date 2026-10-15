
package edu.bsc.exam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("EXAM_DB_URL");
        String user = System.getenv("EXAM_DB_USER");
        String pass = System.getenv("EXAM_DB_PASSWORD");

        if (url == null || url.isBlank()
                || user == null || user.isBlank()
                || pass == null || pass.isBlank()) {
            throw new SQLException(
                "Database configuration is incomplete. Set EXAM_DB_URL, "
                + "EXAM_DB_USER, and EXAM_DB_PASSWORD."
            );
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL JDBC driver is not available to the application.", e
            );
        }

        return DriverManager.getConnection(url, user, pass);
    }
}
