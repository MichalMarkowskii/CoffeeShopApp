package org.example;

import java.sql.*;

// Klasa odpowiedzialna za zbudowanie połączenia pomiędzy mySQL, a Javą.
public class ConnectionBuilder {

    /**
     * Metoda buildConnection pozwala nam na zalogowanie użytkownika mySQL, poprzez wprowadzenie
     * parametrów do listy urlSB, jak na przykład hasło, nazwa bazy danych.
     *
     * @return zwraca zmienną conn, która przechowuje zalogowanego użytkownika.
     */
    public static Connection buildConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder urlSB = new StringBuilder("jdbc:mysql://"); //
        urlSB.append("localhost:3306/"); // numer portu
        urlSB.append("coffee?"); // nazwa bazy
        urlSB.append("useUnicode=true&characterEncoding=utf-8"); // kodowanie
        urlSB.append("&user=root"); // nazwa uzytkownika (root)
        urlSB.append("&password="); // haslo uzytkownika
        urlSB.append("&serverTimezone=CET"); // strefa czasowa (CET)
        String connectionUrl = urlSB.toString();
        try {
            Connection conn = DriverManager.getConnection(connectionUrl);
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}