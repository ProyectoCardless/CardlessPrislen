/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "codigo_cliente", unique = true)
    private String codigoCliente;

    @NotNull
    @Column(name = "nombre_completo")
    private String nombreCompleto;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Categoria categoria;

    @Pattern(regexp = "\\d{8,15}", message = "El número de teléfono debe contener entre 8 y 15 dígitos")
    @NotNull
    private String telefono;

    @Email
    @NotNull
    private String correo;

    private String identificacion;

    private java.sql.Date fechaNacimiento;

    private String tipoNegocio;

    private String razonSocial;

    @NotNull
    private Integer maxCuentas;

    @OneToMany(mappedBy = "cliente")
    @JsonManagedReference
    private List<Cuenta> cuentas;

    // Enum para la categoría del cliente
    public enum Categoria {
        Fisico, Juridico
    }
}