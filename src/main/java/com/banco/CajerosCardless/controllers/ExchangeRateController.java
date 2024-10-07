
package com.banco.CajerosCardless.controllers; //Esto es la version del controlador con listas para llenar la tabla con su mismo nombre

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Controller
public class ExchangeRateController {

    private BigDecimal compraRate;
    private BigDecimal ventaRate;

    @GetMapping("/exchange-rate")
    public String showExchangeRateForm(Model model) {
        return "ExhangeRate";
    }

    @PostMapping("/exchange-rate/compra")
    public String getCompraRate(Model model) {
        compraRate = generateRandomRate();
        model.addAttribute("compraRate", compraRate);
        return "ExhangeRate";
    }

    @PostMapping("/exchange-rate/venta")
    public String getVentaRate(Model model) {
        ventaRate = generateRandomRate().add(new BigDecimal("5"));
        model.addAttribute("ventaRate", ventaRate);
        return "ExhangeRate";
    }

    private BigDecimal generateRandomRate() {
        Random random = new Random();
        return new BigDecimal(random.nextDouble() * (550 - 500) + 500).setScale(2, RoundingMode.HALF_UP);
    }
} 