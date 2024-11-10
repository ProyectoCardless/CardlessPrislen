package com.banco.CajerosCardless.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.banco.CajerosCardless.services.CuentaService;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

@Controller
public class WithdrawalController {
    @Autowired
    private CuentaService cuentaService;
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

        

        // Simular la respuesta de éxito
        try {
            cuentaService.solicitarRetiro(numeroCuenta, pin);
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud de retiro exitosa. Proceda a la verificación.");
            redirectAttributes.addFlashAttribute("numeroCuenta", numeroCuenta);
            redirectAttributes.addFlashAttribute("fase", 2); // Avanzar a la fase 2
            return "redirect:/withdrawal"; // Redirigir para mantener la PRG (Post/Redirect/Get)

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al solicitar retiro: " + e.getMessage());
            redirectAttributes.addFlashAttribute("fase", 1);
            return "redirect:/withdrawal"; // Redirigir de vuelta al formulario

        }

       
    }

    @PostMapping("/verificar-palabra-y-retirar")
    public String verificarYRetirar(
            @RequestParam("numeroCuenta") String numeroCuenta,
            @RequestParam("palabraIngresada") String palabraIngresada,
            @RequestParam("monto") String monto,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        if (palabraIngresada.isEmpty() || monto.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "La palabra ingresada o el monto no son válidos");
            redirectAttributes.addFlashAttribute("fase", 2);
            return "redirect:/withdrawal";
        }

        BigDecimal montobig;
        try {
            montobig = new BigDecimal(monto);
            if (montobig.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "El monto debe ser mayor a 0.");
                redirectAttributes.addFlashAttribute("fase", 2);
                return "redirect:/withdrawal";
            }
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Monto no válido");
            redirectAttributes.addFlashAttribute("fase", 2);
            return "redirect:/withdrawal";
        }

        try {
            cuentaService.verificarPalabraYRetirar(numeroCuenta, palabraIngresada, montobig, request);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("fase", 2);
            return "redirect:/withdrawal";
        }

        redirectAttributes.addFlashAttribute("mensaje", "Retiro realizado con éxito.");
        redirectAttributes.addFlashAttribute("fase", 1); // Reiniciar fase o actualizar según flujo deseado
        return "redirect:/withdrawal";
    }

    // Método para manejar el retiro en dólares
@PostMapping("/{numeroCuenta}/solicitar-retiro-dolares")
public String solicitarRetiroDolares(
        @PathVariable("numeroCuenta") String numeroCuenta,  // NumeroCuenta desde la ruta
        @RequestParam("palabraIngresada") String palabraIngresada,
        @RequestParam("monto") String monto,
        RedirectAttributes redirectAttributes,
        HttpServletRequest request) {

    // Validación de monto y palabra ingresada
    if (palabraIngresada.isEmpty() || monto.isEmpty()) {
        redirectAttributes.addFlashAttribute("errorMessage", "La palabra ingresada o el monto no son válidos");
        redirectAttributes.addFlashAttribute("fase", 2); // Mantener en la fase 2
        return "redirect:/withdrawal";
    }

    BigDecimal montoDolares;
    try {
        montoDolares = new BigDecimal(monto);
        if (montoDolares.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "El monto debe ser mayor a 0.");
            redirectAttributes.addFlashAttribute("fase", 2);
            return "redirect:/withdrawal";
        }
    } catch (NumberFormatException e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Monto no válido");
        redirectAttributes.addFlashAttribute("fase", 2);
        return "redirect:/withdrawal";
    }

    try {
        Map<String, Object> resultado = cuentaService.verificarPalabraYRetirarDolares(numeroCuenta, palabraIngresada, montoDolares,request);

        // Mensaje de éxito y detalles del retiro
        redirectAttributes.addFlashAttribute("mensaje", "Retiro en dólares realizado con éxito.");
        redirectAttributes.addFlashAttribute("saldoActual", resultado.get("saldoActual"));
        redirectAttributes.addFlashAttribute("tipoCambioVenta", resultado.get("tipoCambioVenta"));
        redirectAttributes.addFlashAttribute("montoRetiroColones", resultado.get("montoRetiroColones"));
        redirectAttributes.addFlashAttribute("comisionColones", resultado.get("comisionColones"));

    } catch (IllegalArgumentException e) {
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        redirectAttributes.addFlashAttribute("fase", 2);
    }

    return "redirect:/withdrawal";
}

}
