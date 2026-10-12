package com.courseregistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CourseManager {

    // 1. Handles the logic for a student dropping a course
    public static boolean dropCourse(String studentId, String courseId) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode students = (ArrayNode) root.get("students");
        ArrayNode courses = (ArrayNode) root.get("courses");
        boolean studentUpdated = false;

        for (JsonNode student : students) {
            if (student.get("studentId").asText().equals(studentId)) {
                ArrayNode enrolled = (ArrayNode) student.get("enrolledCourseIds");

                for (int i = enrolled.size() - 1; i >= 0; i--) {
                    if (enrolled.get(i).asText().equals(courseId)) {
                        enrolled.remove(i);
                        studentUpdated = true;
                        break;
                    }
                }
            }
        }

        if (studentUpdated) {
            for (JsonNode course : courses) {
                if (course.get("courseId").asText().equals(courseId)) {
                    int currentSeats = course.get("availableSeats").asInt();
                    ((ObjectNode) course).put("availableSeats", currentSeats + 1);
                    break;
                }
            }
            DatabaseHelper.saveData(root);
            return true;
        }
        return false;
    }

    // 2. Handles the logic for a student registering for a course
    public static String registerCourse(String studentId, String courseId) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode courses = (ArrayNode) root.get("courses");
        ArrayNode students = (ArrayNode) root.get("students");

        boolean courseFound = false;
        boolean seatAvailable = false;

        for (JsonNode course : courses) {
            if (course.get("courseId").asText().equals(courseId)) {
                courseFound = true;
                int seats = course.get("availableSeats").asInt();

                if (seats > 0) {
                    seatAvailable = true;
                    ((ObjectNode) course).put("availableSeats", seats - 1);
                }
                break;
            }
        }

        if (!courseFound) return "CourseNotFound";

        if (!seatAvailable) {
            boolean waitlisted = WaitlistManager.addToWaitlist(courseId, studentId);
            if (waitlisted) {
                return "Waitlisted";
            } else {
                return "AlreadyOnWaitlist";
            }
        }

        boolean studentFound = false;
        for (JsonNode student : students) {
            if (student.get("studentId").asText().equals(studentId)) {
                studentFound = true;
                ArrayNode enrolled = (ArrayNode) student.get("enrolledCourseIds");

                for (JsonNode enrolledCourse : enrolled) {
                    if (enrolledCourse.asText().equals(courseId)) {
                        return "AlreadyEnrolled";
                    }
                }
                enrolled.add(courseId);
                break;
            }
        }

        if (!studentFound) {
            ObjectNode newStudent = DatabaseHelper.mapper.createObjectNode();
            newStudent.put("studentId", studentId);
            newStudent.put("name", "New Student");

            ArrayNode newEnrolled = DatabaseHelper.mapper.createArrayNode();
            newEnrolled.add(courseId);
            newStudent.set("enrolledCourseIds", newEnrolled);

            students.add(newStudent);
        }

        DatabaseHelper.saveData(root);
        return "Success";
    }

    // 3. NEW: Retrieves a student's enrolled courses to display on the frontend dashboard
    public static ArrayNode getStudentCourses(String studentId) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode students = (ArrayNode) root.get("students");
        ArrayNode courses = (ArrayNode) root.get("courses");
        ArrayNode frontendCourseList = DatabaseHelper.mapper.createArrayNode();

        JsonNode targetStudent = null;
        for (JsonNode student : students) {
            if (student.get("studentId").asText().equals(studentId)) {
                targetStudent = student;
                break;
            }
        }

        if (targetStudent == null) {
            return null;
        }

        ArrayNode enrolledIds = (ArrayNode) targetStudent.get("enrolledCourseIds");

        if (enrolledIds != null) {
            for (JsonNode enrolledIdNode : enrolledIds) {
                String currentEnrolledId = enrolledIdNode.asText();

                for (JsonNode course : courses) {
                    if (course.get("courseId").asText().equals(currentEnrolledId)) {
                        ObjectNode courseData = DatabaseHelper.mapper.createObjectNode();
                        courseData.put("courseId", course.get("courseId").asText());
                        courseData.put("name", course.get("name").asText());

                        frontendCourseList.add(courseData);
                        break;
                    }
                }
            }
        }

        return frontendCourseList;
    }
}