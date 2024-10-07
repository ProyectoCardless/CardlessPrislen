/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.banco.CajerosCardless.repositories;

import com.banco.CajerosCardless.models.Cliente;
import com.banco.CajerosCardless.models.Cuenta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    Cuenta findByNumeroCuenta(String numeroCuenta);
    long countByCliente(Cliente cliente);

    List<Cuenta> findByClienteId(int clienteId);

    List<Cuenta> findAllByCliente(Cliente cliente);

}
