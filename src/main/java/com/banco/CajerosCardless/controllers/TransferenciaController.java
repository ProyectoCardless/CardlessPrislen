package com.banco.CajerosCardless.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.banco.CajerosCardless.services.CuentaService;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/transferencia")
public class TransferenciaController {

    @Autowired
    private CuentaService cuentaService;

    @GetMapping("/{numeroCuentaOrigen}")
    public String showTransferenciaForm(@PathVariable String numeroCuentaOrigen, Model model) {
        model.addAttribute("numeroCuentaOrigen", numeroCuentaOrigen);
        return "transferencia"; // Vista del formulario de transferencia
    }

    @PostMapping("/{numeroCuentaOrigen}/iniciar-transferencia")
    public String iniciarTransferencia(@PathVariable String numeroCuentaOrigen,
                                        @RequestParam String pin,
                                        RedirectAttributes redirectAttributes) {
        if (pin.isEmpty() || pin.length() < 4 || pin.length() > 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "El PIN es inválido");
            return "redirect:/transferencia/" + numeroCuentaOrigen; // Redirigir de vuelta al formulario
        }

        try {
            cuentaService.iniciarTransferencia(numeroCuentaOrigen, pin);
            redirectAttributes.addFlashAttribute("mensaje", "Mensaje enviado al cliente con la palabra clave.");
            return "redirect:/transferencia/" + numeroCuentaOrigen + "/verificacion"; // Redirigir a la vista de verificación
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/transferencia/" + numeroCuentaOrigen; // Redirigir de vuelta al formulario
        }
    }

    @PostMapping("/{numeroCuentaOrigen}/verificar-palabra-y-transferir")
    public String verificarPalabraYTransferir(@PathVariable String numeroCuentaOrigen,
                                               @RequestParam String palabraIngresada,
                                               @RequestParam BigDecimal monto,
                                               @RequestParam String numeroCuentaDestino,
                                               RedirectAttributes redirectAttributes) {
        if (palabraIngresada.isEmpty() || monto.compareTo(BigDecimal.ZERO) <= 0 || numeroCuentaDestino.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "La palabra ingresada, monto o número de cuenta destino no son válidos");
            return "redirect:/transferencia/" + numeroCuentaOrigen + "/verificacion"; // Mantener en la fase de verificación
        }

        try {
            Map<String, Object> result = cuentaService.verificarPalabraYTransferir(
                    numeroCuentaOrigen, palabraIngresada, monto, numeroCuentaDestino);

            redirectAttributes.addFlashAttribute("mensaje", "Transferencia exitosa");
            redirectAttributes.addFlashAttribute("saldoOrigen", result.get("saldoOrigen"));
            redirectAttributes.addFlashAttribute("montoTransferido", result.get("montoTransferido"));
            redirectAttributes.addFlashAttribute("comision", result.get("comision"));

            return "redirect:/transferencia/" + numeroCuentaOrigen + "/exito"; // Redirigir a la vista de éxito
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/transferencia/" + numeroCuentaOrigen + "/verificacion"; // Volver a la vista de verificación
        }
    }

    @GetMapping("/{numeroCuentaOrigen}/verificacion")
    public String showVerificacionForm(@PathVariable String numeroCuentaOrigen, Model model) {
        model.addAttribute("numeroCuentaOrigen", numeroCuentaOrigen);
        return "transferencia-verificacion"; // Vista para verificar la transferencia
    }

    @GetMapping("/{numeroCuentaOrigen}/exito")
    public String showExitoPage(@PathVariable String numeroCuentaOrigen, Model model) {
        model.addAttribute("numeroCuentaOrigen", numeroCuentaOrigen);
        return "transferencia-exito"; // Vista para la transferencia exitosa
    }
}
