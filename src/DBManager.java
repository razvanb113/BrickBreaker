import java.sql.*;

public class DBManager {
    private static final String URL = "jdbc:mysql://localhost:3306/brickbreaker";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Connection conn;

    public DBManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conectat la baza de date.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
