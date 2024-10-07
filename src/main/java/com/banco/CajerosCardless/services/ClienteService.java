/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.services;

import com.banco.CajerosCardless.models.Cliente;
import com.banco.CajerosCardless.models.Cuenta;
import com.banco.CajerosCardless.repositories.ClienteRepository;
import com.banco.CajerosCardless.repositories.CuentaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    public Cliente crearCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public Cliente obtenerClientePorId(Long id) {
        return clienteRepository.findById(id).orElse(null);
    }

    public void eliminarCliente(Long id) {
        clienteRepository.deleteById(id);
    }

    public List<Cliente> getAllClientes() {
        return clienteRepository.findAll();
    }

    public Cliente obtenerClientePorIdentificacion(String identificacion) {
        return clienteRepository.findByIdentificacion(identificacion)
                .orElse(null);
    }

    public String cambiarTelefonoCliente(String identificacion, String nuevoTelefono) {
        Cliente cliente = obtenerClientePorIdentidad(identificacion);
        String telefonoAnterior = cliente.getTelefono();
        cliente.setTelefono(nuevoTelefono);
        clienteRepository.save(cliente);

        return "Estimado cliente: " + cliente.getNombreCompleto() +
                ", usted ha cambiado el número de teléfono " + telefonoAnterior +
                " por el nuevo número " + nuevoTelefono;
    }

    public Cliente buscarClientePorIdentidad(String identidad) {
        // Busca el cliente usando la identificación o razón social
        Cliente cliente = clienteRepository.findByIdentificacionOrRazonSocial(identidad, identidad);

        if (cliente == null) {
            throw new IllegalArgumentException("No se encontró un cliente con la identidad proporcionada.");
        }

        return cliente;
    }
    // Change email address of a client
    public String cambiarCorreoCliente(String identificacion, String nuevoCorreo) {
        Cliente cliente = obtenerClientePorIdentidad(identificacion);
        String correoAnterior = cliente.getCorreo();
        cliente.setCorreo(nuevoCorreo);
        clienteRepository.save(cliente);

        return "Estimado cliente: " + cliente.getNombreCompleto() +
                ", usted ha cambiado la dirección de correo " + correoAnterior +
                " por " + nuevoCorreo;
    }

    // Get the status of a given account
    public String obtenerEstatusCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta == null) {
            throw new IllegalArgumentException("Cuenta no encontrada.");
        }
        return "La cuenta número " + cuenta.getNumeroCuenta() + " a nombre de " +
                cuenta.getCliente().getNombreCompleto() + " tiene estatus de " + cuenta.getEstatus();
    }

    // Get all account numbers with balances for a client
    public List<Cuenta> obtenerCuentasPorCliente(String identificacion) {
        Cliente cliente = obtenerClientePorIdentidad(identificacion);
        return cuentaRepository.findAllByCliente(cliente);
    }

    private Cliente obtenerClientePorIdentidad(String identificacion) {
        System.out.println(identificacion);
        return clienteRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
    }

    public Map<String, Object> obtenerDetallesCuenta(String numeroCuenta) throws Exception {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta == null) {
            throw new Exception("Cuenta no encontrada");
        }

        Map<String, Object> cuentaInfo = new HashMap<>();
        cuentaInfo.put("numeroCuenta", cuenta.getNumeroCuenta());
        cuentaInfo.put("saldo", cuenta.getSaldo());
        cuentaInfo.put("fechaApertura", cuenta.getFechaCreacion());
        cuentaInfo.put("estatus", cuenta.getEstatus());

        return cuentaInfo;
    }

}
