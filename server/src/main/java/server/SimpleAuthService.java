package server;

import java.sql.*;

public class SimpleAuthService implements AuthService {
    private static Connection connection;
    private static Statement stmt;

    SimpleAuthService(){
        try {
            connect();
            clearTable();

            for (int i = 0; i < 10; i++) {
                stmt.executeUpdate("INSERT INTO chatUsers (login, password, nickName) VALUES ('login"
                        + i + "', 'pass" + i + "', 'nick" + i + "')");
            }
            } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public String getNicknameByLoginAndPassword(String login, String password) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT nickName, login, password FROM chatUsers");
        while (rs.next()){
            if(rs.getString("login").equals(login) && rs.getString("password").equals(password))
                return rs.getString("nickName");
        }

        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        return false;
    }

    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        stmt = connection.createStatement();
    }

    private static void clearTable() throws SQLException{
        stmt.executeUpdate("DELETE FROM chatUsers");
    }

    public static void changeNickName(String login, String nickAfter) throws SQLException {
        stmt.executeUpdate("UPDATE chatUsers SET nickName='" + nickAfter + "' WHERE login='" + login + "';");
    }

    private static void disconnect() {
        try {
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
