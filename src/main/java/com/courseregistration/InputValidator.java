package com.courseregistration;

public class InputValidator {

    // Checks if a string is null or only contains blank spaces
    public static boolean isValidText(String input) {
        return input != null && !input.trim().isEmpty();
    }

    // Enforces a minimum password length for basic security
    public static boolean isValidPassword(String password) {
        return isValidText(password) && password.length() >= 6;
    }

    // Ensures course seats cannot be a negative number
    public static boolean isValidSeats(int seats) {
        return seats >= 0;
    }
}