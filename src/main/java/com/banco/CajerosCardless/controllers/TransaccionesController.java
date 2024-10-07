package com.banco.CajerosCardless.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.banco.CajerosCardless.models.Transaccion;
import com.banco.CajerosCardless.services.CuentaService;

import java.util.List;

@Controller
public class TransaccionesController {

    @Autowired
    private CuentaService cuentaService;

    // Carga la vista para consultar transacciones
    @GetMapping("/consultar-transacciones")
    public String consultarTransacciones(Model model) {
        if (model.getAttribute("fase") == null) {
            model.addAttribute("fase", 1); // Solo setea la fase 1 si es null
        }
        model.addAttribute("numeroCuenta", ""); // Inicializa el número de cuenta vacío
        model.addAttribute("transacciones", null); // Inicializa sin transacciones
        return "consultar-transacciones"; // Nombre de la vista Thymeleaf
    }

    @PostMapping("/solicitar-acceso-transacciones")
    public String solicitarAccesoTransacciones(@RequestParam("numeroCuenta") String numeroCuenta, @RequestParam("pin") String pin,  Model model) {
        try {
            // Validar la cuenta y el PIN
            cuentaService.validarCuenta(numeroCuenta, pin);

            // Iniciar el proceso de generación y envío de palabra clave por SMS
            cuentaService.iniciarTransferencia(numeroCuenta, pin);

            // Avanzar a la Fase 2
            model.addAttribute("fase", 2);
            model.addAttribute("numeroCuenta", numeroCuenta);
            return "consultar-transacciones";
        } catch (IllegalArgumentException e) {
            // En caso de error, regresa a Fase 1 y muestra el mensaje de error
            model.addAttribute("fase", 1);
            model.addAttribute("errorMessage", e.getMessage());
            return "consultar-transacciones";
        }
    }

    @PostMapping("/verificar-palabra-transacciones")
    public String verificarPalabraClave(@RequestParam("numeroCuenta") String numeroCuenta, 
                                        @RequestParam("palabraIngresada") String palabraIngresada, 
                                        Model model) {
        try {

            // Obtener las transacciones
            List<Transaccion> transacciones = cuentaService.verificarPalabraYObtenerTransacciones(numeroCuenta,palabraIngresada);
            model.addAttribute("transacciones", transacciones);
            model.addAttribute("successMessage", "Transacción exitosa");

            return "consultar-transacciones";
        } catch (IllegalArgumentException e) {
            // En caso de error, regresa a Fase 2 y muestra el mensaje de error
            model.addAttribute("fase", 2);
            model.addAttribute("numeroCuenta", numeroCuenta);
            model.addAttribute("errorMessage", e.getMessage());
            return "consultar-transacciones";
        }
    }
}