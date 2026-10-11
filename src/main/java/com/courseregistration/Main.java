package com.courseregistration;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class Main {
    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(7070);

        System.out.println("Server is running! API is live at http://localhost:7070/api/data");

        // 1. READ Route (Used to display data)
        app.get("/api/data", ctx -> {
            ctx.json(DatabaseHelper.getAllData());
        });

        // 2. UPDATE/DELETE Route (Used to drop a course)
        app.post("/api/course/drop", ctx -> {
            // Javalin translates the incoming frontend JSON into our DropRequest class
            DropRequest request = ctx.bodyAsClass(DropRequest.class);

            // Send the IDs to your DatabaseHelper logic
            boolean success = CourseManager.dropCourse(request.studentId, request.courseId);

            // Tell the frontend if it worked or failed
            if (success) {
                ctx.status(200).result("Success: Course dropped and seats updated.");
            } else {
                ctx.status(400).result("Failed: Could not drop course. Check IDs.");
            }
        });

        // 3. CREATE Route (Used to register for a course)
        app.post("/api/course/register", ctx -> {
            DropRequest request = ctx.bodyAsClass(DropRequest.class);
            String result = CourseManager.registerCourse(request.studentId, request.courseId);

            // Reply to Member 1's frontend based on the new waitlist logic
            if (result.equals("Success")) {
                ctx.status(200).result("Success: Registered for course.");
            } else if (result.equals("Waitlisted")) {
                ctx.status(200).result("Notice: Course full. Added to waitlist."); // 200 OK because the action succeeded
            } else if (result.equals("AlreadyOnWaitlist")) {
                ctx.status(400).result("Failed: Student is already on the waitlist for this course.");
            } else if (result.equals("AlreadyEnrolled")) {
                ctx.status(400).result("Failed: Student is already enrolled in this course.");
            } else {
                ctx.status(404).result("Failed: Course not found.");
            }
        });

        // 4. LOGIN Route
        app.post("/api/login", ctx -> {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            ObjectNode result = AuthManager.loginUser(request.username, request.password);

            if (result.get("success").asBoolean()) {
                ctx.status(200).json(result);
            } else {
                ctx.status(401).json(result); // 401 means Unauthorized
            }
        });

        // 5. SIGNUP Route
        app.post("/api/signup", ctx -> {
            SignupRequest request = ctx.bodyAsClass(SignupRequest.class);

            // INPUT VALIDATION BLOCK (Add this before calling AuthManager)
            if (!InputValidator.isValidText(request.username) || !InputValidator.isValidText(request.name)) {
                ctx.status(400).result("Validation Failed: Username and Name are required.");
                return;
            }
            if (!InputValidator.isValidPassword(request.password)) {
                ctx.status(400).result("Validation Failed: Password must be at least 6 characters long.");
                return;
            }

            // Proceed to AuthManager if validation passes
            ObjectNode result = AuthManager.signupStudent(
                    request.username.trim(),
                    request.password,
                    request.name.trim()
            );

            if (result.get("success").asBoolean()) {
                ctx.status(201).json(result); // 201 means Created
            } else {
                ctx.status(400).json(result);
            }
        });
        // 6. ADMIN: Add Course Route (With Validation)
        app.post("/api/admin/add-course", ctx -> {
            AddCourseRequest request = ctx.bodyAsClass(AddCourseRequest.class);

            // INPUT VALIDATION BLOCK
            if (!InputValidator.isValidText(request.courseId) || !InputValidator.isValidText(request.name)) {
                ctx.status(400).result("Validation Failed: Course ID and Name cannot be empty.");
                return; // Abort the request
            }
            if (!InputValidator.isValidSeats(request.seats)) {
                ctx.status(400).result("Validation Failed: Course seats cannot be negative.");
                return; // Abort the request
            }

            // Only proceed to the Manager if validation passes
            String result = AdminManager.addCourse(request.courseId.trim(), request.name.trim(), request.seats);

            if (result.equals("Success")) {
                ctx.status(201).result("Success: Course added to catalog.");
            } else {
                ctx.status(400).result("Failed: Course ID already exists.");
            }
        });

        // 7. ADMIN: Remove Course Route
        app.post("/api/admin/remove-course", ctx -> {
            RemoveCourseRequest request = ctx.bodyAsClass(RemoveCourseRequest.class);
            boolean success = AdminManager.removeCourse(request.courseId);

            if (success) {
                ctx.status(200).result("Success: Course permanently deleted and student records cleaned.");
            } else {
                ctx.status(404).result("Failed: Course not found in catalog.");
            }
        });

        // 8. ADMIN / QUEUE: Get the sorted waitlist for a specific course
        app.post("/api/course/waitlist", ctx -> {
            WaitlistRequest request = ctx.bodyAsClass(WaitlistRequest.class);
            ArrayNode waitlist = WaitlistManager.getWaitlistData(request.courseId);
            ctx.status(200).json(waitlist);
        });
    }

    // A tiny helper class just to catch the incoming data from the frontend
    public static class DropRequest {
        public String studentId;
        public String courseId;
    }
    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class SignupRequest {
        public String username;
        public String password;
        public String name;
    }

    public static class AddCourseRequest {
        public String courseId;
        public String name;
        public int seats;
    }

    public static class RemoveCourseRequest {
        public String courseId;
    }

    // Helper class for Route 8 (Fetching the waitlist array)
    public static class WaitlistRequest {
        public String courseId;
    }


}