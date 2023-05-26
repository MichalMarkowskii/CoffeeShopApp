package org.example;

//Klasa przechowująca specjalny klucz, który pozwala zarejestrować się jako administrator
public class AdminAccess {

    private String key;

    public AdminAccess() {
        this.key = "999aa";
    }

    public String getKey() {
        return key;
    }
}
