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

    private static final String FILE_PATH = "logs/log.csv";
    private PrintWriter writer;

    public CsvLogObserver() {
        initializeCsvFile();
    }

    // Inicializa el archivo CSV y agrega encabezados si el archivo está vacío
    private synchronized void initializeCsvFile() {
        try {
            // Crea el directorio "logs" si no existe
            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();

            boolean fileExists = file.exists();
            this.writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));

            // Agrega encabezados solo si el archivo está vacío
            if (!fileExists || file.length() == 0) {
                writer.println("timestamp,ipAddress,operatingSystem,country,action");
                writer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CSV log writer", e);
        }
    }

    @Override
    public synchronized void logAction(LogEntry logEntry) {
        writer.printf("%s,%s,%s,%s,%s%n",
                logEntry.getTimestamp(),
                logEntry.getIpAddress(),
                logEntry.getOperatingSystem(),
                logEntry.getCountry(),
                logEntry.getAction());
        writer.flush(); // Asegura que los datos se escriban en el archivo inmediatamente
    }

    // Método para cerrar el escritor y liberar el archivo
    public void closeWriter() {
        if (writer != null) {
            writer.close();
        }
    }
}
