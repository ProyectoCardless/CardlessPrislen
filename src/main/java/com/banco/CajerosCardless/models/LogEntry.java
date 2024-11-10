package com.banco.CajerosCardless.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class LogEntry {
    private LocalDateTime timestamp;
    private String ipAddress;
    private String operatingSystem;
    private String country;
    private String action;
}
