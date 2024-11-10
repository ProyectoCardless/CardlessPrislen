/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.services;
import com.banco.CajerosCardless.models.LogEntry;
import com.banco.CajerosCardless.observers.LogObserver;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserActionLogger {

    private final List<LogObserver> observers = new ArrayList<>();

    public void addObserver(LogObserver observer) {
        observers.add(observer);
    }

    public void logAction(String ipAddress, String operatingSystem, String country, String action) {
        LogEntry logEntry = new LogEntry(LocalDateTime.now(), ipAddress, operatingSystem, country, action);
        observers.forEach(observer -> observer.logAction(logEntry));
    }
}