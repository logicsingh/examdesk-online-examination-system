
package edu.bsc.exam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault(
            "EXAM_DB_URL",
            "jdbc:mysql://localhost:3306/exam_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        );
        String user = System.getenv().getOrDefault("EXAM_DB_USER", "root");
        String pass = System.getenv().getOrDefault("EXAM_DB_PASSWORD", "");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver is not available to the application.", e);
        }

        return DriverManager.getConnection(url, user, pass);
    }
}
