package com.courseregistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CourseManager {

    // Handles the logic for a student dropping a course
    public static boolean dropCourse(String studentId, String courseId) {
        // Fetch the entire database
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode students = (ArrayNode) root.get("students");
        ArrayNode courses = (ArrayNode) root.get("courses");
        boolean studentUpdated = false;

        // Step 1: Locate the student and remove the course from their enrolled list
        for (JsonNode student : students) {
            if (student.get("studentId").asText().equals(studentId)) {
                ArrayNode enrolled = (ArrayNode) student.get("enrolledCourseIds");

                // Iterate backwards to safely remove items from the array without shifting index errors
                for (int i = enrolled.size() - 1; i >= 0; i--) {
                    if (enrolled.get(i).asText().equals(courseId)) {
                        enrolled.remove(i);
                        studentUpdated = true;
                        break; // Exit loop once the course is found and removed
                    }
                }
            }
        }

        // Step 2: If the student successfully dropped the course, restore 1 seat to the course capacity
        if (studentUpdated) {
            for (JsonNode course : courses) {
                if (course.get("courseId").asText().equals(courseId)) {
                    int currentSeats = course.get("availableSeats").asInt();
                    // Update the available seats by casting the JsonNode to an ObjectNode
                    ((ObjectNode) course).put("availableSeats", currentSeats + 1);
                    break;
                }
            }
            // Step 3: Permanently save the updated JSON data
            DatabaseHelper.saveData(root);
            return true;
        }
        return false; // Return false if the student or course was not found
    }

    // Handles the logic for a student registering for a course
    public static String registerCourse(String studentId, String courseId) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode courses = (ArrayNode) root.get("courses");
        ArrayNode students = (ArrayNode) root.get("students");

        boolean courseFound = false;
        boolean seatAvailable = false;

        // Step 1: Verify the course exists and has available seats
        for (JsonNode course : courses) {
            if (course.get("courseId").asText().equals(courseId)) {
                courseFound = true;
                int seats = course.get("availableSeats").asInt();

                if (seats > 0) {
                    seatAvailable = true;
                    // Deduct 1 seat to secure the registration
                    ((ObjectNode) course).put("availableSeats", seats - 1);
                }
                break;
            }
        }

        // Return early if registration cannot proceed
        if (!courseFound) return "CourseNotFound";
        // If the course is full, automatically route the student to the waitlist
        if (!seatAvailable) {
            boolean waitlisted = WaitlistManager.addToWaitlist(courseId, studentId);
            if (waitlisted) {
                return "Waitlisted";
            } else {
                return "AlreadyOnWaitlist";
            }
        }

        // Step 2: Locate the student and verify they aren't already enrolled
        boolean studentFound = false;
        for (JsonNode student : students) {
            if (student.get("studentId").asText().equals(studentId)) {
                studentFound = true;
                ArrayNode enrolled = (ArrayNode) student.get("enrolledCourseIds");

                // Prevent duplicate course registrations
                for (JsonNode enrolledCourse : enrolled) {
                    if (enrolledCourse.asText().equals(courseId)) {
                        return "AlreadyEnrolled";
                    }
                }
                // Add the course ID to the student's enrolled list
                enrolled.add(courseId);
                break;
            }
        }

        // Step 3: If the student does not exist in the database, create a new record
        if (!studentFound) {
            ObjectNode newStudent = DatabaseHelper.mapper.createObjectNode();
            newStudent.put("studentId", studentId);
            newStudent.put("name", "New Student"); // Default name assignment

            ArrayNode newEnrolled = DatabaseHelper.mapper.createArrayNode();
            newEnrolled.add(courseId); // Add the registered course
            newStudent.set("enrolledCourseIds", newEnrolled);

            students.add(newStudent);
        }

        // Step 4: Save all changes back to data.json
        DatabaseHelper.saveData(root);
        return "Success";
    }
}