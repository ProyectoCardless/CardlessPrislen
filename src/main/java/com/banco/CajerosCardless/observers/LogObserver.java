package com.banco.CajerosCardless.observers;

import com.banco.CajerosCardless.models.LogEntry;

public interface LogObserver {
    void logAction(LogEntry logEntry);
}
