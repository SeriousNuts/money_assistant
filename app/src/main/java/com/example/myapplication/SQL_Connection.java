package com.example.myapplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQL_Connection {
    private String driver   = "com.mysql.jdbc.Driver";
    private String url      = "jdbc:mysql://localhost:3306/money_assistant";
    private String dbName   = "money_assistant";
    private String userName = "root";
    private String password = "";
    private Connection conn;

    public String getUrl(){
        return  url;
    }

    public String getUserName() {
        return userName;
    }
    public String getPassword(){
        return password;
    }
    public String getDbName(){
        return dbName;
    }

    public boolean tryConnection() {
        try {
            Class.forName(driver).newInstance();
            conn = DriverManager.getConnection(url + dbName, userName, password);
            if (conn != null) {
                System.out.println("Приложение подключилось к БД !");
                return true;
            }
            else {
                System.out.println("Приложение НЕ подключилось к БД ?");
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return false;
    }
    public void closeConnection()
    {
        if (conn != null){
            try {conn.close();}
            catch (SQLException ignore){};
        }
    }
    public Connection getConnection()
    {
        return conn;
    }
}
