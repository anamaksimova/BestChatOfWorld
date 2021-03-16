package server;

import java.sql.*;

public class DataBaseAuthServise implements AuthService {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement psGetNickname;
    private static PreparedStatement psReg;
    private static PreparedStatement psChangeNickname;

    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:chatusers.db");
        statement=connection.createStatement();
    }
    public static void disconnect(){
        try {
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
     public static void prepareAllStatements() throws SQLException {
        psGetNickname = connection.prepareStatement("SELECT nickname FROM usersinfo WHERE login = ? and password = ?;");
        psReg = connection.prepareStatement("INSERT INTO usersinfo (login, password, nickname) VALUES (?, ?, ?) ");
        psChangeNickname = connection.prepareStatement("UPDATE usersinfo SET nickname=? WHERE nickname=?;");
     }
    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        String nickname = null;
        try {
            psGetNickname.setString(1, login);
            psGetNickname.setString(2, password);
            ResultSet rs = psGetNickname.executeQuery();

            while (rs.next()) {
                nickname = rs.getString("nickname");
            }
            rs.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return nickname;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {

        try {
            psReg.setString(1, login);
            psReg.setString(2,password);
            psReg.setString(3, nickname);
            psReg.executeUpdate();
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();

        }
        return false;
    }

    @Override
    public boolean changeNickname(String nickname, String newNickname) {
        try {
            psChangeNickname.setString(1, newNickname);
            psChangeNickname.setString(2, nickname);
            psChangeNickname.executeUpdate();
            return true;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }

    }
}
