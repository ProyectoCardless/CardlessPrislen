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
            @RequestParam String pin) {

        try {
            if ("dolares".equals(tipoDeposito)) {
                return depositarDolares(numeroCuenta, monto, pin);
            } else {
                return depositarColones(numeroCuenta, monto, pin);
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    private ResponseEntity<Map<String, Object>> depositarColones(String numeroCuenta, BigDecimal monto, String pin) {
        // Lógica para depositar en colones
        Cuenta cuenta = cuentaService.depositar(numeroCuenta, monto, pin);
        Map<String, Object> response = new HashMap<>();
        response.put("numeroCuenta", cuenta.getNumeroCuenta());
        response.put("montoRealDepositado", monto);
        response.put("comision", cuenta.getTransacciones().get(cuenta.getTransacciones().size() - 1).getComisionAplicada());
        response.put("mensaje", "Depósito realizado correctamente.");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> depositarDolares(String numeroCuenta, BigDecimal montoUSD, String pin) {
        // Lógica para depositar en dólares
        BigDecimal tipoCambioCompra = exchangeRateService.getCRCExchangeRate();
        BigDecimal montoCRC = montoUSD.multiply(tipoCambioCompra);
        Cuenta cuenta = cuentaService.depositar(numeroCuenta, montoCRC, pin);
        
        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Depósito realizado correctamente");
        response.put("montoDepositadoUSD", montoUSD);
        response.put("tipoCambio", tipoCambioCompra);
        response.put("montoDepositadoCRC", montoCRC);
        response.put("montoRealDepositado", cuenta.getSaldo());
        response.put("comision", cuenta.getTransacciones().get(cuenta.getTransacciones().size() - 1).getComisionAplicada());

        return ResponseEntity.ok(response);
    }
}
