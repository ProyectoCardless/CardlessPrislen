package com.banco.CajerosCardless.models;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
public class EstadoCuenta {

    private String numeroCuenta;
    private BigDecimal saldo;
    private String estatus;
    private Timestamp fechaCreacion;
    private List<Transaccion> transacciones;

    private BigDecimal montoTotalComisiones = BigDecimal.ZERO;
    private BigDecimal montoPorRetirosComision = BigDecimal.ZERO;
    private BigDecimal montoPorDepositosComision = BigDecimal.ZERO;

    private BigDecimal montoPorDepositos = BigDecimal.ZERO;
    private BigDecimal montoPorRetiros = BigDecimal.ZERO;
    private BigDecimal montoDepositosUSD = BigDecimal.ZERO; 
    private BigDecimal montoRetirosUSD = BigDecimal.ZERO;
    
        public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public String getEstatus() {
        return estatus;
    }

    public void setEstatus(String estatus) {
        this.estatus = estatus;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<Transaccion> getTransacciones() {
        return transacciones;
    }

    public void setTransacciones(List<Transaccion> transacciones) {
        this.transacciones = transacciones;
    }

    public BigDecimal getMontoTotalComisiones() {
        return montoTotalComisiones;
    }

    public void setMontoTotalComisiones(BigDecimal montoTotalComisiones) {
        this.montoTotalComisiones = montoTotalComisiones;
    }

    public BigDecimal getMontoPorRetiros() {
        return montoPorRetiros;
    }

    public void setMontoPorRetiros(BigDecimal montoPorRetiros) {
        this.montoPorRetiros = montoPorRetiros;
    }

    public BigDecimal getMontoPorDepositos() {
        return montoPorDepositos;
    }

    public void setMontoPorDepositos(BigDecimal montoPorDepositos) {
        this.montoPorDepositos = montoPorDepositos;
    }
}

