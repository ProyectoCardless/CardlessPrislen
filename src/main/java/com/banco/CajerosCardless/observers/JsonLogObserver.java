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

    private final ObjectMapper objectMapper;

    // Constructor to receive the configured ObjectMapper
    public JsonLogObserver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void logAction(LogEntry logEntry) {
        try (FileWriter writer = new FileWriter("logs/log.json", true)) {
            writer.write(objectMapper.writeValueAsString(logEntry) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}