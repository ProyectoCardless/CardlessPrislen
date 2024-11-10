/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.observers;

import com.banco.CajerosCardless.models.LogEntry;
import java.io.FileWriter;
import java.io.IOException;

public class PositionalLogObserver implements LogObserver {

    @Override
    public void logAction(LogEntry logEntry) {
        try (FileWriter writer = new FileWriter("logs/log.pos", true)) {
            writer.write(String.format("%-20s %-15s %-20s %-15s %-20s\n",
                    logEntry.getTimestamp(),
                    logEntry.getIpAddress(),
                    logEntry.getOperatingSystem(),
                    logEntry.getCountry(),
                    logEntry.getAction()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
