import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserAccount {
    private final String accountNumber;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/atm_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "lohit123";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // still valid for 8.3.0
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found.");
        }
    }

    public UserAccount(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public static boolean authenticate(String acc, String pin) throws SQLException {
        String query = "SELECT 1 FROM users WHERE account_number = ? AND pin = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, acc);
            stmt.setString(2, pin);
            return stmt.executeQuery().next();
        }
    }

    public double getBalanceFromDB() throws SQLException {
        String query = "SELECT balance FROM users WHERE account_number = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
            throw new SQLException("Account not found.");
        }
    }

    public void depositToDB(double amount) throws SQLException {
        String updateQuery = "UPDATE users SET balance = balance + ? WHERE account_number = ?";
        String insertQuery = "INSERT INTO transactions (account_number, type, amount, timestamp) VALUES (?, 'Deposit', ?, NOW())";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt1 = conn.prepareStatement(updateQuery);
                 PreparedStatement stmt2 = conn.prepareStatement(insertQuery)) {

                stmt1.setDouble(1, amount);
                stmt1.setString(2, accountNumber);
                stmt1.executeUpdate();

                stmt2.setString(1, accountNumber);
                stmt2.setDouble(2, amount);
                stmt2.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean withdrawFromDB(double amount) throws SQLException {
        String selectQuery = "SELECT balance FROM users WHERE account_number = ?";
        String updateQuery = "UPDATE users SET balance = balance - ? WHERE account_number = ?";
        String insertQuery = "INSERT INTO transactions (account_number, type, amount, timestamp) VALUES (?, 'Withdraw', ?, NOW())";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
                 PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {

                selectStmt.setString(1, accountNumber);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next() || rs.getDouble("balance") < amount) {
                    conn.rollback();
                    return false;
                }

                updateStmt.setDouble(1, amount);
                updateStmt.setString(2, accountNumber);
                updateStmt.executeUpdate();

                insertStmt.setString(1, accountNumber);
                insertStmt.setDouble(2, amount);
                insertStmt.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void changePin(String newPin) throws SQLException {
        String query = "UPDATE users SET pin = ? WHERE account_number = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newPin);
            stmt.setString(2, accountNumber);
            stmt.executeUpdate();
        }
    }

    public List<String> getTransactionHistory() throws SQLException {
        List<String> history = new ArrayList<>();
        String query = "SELECT type, amount, timestamp FROM transactions WHERE account_number = ? ORDER BY timestamp DESC LIMIT 10";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(rs.getString("timestamp") + " - " +
                        rs.getString("type") + ": $" + rs.getDouble("amount"));
            }
        }
        return history;
    }
}
