package com.courseregistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class WaitlistManager {

    // 1. ADD TO WAITLIST: Appends student and timestamp, then calls Member 3's sorter
    public static boolean addToWaitlist(String courseId, String studentId) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode courses = (ArrayNode) root.get("courses");

        for (JsonNode course : courses) {
            if (course.get("courseId").asText().equals(courseId)) {

                // If the waitlist array is missing, create it
                if (!course.has("waitlist")) {
                    ((ObjectNode) course).set("waitlist", DatabaseHelper.mapper.createArrayNode());
                }

                ArrayNode waitlist = (ArrayNode) course.get("waitlist");

                // Prevent duplicate waitlist entries
                for (JsonNode entry : waitlist) {
                    if (entry.isObject() && entry.has("studentId") && entry.get("studentId").asText().equals(studentId)) {
                        return false;
                    }
                    // Legacy support if there are old strings in the JSON
                    if (entry.isTextual() && entry.asText().equals(studentId)) {
                        return false;
                    }
                }

                // Create the waitlist entry as an object with the exact current time
                ObjectNode newEntry = DatabaseHelper.mapper.createObjectNode();
                newEntry.put("studentId", studentId);
                newEntry.put("timestamp", System.currentTimeMillis());

                // Append the student to the array
                waitlist.add(newEntry);

                // ==========================================
                // HANDOFF: TRIGGER MEMBER 3'S INSERTION SORT
                // ==========================================
                Main.insertionSort(waitlist);

                // Save to the database AFTER Member 3's code sorts it
                DatabaseHelper.saveData(root);
                return true;
            }
        }
        return false;
    }

    // 2. LOAD QUEUE: Member 1 calls this to pull the sorted array into their Java Queue
    public static ArrayNode getWaitlistData(String courseId) {
        JsonNode root = DatabaseHelper.getAllData();
        ArrayNode courses = (ArrayNode) root.get("courses");

        for (JsonNode course : courses) {
            if (course.get("courseId").asText().equals(courseId)) {
                if (course.has("waitlist")) {
                    return (ArrayNode) course.get("waitlist");
                }
            }
        }
        return DatabaseHelper.mapper.createArrayNode();
    }
}