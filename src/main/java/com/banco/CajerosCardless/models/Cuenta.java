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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "numero_cuenta", unique = true)
    private String numeroCuenta;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @JsonBackReference  
    private Cliente cliente;

    @NotNull
    @Column(name = "pin_encriptado")
    private String pinEncriptado;

    @NotNull
    @Column(name = "saldo")
    private BigDecimal saldo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private Estatus estatus = Estatus.Activa;

    @Column(name = "fecha_creacion")
    private Timestamp fechaCreacion = new Timestamp(System.currentTimeMillis());

    @OneToMany(mappedBy = "cuenta", cascade = CascadeType.ALL)
    @JsonIgnore 
    private List<Transaccion> transacciones;

    @OneToOne(mappedBy = "cuenta", cascade = CascadeType.ALL)
    @JsonIgnore
    private Comision comision;

    public enum Estatus {
        Activa, Inactiva
    }
}
