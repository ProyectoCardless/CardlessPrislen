package com.banco.CajerosCardless.models;

public class AccountForm { //Se usa como objeto de transferencia de datos (DTO) que lo hace es recibir la infomacion del cliente y luego usar estos datos para hacer las busquedas y crear un nueva cuenta
    private String identificacion;
    private String pin;
    private String montoInicial;


    public String getIdentificacion() {
        return identificacion;
    }
    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }
    public String getPin() {
        return pin;
    }
    public void setPin(String pin) {
        this.pin = pin;
    }
    public String getMontoInicial() {
        return montoInicial;
    }
    public void setMontoInicial(String montoInicial) {
        this.montoInicial = montoInicial;
    }

    
}
