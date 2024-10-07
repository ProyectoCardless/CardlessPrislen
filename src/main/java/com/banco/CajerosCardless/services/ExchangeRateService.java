package com.banco.CajerosCardless.services;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class ExchangeRateService {

    private static final String API_URL = "https://v6.exchangerate-api.com/v6/9e32948ee302d08a4daaecb0/latest/USD";



    public BigDecimal getCompraExchangeRate() {
        return new BigDecimal("520.1198"); 
    }
    public BigDecimal getCRCExchangeRate() {
        try {
            // Set up the connection to the API
            @SuppressWarnings("deprecation")
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Read the response from the API
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            int read;
            char[] buffer = new char[1024];
            while ((read = reader.read(buffer)) != -1) {
                response.append(buffer, 0, read);
            }

            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject conversionRates = jsonResponse.getJSONObject("conversion_rates");

            // Return the exchange rate for CRC
            return conversionRates.getBigDecimal("CRC");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    
    }
    
    public BigDecimal getVentaExchangeRate() {
        try {
            // Lógica para hacer una solicitud a la API y obtener el tipo de cambio
            @SuppressWarnings("deprecation")
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // Leer la respuesta de la API
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            StringBuilder response = new StringBuilder();
            int read;
            char[] buffer = new char[1024];
            while ((read = reader.read(buffer)) != -1) {
                response.append(buffer, 0, read);
            }

            // Parsear la respuesta JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject conversionRates = jsonResponse.getJSONObject("conversion_rates");

            // Retornar el tipo de cambio de venta para CRC
            return conversionRates.getBigDecimal("CRC");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("No se pudo obtener el tipo de cambio de venta.");
        }
    }
}
