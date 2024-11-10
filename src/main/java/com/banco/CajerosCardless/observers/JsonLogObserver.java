/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.observers;
import com.banco.CajerosCardless.models.LogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileWriter;
import java.io.IOException;

public class JsonLogObserver implements LogObserver {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/log.json";
    private static final String LOCAL_DIR = "src/main/resources/static/logs/log.json";

    private final ObjectMapper objectMapper;

    public JsonLogObserver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void logAction(LogEntry logEntry) {
        try {
            String logData = objectMapper.writeValueAsString(logEntry) + "\n";
            writeToFile(TEMP_DIR, logData);
            writeToFile(LOCAL_DIR, logData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(String filePath, String logData) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(logData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
