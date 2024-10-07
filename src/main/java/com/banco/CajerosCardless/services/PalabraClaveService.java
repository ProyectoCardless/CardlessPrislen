package com.banco.CajerosCardless.services;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
@Service
public class PalabraClaveService {

    private Map<String, String> palabrasClave = new ConcurrentHashMap<>();
    
    public String generarPalabraClave() {
        Random random = new Random();
        int numeroAleatorio = random.nextInt(900000) + 100000; // Número de 6 dígitos
        return String.valueOf(numeroAleatorio);
    }

    public void guardarPalabraClave(String numeroCuenta, String palabraClave) {
        palabrasClave.put(numeroCuenta, palabraClave);
    }

    public String obtenerPalabraClave(String numeroCuenta) {
        return palabrasClave.get(numeroCuenta);
    }

    public void eliminarPalabraClave(String numeroCuenta) {
        palabrasClave.remove(numeroCuenta);
    }
}
