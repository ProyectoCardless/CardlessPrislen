/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comisiones")
public class Comision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id")
    private Cuenta cuenta;

    private BigDecimal montoTotal = BigDecimal.ZERO;

    private BigDecimal montoPorRetiros = BigDecimal.ZERO;

    private BigDecimal montoPorDepositos = BigDecimal.ZERO;
}
