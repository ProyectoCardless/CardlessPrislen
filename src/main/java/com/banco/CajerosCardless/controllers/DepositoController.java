package com.banco.CajerosCardless.controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.banco.CajerosCardless.models.Cuenta;
import com.banco.CajerosCardless.services.CuentaService;
import com.banco.CajerosCardless.services.ExchangeRateService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class DepositoController {

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    // Método para mostrar la vista de depósito
    @GetMapping("/{numeroCuenta}/depositar")
    public String mostrarVistaDeposito(@PathVariable String numeroCuenta, Model model) {
        model.addAttribute("numeroCuenta", numeroCuenta);
        return "deposito"; // Nombre de la vista Thymeleaf (deposito.html)
    }

    // Depositar en cuenta
    @PostMapping("/{numeroCuenta}/depositar")
    public ResponseEntity<Map<String, Object>> depositar(
            @PathVariable String numeroCuenta,
            @RequestParam String tipoDeposito,
            @RequestParam BigDecimal monto,
            @RequestParam String pin,
            HttpServletRequest request) { // Agregar `HttpServletRequest request` para capturar la IP y otros datos

        try {
            // Determina si es un depósito en dólares o colones
            if ("dolares".equalsIgnoreCase(tipoDeposito)) {
                return depositarDolares(numeroCuenta, monto, pin, request);
            } else {
                return depositarColones(numeroCuenta, monto, pin, request);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private ResponseEntity<Map<String, Object>> depositarColones(String numeroCuenta, BigDecimal monto, String pin,
            HttpServletRequest request) {
        // Llamada al método `depositar` en `cuentaService` pasando `request`
        Cuenta cuenta = cuentaService.depositar(numeroCuenta, monto, pin, request);

        Map<String, Object> response = new HashMap<>();
        response.put("numeroCuenta", cuenta.getNumeroCuenta());
        response.put("montoRealDepositado", monto);
        response.put("comision",
                cuenta.getTransacciones().get(cuenta.getTransacciones().size() - 1).getComisionAplicada());
        response.put("mensaje", "Depósito realizado correctamente.");

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> depositarDolares(String numeroCuenta, BigDecimal montoUSD, String pin,
            HttpServletRequest request) {
        // Obtener el tipo de cambio de compra en colones
        BigDecimal tipoCambioCompra = exchangeRateService.getCRCExchangeRate();
        BigDecimal montoCRC = montoUSD.multiply(tipoCambioCompra); // Convertir de USD a CRC

        // Llamada al método `depositar` en `cuentaService` pasando `request`
        Cuenta cuenta = cuentaService.depositar(numeroCuenta, montoCRC, pin, request);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Depósito realizado correctamente");
        response.put("tipoCambioDolaresUSD", tipoCambioCompra);
        response.put("montoDepositadoUSD", montoUSD);
        response.put("montoDepositadoCRC", montoCRC);
        response.put("saldo", cuenta.getSaldo());
        response.put("comision",
                cuenta.getTransacciones().get(cuenta.getTransacciones().size() - 1).getComisionAplicada());

        return ResponseEntity.ok(response);
    }
}
