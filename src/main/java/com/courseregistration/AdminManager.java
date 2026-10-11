package com.courseregistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AdminManager {

    // 1. ADD COURSE: Allows an admin to create a new course for the catalog
    public static String addCourse(String courseId, String name, int seats) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode courses = (ArrayNode) root.get("courses");

        // Prevent duplicate courses by checking if the ID already exists
        for (JsonNode course : courses) {
            if (course.get("courseId").asText().equals(courseId)) {
                return "CourseExists";
            }
        }

        // Create the new course JSON object
        ObjectNode newCourse = DatabaseHelper.mapper.createObjectNode();
        newCourse.put("courseId", courseId);
        newCourse.put("name", name);
        newCourse.put("availableSeats", seats);

        // NEW: Initialize an empty waitlist array instantly
        newCourse.set("waitlist", DatabaseHelper.mapper.createArrayNode());

        // Add it to the catalog and save
        courses.add(newCourse);
        DatabaseHelper.saveData(root);

        return "Success";
    }

    // 2. REMOVE COURSE: Deletes a course and cleans up all student records
    public static boolean removeCourse(String courseId) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode courses = (ArrayNode) root.get("courses");
        ArrayNode students = (ArrayNode) root.get("students");

        boolean courseRemoved = false;

        // Step A: Find the course in the catalog and delete it
        // We loop backwards (size - 1 down to 0) to prevent index shifting errors when deleting
        for (int i = courses.size() - 1; i >= 0; i--) {
            if (courses.get(i).get("courseId").asText().equals(courseId)) {
                courses.remove(i);
                courseRemoved = true;
                break;
            }
        }

        // If the course wasn't in the catalog, abort the process
        if (!courseRemoved) return false;

        // Step B: Search every single student and remove this course from their enrolled list
        for (JsonNode student : students) {
            ArrayNode enrolled = (ArrayNode) student.get("enrolledCourseIds");

            for (int i = enrolled.size() - 1; i >= 0; i--) {
                if (enrolled.get(i).asText().equals(courseId)) {
                    enrolled.remove(i);
                    break; // Move to the next student once removed
                }
            }
        }

        // Step C: Save the fully cleaned database
        DatabaseHelper.saveData(root);
        return true;
    }
}