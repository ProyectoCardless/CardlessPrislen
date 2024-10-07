/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transacciones")
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cuenta_id")
    @JsonBackReference
    private Cuenta cuenta;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    @NotNull
    private BigDecimal monto;

    @Column(name = "fecha")
    private Timestamp fecha = new Timestamp(System.currentTimeMillis());

    private BigDecimal comisionAplicada = BigDecimal.ZERO;

    public enum Tipo {
        Deposito, Retiro,Transf
    }
}
