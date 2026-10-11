package com.courseregistration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class DatabaseHelper {

    private static final String FILE_PATH = "src/main/resources/data.json";

    // Changed to public so other managers can use the JSON tools
    public static final ObjectMapper mapper = new ObjectMapper();

    // 1. ONLY READS
    public static JsonNode getAllData() {
        try {
            return mapper.readTree(new File(FILE_PATH));
        } catch (IOException e) {
            System.out.println("CRITICAL ERROR: Cannot read data.json.");
            return mapper.createObjectNode();
        }
    }

    // 2. ONLY SAVES
    public static void saveData(JsonNode rootNode) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), rootNode);
            System.out.println("SUCCESS: Database saved.");
        } catch (IOException e) {
            System.out.println("CRITICAL ERROR: Cannot save to data.json.");
            e.printStackTrace();
        }
    }
}