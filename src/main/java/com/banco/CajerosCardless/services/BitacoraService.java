/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/springframework/Configuration.java to edit this template
 */
package com.banco.CajerosCardless.services;

import org.springframework.stereotype.Service;

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

    // Rutas de los archivos de bitácora
    private static final String BITACORA_JSON_PATH = "logs/log.json";
    private static final String BITACORA_CSV_PATH = "logs/log.csv";
    private static final String BITACORA_PLAIN_PATH = "logs/log.pos";

    /**
     * Método para obtener registros de bitácoras de acuerdo con los criterios
     * especificados.
     *
     * @param fecha      La fecha de los registros a consultar.
     * @param horaInicio La hora de inicio del rango de registros.
     * @param horaFin    La hora de fin del rango de registros.
     * @param formato    El formato de la bitácora (JSON, CSV o PLANO).
     * @return Registros de bitácora en el formato especificado.
     * @throws IOException Si ocurre un error al leer los archivos de bitácora.
     */
    public String obtenerBitacoras(LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, String formato)
            throws IOException {
        // Seleccionar el archivo en función del formato
        String bitacoraPath = getBitacoraPathByFormat(formato);

        // Leer registros del archivo
        List<String> registros = Files.readAllLines(Paths.get(bitacoraPath));

        // Filtrar por fecha y hora
        registros = filtrarPorFechaYHora(registros, fecha, horaInicio, horaFin);

        // Formatear los registros según el formato
        return formatBitacoraOutput(registros, formato);
    }

    // Obtener la ruta del archivo en función del formato
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

    // Filtrar registros por fecha y rango de horas
    private List<String> filtrarPorFechaYHora(List<String> registros, LocalDate fecha, LocalTime horaInicio,
            LocalTime horaFin) {
        return registros.stream()
                .filter(line -> {
                    // Filtrar por fecha si está presente
                    boolean fechaMatch = (fecha == null) || line.contains(fecha.toString());

                    // Filtrar por rango de horas si está presente
                    boolean horaMatch = true;
                    if (horaInicio != null && horaFin != null) {
                        String[] parts = line.split("\\s+");
                        if (parts.length > 1) {
                            try {
                                LocalTime horaRegistro = LocalTime.parse(parts[1],
                                        DateTimeFormatter.ofPattern("HH:mm:ss"));
                                horaMatch = (horaRegistro.isAfter(horaInicio) || horaRegistro.equals(horaInicio)) &&
                                        (horaRegistro.isBefore(horaFin) || horaRegistro.equals(horaFin));
                            } catch (Exception e) {
                                // Manejo de errores si el formato de hora en el registro es incorrecto
                                horaMatch = false;
                            }
                        } else {
                            horaMatch = false;
                        }
                    }

                    return fechaMatch && horaMatch;
                })
                .collect(Collectors.toList());
    }

    // Formatear registros en JSON, CSV o texto plano
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

    // Convertir registros a JSON
    private String registrosToJson(List<String> registros) {
        StringBuilder jsonOutput = new StringBuilder("[\n");
        for (String registro : registros) {
            jsonOutput.append("  { \"registro\": \"").append(registro).append("\" },\n");
        }
        jsonOutput.deleteCharAt(jsonOutput.length() - 2); // Eliminar la última coma
        jsonOutput.append("]");
        return jsonOutput.toString();
    }

    // Convertir registros a CSV
    private String registrosToCsv(List<String> registros) {
        StringBuilder csvOutput = new StringBuilder("Registro\n");
        for (String registro : registros) {
            csvOutput.append(registro).append("\n");
        }
        return csvOutput.toString();
    }

    // Convertir registros a texto plano
    private String registrosToPlainText(List<String> registros) {
        return String.join("\n", registros);
    }
}
