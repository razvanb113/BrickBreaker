import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/brickbreaker";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    private static String currentUser = null;
    private static final Map<Integer, Integer> localScores = new HashMap<>();
    private static final String USER_CACHE_FILE = "user.cache";
    public static void init() {
        try {
            Path tokenPath = java.nio.file.Path.of(USER_CACHE_FILE);

            if (!Files.exists(tokenPath)) return;

            String token = Files.readString(tokenPath).trim();
            if (token.isEmpty()) return;

            String[] parts = token.split(":");
            if (parts.length != 3) return;

            String expectedUserHash = parts[0];
            String expectedEmailHash = parts[1];
            String expectedPassHash = parts[2];

            String sql = "SELECT username, email FROM users WHERE password = ?";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, expectedPassHash);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String username = rs.getString("username");
                    String email = rs.getString("email");

                    String actualUserHash = hash(username);
                    String actualEmailHash = hash(email);

                    if (expectedUserHash.equals(actualUserHash) &&
                            expectedEmailHash.equals(actualEmailHash)) {

                        currentUser = username;
                        Session.loggedUsername = username;
                        System.out.println("Utilizator conectat automat: " + currentUser);
                        return;
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Eroare la citirea utilizatorului din cache.");
            e.printStackTrace();
        }
    }

    private static String hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static String register(String username, String email, String password) {
        String hashed = hashPassword(password);
        String sql = "INSERT INTO users(username, email, password) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashed);
            stmt.executeUpdate();
            return "Cont creat cu succes.";

        } catch (SQLException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("duplicate") || msg.contains("unique")) {
                if (msg.contains("username")) {
                    return "Username deja existent.";
                } else if (msg.contains("email")) {
                    return "Există deja un cont cu acest email.";
                } else {
                    return "Datele introduse sunt deja folosite.";
                }
            } else {
                e.printStackTrace();
                return "Eroare la creare cont.";
            }
        }
    }


    public static String login(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) return "Utilizator inexistent.";

            String storedHash = rs.getString("password");
            String inputHash = hashPassword(password);

            if (!storedHash.equals(inputHash)) return "Parolă greșită.";

            currentUser = username;
            saveUserToCache(username);
            for (Map.Entry<Integer, Integer> entry : localScores.entrySet()) {
                saveScore(username, entry.getKey(), entry.getValue());
            }
            localScores.clear();
            return "Autentificat cu succes.";

        } catch (SQLException e) {
            e.printStackTrace();
            return "Eroare la conectare.";
        }
    }


    public static void saveScore(String username, int level, int score) {

        String getUserIdSql = "SELECT id FROM users WHERE username = ?";
        String selectScoreSql = "SELECT score FROM scores WHERE user_id = ? AND level = ?";
        String insertScoreSql = "INSERT INTO scores(user_id, level, score) VALUES (?, ?, ?)";
        String updateScoreSql = "UPDATE scores SET score = ? WHERE user_id = ? AND level = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement userStmt = conn.prepareStatement(getUserIdSql)) {

            userStmt.setString(1, username);
            ResultSet rs = userStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");

                try (PreparedStatement checkStmt = conn.prepareStatement(selectScoreSql)) {
                    checkStmt.setInt(1, userId);
                    checkStmt.setInt(2, level);
                    ResultSet scoreRs = checkStmt.executeQuery();

                    if (scoreRs.next()) {
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateScoreSql)) {
                            updateStmt.setInt(1, score);
                            updateStmt.setInt(2, userId);
                            updateStmt.setInt(3, level);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertScoreSql)) {
                            insertStmt.setInt(1, userId);
                            insertStmt.setInt(2, level);
                            insertStmt.setInt(3, score);
                            insertStmt.executeUpdate();
                        }
                    }
                }

            } else {
                System.out.println("Utilizatorul nu există.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int getLastLevel(String username) {
        String sql = """
            SELECT MAX(level) as last_level FROM scores
            JOIN users ON scores.user_id = users.id
            WHERE users.username = ?
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("last_level") : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static Map<Integer, Integer> getScoresByLevel(String username) {
        Map<Integer, Integer> scores = new HashMap<>();

        String sql = """
            SELECT level, score FROM scores
            JOIN users ON scores.user_id = users.id
            WHERE users.username = ?
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                scores.put(rs.getInt("level"), rs.getInt("score"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return scores;
    }

    public static String getAccountSummary(String username) {
        int lastLevel = getLastLevel(username);
        int totalScore = getScoresByLevel(username).values().stream().mapToInt(Integer::intValue).sum();
        return "<html>User: " + username + "<br>Ultimul nivel completat: " + lastLevel + "<br>Scor total: " + totalScore + "</html>";
    }


    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveUserToCache(String username) {
        String sql = "SELECT email, password FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String email = rs.getString("email");
                String passwordHash = rs.getString("password");

                String token = hash(username) + ":" + hash(email) + ":" + passwordHash;
                java.nio.file.Files.writeString(java.nio.file.Path.of(USER_CACHE_FILE), token);
            }

        } catch (Exception e) {
            System.out.println("Eroare la salvarea tokenului în cache.");
            e.printStackTrace();
        }
    }


    private static void clearUserCache() {
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(USER_CACHE_FILE));
        } catch (Exception e) {
            System.out.println("Eroare la ștergerea cache-ului.");
            e.printStackTrace();
        }
    }

    public static void logout() {
        currentUser = null;
        clearUserCache();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }


}
