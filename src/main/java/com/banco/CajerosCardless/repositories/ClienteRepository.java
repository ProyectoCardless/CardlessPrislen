/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.banco.CajerosCardless.repositories;

import com.banco.CajerosCardless.models.Cliente;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Puedes agregar métodos personalizados aquí si es necesario
     Optional<Cliente> findByIdentificacion(String identificacion);
     
     Cliente findByIdentificacionOrRazonSocial(String identificacion, String razonSocial);

     Optional<Cliente> findByRazonSocial(String razonSocial);

}
