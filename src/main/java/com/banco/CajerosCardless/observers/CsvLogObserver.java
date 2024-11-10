/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.observers;

import com.banco.CajerosCardless.models.LogEntry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CsvLogObserver implements LogObserver {
    private static final String TEMP_FILE_PATH = System.getProperty("java.io.tmpdir") + "/log.csv";
    private static final String LOCAL_FILE_PATH = "src/main/resources/static/logs/log.csv";
    private final PrintWriter tempWriter;
    private final PrintWriter localWriter;

    public CsvLogObserver() {
        tempWriter = initializeWriter(TEMP_FILE_PATH);
        localWriter = initializeWriter(LOCAL_FILE_PATH);
    }

    private PrintWriter initializeWriter(String filePath) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();

            boolean fileExists = file.exists();
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));

            if (!fileExists || file.length() == 0) {
                writer.println("timestamp,ipAddress,operatingSystem,country,action");
                writer.flush();
            }
            return writer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CSV log writer for " + filePath, e);
        }
    }

    @Override
    public synchronized void logAction(LogEntry logEntry) {
        String logData = String.format("%s,%s,%s,%s,%s%n",
                logEntry.getTimestamp(),
                logEntry.getIpAddress(),
                logEntry.getOperatingSystem(),
                logEntry.getCountry(),
                logEntry.getAction());

        tempWriter.write(logData);
        localWriter.write(logData);

        tempWriter.flush();
        localWriter.flush();
    }

    public void closeWriters() {
        if (tempWriter != null) {
            tempWriter.close();
        }
        if (localWriter != null) {
            localWriter.close();
        }
    }
}
