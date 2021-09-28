package diff.cache;

import static diff.utils.Log.errors;
import static diff.utils.Log.infos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class H2CacheAdapter {

    // TODO
    private static final String JDBC_DRIVER = "org.h2.Driver";
    private static final String DB_URL = "jdbc:h2:~/test";
    private static final String USER = "sa";
    private static final String PASS = "";
    private static Connection conn;
    private static Statement stmt;

    static {
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void exit() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException se2) {
        } // nothing we can do
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    public static ResultSet query(String sql) {
        infos("即将执行SQL" + sql);
        try {
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            errors("无法执行SQL" + sql + " err=" + e.getMessage());
        }
        return null;
    }


    public static void main(String[] args) {
        ResultSet resultSet = H2CacheAdapter.query("select * from REGISTRATION");
    }

}
