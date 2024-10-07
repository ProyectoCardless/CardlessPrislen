package com.banco.CajerosCardless.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class WithdrawalController {

    @GetMapping("/withdrawal")
    public String showWithdrawalForm(Model model) {
        Integer fase = (Integer) model.getAttribute("fase");
        if (fase == null) {
            fase = 1; // Si no hay fase en los atributos, por defecto será 1
        }
        model.addAttribute("fase", fase);
        return "WithDrawal";
    }

    @PostMapping("/solicitar-retiro")
    public String solicitarRetiro(
            @RequestParam("numeroCuenta") String numeroCuenta,
            @RequestParam("pin") String pin,
            RedirectAttributes redirectAttributes) {

        if (numeroCuenta.isEmpty() || pin.isEmpty() || pin.length() < 4 || pin.length() > 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "El número de cuenta o PIN es inválido");
            redirectAttributes.addFlashAttribute("fase", 1); // Mantener fase 1 en caso de error
            return "redirect:/withdrawal"; // Redirigir de vuelta al formulario
        }

        // Simular la respuesta de éxito
        redirectAttributes.addFlashAttribute("mensaje", "Solicitud de retiro exitosa. Proceda a la verificación.");
        redirectAttributes.addFlashAttribute("fase", 2); // Avanzar a la fase 2
        return "redirect:/withdrawal"; // Redirigir para mantener la PRG (Post/Redirect/Get)
    }

    @PostMapping("/verificar-palabra-y-retirar")
    public String verificarYRetirar(
            @RequestParam("numeroCuenta") String numeroCuenta,
            @RequestParam("palabraIngresada") String palabraIngresada,
            @RequestParam("monto") String monto,
            RedirectAttributes redirectAttributes) {

        if (palabraIngresada.isEmpty() || monto.isEmpty() || Integer.parseInt(monto) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "La palabra ingresada o el monto no son válidos");
            redirectAttributes.addFlashAttribute("fase", 2); // Mantener en la fase 2
            return "redirect:/withdrawal";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Retiro realizado con éxito.");
        return "redirect:/withdrawal";
    }

    // Método para manejar el retiro en dólares
    @PostMapping("/{numeroCuenta}/solicitar-retiro-dolares")
    public String solicitarRetiroDolares(
            @RequestParam("numeroCuenta") String numeroCuenta,
            @RequestParam("palabraIngresada") String palabraIngresada,
            @RequestParam("monto") String monto,
            RedirectAttributes redirectAttributes) {

        if (palabraIngresada.isEmpty() || monto.isEmpty() || Integer.parseInt(monto) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "La palabra ingresada o el monto no son válidos");
            redirectAttributes.addFlashAttribute("fase", 2); // Mantener en la fase 2
            return "redirect:/withdrawal";
        }

        // Lógica para procesar el retiro en dólares
        // Aquí podrías llamar a un servicio que maneje la lógica de retiros en dólares

        redirectAttributes.addFlashAttribute("mensaje", "Retiro en dólares realizado con éxito.");
        return "redirect:/withdrawal";
    }
}
