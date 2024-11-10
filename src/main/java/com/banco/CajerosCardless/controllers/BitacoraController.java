/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.controllers;

import com.banco.CajerosCardless.services.BitacoraService;

import io.jsonwebtoken.io.IOException;

import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class BitacoraController {

    @Autowired
    private BitacoraService bitacoraService;

    // Mostrar la página de consulta de bitácoras
    @GetMapping("/consultarBitacoras")
    public String showBitacoraPage() {
        return "consultarBitacoras"; // Página Thymeleaf para la consulta de bitácoras
    }

    // Procesar la consulta de bitácoras
    @PostMapping("/consultarBitacoras")
    public String consultarBitacoras(
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fecha,
            @RequestParam(value = "horaInicio", required = false) @DateTimeFormat(pattern = "HH:mm:ss") LocalTime horaInicio,
            @RequestParam(value = "horaFin", required = false) @DateTimeFormat(pattern = "HH:mm:ss") LocalTime horaFin,
            @RequestParam(value = "formato", defaultValue = "PLANO") String formato,
            Model model) throws java.io.IOException {
        try {
            String resultado = bitacoraService.obtenerBitacoras(fecha, horaInicio, horaFin, formato);
            model.addAttribute("resultado", resultado);
            model.addAttribute("formato", formato);
        } catch (IOException e) {
            model.addAttribute("error", "Error al leer las bitácoras: " + e.getMessage());
        }
        return "consultarBitacoras";
    }
}
