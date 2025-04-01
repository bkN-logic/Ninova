import java.sql.*;

public class DataBaseHelper {
    private static Connection connection = null;
    private static final String URL = "*****";
    private static final String USERNAME = "****";
    private static final String PASSWORD = "***";

    public static String getUrl() {
        return URL;
    }

    // USERNAME ve PASSWORD için getter metodları da eklenebilir
    public static String getUsername() {
        return USERNAME;
    }

    public static String getPassword() {
        return PASSWORD;
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
            return connection;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
