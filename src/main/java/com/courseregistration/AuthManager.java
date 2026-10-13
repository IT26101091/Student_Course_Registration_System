package com.courseregistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AuthManager {

    // Verifies user credentials and returns role information for frontend routing
    public static ObjectNode loginUser(String username, String password) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode users = (ArrayNode) root.get("users");
        ObjectNode response = DatabaseHelper.mapper.createObjectNode();

        // Loop through all users to find a matching username and password
        for (JsonNode user : users) {
            if (user.get("username").asText().equals(username) &&
                    user.get("password").asText().equals(password)) {

                response.put("success", true);
                response.put("role", user.get("role").asText()); // Identifies if user is "admin" or "student"

                // If a student logs in, attach their studentId so the frontend can use it for course registration requests
                if (user.has("studentId")) {
                    response.put("studentId", user.get("studentId").asText());
                }
                return response;
            }
        }

        // Return failure if no matching credentials are found
        response.put("success", false);
        response.put("message", "Invalid username or password");
        return response;
    }

    // Creates a new student account and default database record
    public static ObjectNode signupStudent(String username, String password, String name) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode users = (ArrayNode) root.get("users");
        ArrayNode students = (ArrayNode) root.get("students");
        ObjectNode response = DatabaseHelper.mapper.createObjectNode();

        // Step 1: Ensure the requested username is not already taken
        for (JsonNode user : users) {
            if (user.get("username").asText().equals(username)) {
                response.put("success", false);
                response.put("message", "Username already exists.");
                return response; // Abort signup
            }
        }

        // Step 2: Auto-generate a sequential student ID based on current database size (e.g., STU002, STU003)
        String newStudentId = "STU00" + (students.size() + 1);

        // Step 3: Create the authentication profile (Username, Password, Role)
        ObjectNode newUser = DatabaseHelper.mapper.createObjectNode();
        newUser.put("username", username);
        newUser.put("password", password);
        newUser.put("role", "student");
        newUser.put("studentId", newStudentId);
        users.add(newUser);

        // Step 4: Create the matching student database profile (Name, Enrolled Courses)
        ObjectNode newStudent = DatabaseHelper.mapper.createObjectNode();
        newStudent.put("studentId", newStudentId);
        newStudent.put("name", name);
        // Initialize an empty JSON array for future course registrations
        newStudent.set("enrolledCourseIds", DatabaseHelper.mapper.createArrayNode());
        students.add(newStudent);

        // Step 5: Save the new account and profile to the database
        DatabaseHelper.saveData(root);

        // Return success along with the newly generated ID
        response.put("success", true);
        response.put("studentId", newStudentId);
        return response;
    }
    // Updates a student's profile details (Name or Password)
    public static ObjectNode updateStudentProfile(String studentId, String newName, String newPassword) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode users = (ArrayNode) root.get("users");
        ArrayNode students = (ArrayNode) root.get("students");
        ObjectNode response = DatabaseHelper.mapper.createObjectNode();

        boolean updated = false;

        // Step 1: Update the name in the students database array
        for (JsonNode student : students) {
            if (student.get("studentId").asText().equals(studentId)) {
                if (newName != null && !newName.trim().isEmpty()) {
                    ((ObjectNode) student).put("name", newName.trim());
                    updated = true;
                }
                break;
            }
        }

        // Step 2: Update the password in the users authentication array (if provided)
        for (JsonNode user : users) {
            if (user.has("studentId") && user.get("studentId").asText().equals(studentId)) {
                if (newPassword != null && !newPassword.trim().isEmpty()) {
                    ((ObjectNode) user).put("password", newPassword);
                    updated = true;
                }
                break;
            }
        }

        if (updated) {
            DatabaseHelper.saveData(root);
            response.put("success", true);
            response.put("message", "Profile updated successfully.");
        } else {
            response.put("success", false);
            response.put("message", "Student not found or no changes made.");
        }

        return response;
    }
}