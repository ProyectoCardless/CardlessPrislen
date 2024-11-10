/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.observers;

import com.banco.CajerosCardless.models.LogEntry;
import java.io.FileWriter;
import java.io.IOException;

public class PositionalLogObserver implements LogObserver {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/log.pos";
    private static final String LOCAL_DIR = "src/main/resources/static/logs/log.pos";

    @Override
    public void logAction(LogEntry logEntry) {
        String logData = String.format("%-20s %-15s %-20s %-15s %-20s\n",
                logEntry.getTimestamp(),
                logEntry.getIpAddress(),
                logEntry.getOperatingSystem(),
                logEntry.getCountry(),
                logEntry.getAction());

        // Escribe el log en ambos archivos
        writeToFile(TEMP_DIR, logData);
        writeToFile(LOCAL_DIR, logData);
    }

    private void writeToFile(String filePath, String logData) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(logData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
