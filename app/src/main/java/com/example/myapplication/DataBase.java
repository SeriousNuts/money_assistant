package com.example.myapplication;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DataBase {
    SQL_Connection sql_connection;
    Connection connection;
    ResultSet resultSet;
    Statement statement;

    public void add_user() {

    }

    public void remove_user() {

    }

    public boolean signIn_user(String Password, String Phone) throws SQLException {
        String query = "select Phone, Password from login_pass";
        String phone_check = "";
        String hashpw = "";
        try (Statement statement = connection.createStatement()) {
            connection = DriverManager.getConnection(sql_connection.getUrl(),sql_connection.getUserName(),sql_connection.getPassword());
            resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                phone_check = resultSet.getString("Phone");
                hashpw = resultSet.getString("Password");
            }

        } catch (SQLException sqlEx) {
            sqlEx.printStackTrace();
        }
        finally {
            //close connection ,stmt and resultset here
            try { connection.close(); } catch(SQLException se) { /*can't do anything */ }
            try { statement.close(); } catch(SQLException se) { /*can't do anything */ }
            try { resultSet.close(); } catch(SQLException se) { /*can't do anything */ }
        }
        if (Phone.equals(phone_check) && BCrypt.checkpw(Password.toString(),hashpw) )
            return true;
        else {
            return false;
        }
    }
}
