package com.courseregistration;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.javalin.http.staticfiles.Location;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Main {

    public static CourseQueue masterQueue = new CourseQueue(100);

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
            // Add the static files configuration
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(7070);

        System.out.println("Server is running! API is live at http://localhost:7070/api/data");

        // Redirects anyone visiting the base URL straight to the login page
        app.get("/", ctx -> ctx.redirect("/login.html"));

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
                ctx.status(200).json("Success: Course dropped and seats updated.");
            } else {
                ctx.status(400).json("Failed: Could not drop course. Check IDs.");
            }
        });

        // 3. CREATE Route (Used to register for a course)
        // Initialize the JSON mapper
        ObjectMapper mapper = new ObjectMapper();

        // Your specific POST route for student registration
        app.post("/api/register", ctx -> {
            try {
                // Parse the incoming JSON from register.html
                JsonNode body = mapper.readTree(ctx.body());
                String studentId = body.get("studentId").asText();
                String courseId = body.get("courseId").asText();

                // Capture exact time and create the Request object
                long currentTimestamp = System.currentTimeMillis();
                Request newRequest = new Request(studentId, courseId, currentTimestamp);

                // Send it to the queue you just built
                boolean isAdded = masterQueue.enqueue(newRequest);

                // Construct the JSON success/failure message to send back to the frontend
                ObjectNode response = mapper.createObjectNode();
                if (isAdded) {
                    response.put("success", true);
                    response.put("message", "Added to Queue successfully!");
                } else {
                    response.put("success", false);
                    response.put("message", "Failed: Queue is currently full.");
                }

                ctx.json(response);

            } catch (Exception e) {
                ObjectNode errorResponse = mapper.createObjectNode();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid Request format");
                ctx.status(400).json(errorResponse);
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

        // 9. STUDENT: Get Enrolled Courses Route
        app.get("/api/student/{id}/courses", ctx -> {
            // Extract the target student ID from the URL path (e.g., "STU002")
            String targetStudentId = ctx.pathParam("id");

            // Ask CourseManager to find this student's courses
            ArrayNode enrolledCourses = CourseManager.getStudentCourses(targetStudentId);

            if (enrolledCourses != null) {
                ctx.status(200).json(enrolledCourses);
            } else {
                ctx.status(404).json(java.util.Map.of("success", false, "message", "Student not found."));
            }
        });

        app.get("/api/courses", ctx -> {
            try {
                java.io.File databaseFile = new java.io.File("data.json");
                JsonNode rootNode = mapper.readTree(databaseFile);
                JsonNode coursesNode = rootNode.get("courses");

                ctx.contentType("application/json").json(coursesNode);

            } catch (Exception e) {
                System.out.println("Error reading database: " + e.getMessage());
                ctx.status(500).result("[]");
            }
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
        public String role;
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