/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/springframework/Configuration.java to edit this template
 */
package com.banco.CajerosCardless.services;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BitacoraService {

    // Directorio de logs en el sistema de archivos temporal
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    private static final String BITACORA_JSON_PATH = TEMP_DIR + "/log.json";
    private static final String BITACORA_CSV_PATH = TEMP_DIR + "/log.csv";
    private static final String BITACORA_PLAIN_PATH = TEMP_DIR + "/log.pos";

    public BitacoraService() throws IOException {
        // Crear los archivos de bitácora si no existen
        crearArchivoSiNoExiste(BITACORA_JSON_PATH);
        crearArchivoSiNoExiste(BITACORA_CSV_PATH);
        crearArchivoSiNoExiste(BITACORA_PLAIN_PATH);
    }

    private void crearArchivoSiNoExiste(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile(); // Crear el archivo si no existe
        }
    }

    public String obtenerBitacoras(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String formato)
            throws IOException {
        // Obtener la ruta de archivo en función del formato
        String bitacoraPath = getBitacoraPathByFormat(formato);

        // Leer los registros del archivo de bitácora
        List<String> registros = Files.readAllLines(Paths.get(bitacoraPath));

        // Filtrar los registros por fecha y hora
        registros = filtrarPorFechaYHora(registros, fecha, horaInicio, horaFin);

        // Formatear los registros según el formato solicitado
        return formatBitacoraOutput(registros, formato);
    }

    private String getBitacoraPathByFormat(String formato) {
        switch (formato.toUpperCase()) {
            case "JSON":
                return BITACORA_JSON_PATH;
            case "CSV":
                return BITACORA_CSV_PATH;
            case "PLANO":
            default:
                return BITACORA_PLAIN_PATH;
        }
    }

    private List<String> filtrarPorFechaYHora(List<String> registros, LocalDate fecha, LocalTime horaInicio,
            LocalTime horaFin) {
        if (fecha != null) {
            registros = registros.stream()
                    .filter(line -> line.contains(fecha.toString()))
                    .collect(Collectors.toList());
        }

        if (horaInicio != null && horaFin != null) {
            registros = registros.stream()
                    .filter(line -> {
                        String[] parts = line.split("\\s+");
                        LocalTime horaRegistro = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm:ss"));
                        return (horaRegistro.isAfter(horaInicio) || horaRegistro.equals(horaInicio)) &&
                                (horaRegistro.isBefore(horaFin) || horaRegistro.equals(horaFin));
                    })
                    .collect(Collectors.toList());
        }

        return registros;
    }

    private String formatBitacoraOutput(List<String> registros, String formato) {
        switch (formato.toUpperCase()) {
            case "JSON":
                return registrosToJson(registros);
            case "CSV":
                return registrosToCsv(registros);
            case "PLANO":
            default:
                return registrosToPlainText(registros);
        }
    }

    private String registrosToJson(List<String> registros) {
        StringBuilder jsonOutput = new StringBuilder("[\n");
        for (String registro : registros) {
            jsonOutput.append("  { \"registro\": \"").append(registro).append("\" },\n");
        }
        jsonOutput.deleteCharAt(jsonOutput.length() - 2); // Eliminar la última coma
        jsonOutput.append("]");
        return jsonOutput.toString();
    }

    private String registrosToCsv(List<String> registros) {
        StringBuilder csvOutput = new StringBuilder("Registro\n");
        for (String registro : registros) {
            csvOutput.append(registro).append("\n");
        }
        return csvOutput.toString();
    }

    private String registrosToPlainText(List<String> registros) {
        return String.join("\n", registros);
    }
}
