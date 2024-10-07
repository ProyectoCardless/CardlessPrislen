package com.banco.CajerosCardless.controllers;

import com.banco.CajerosCardless.models.Cliente;
import com.banco.CajerosCardless.models.ClienteForm;
import com.banco.CajerosCardless.models.Cuenta;
import com.banco.CajerosCardless.models.AccountForm;
import com.banco.CajerosCardless.services.ClienteService;
import com.banco.CajerosCardless.services.CuentaService;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CuentaService cuentaService;

    // Método para mostrar el formulario para crear un cliente
    @GetMapping("/clientes/crear")
    public String mostrarFormularioCrearCliente(Model model) {
        model.addAttribute("clienteForm", new ClienteForm());
        return "crearCliente"; // Retorna la vista del formulario
    }
    //Controlador nuevo 
    @Controller
@RequestMapping("/seleccion-operacion")
public class SeleccionOperacionController {

    @GetMapping
    public String mostrarVistaSeleccion() {
        return "seleccion-operacion";
    }

    @PostMapping
    public String procesarSeleccion(@RequestParam String numeroCuenta, 
                                    @RequestParam String operacion) {
        if ("deposito".equals(operacion)) {
            return "redirect:/" + numeroCuenta + "/depositar";
        } else if ("transferencia".equals(operacion)) {
            return "redirect:/transferencia/" + numeroCuenta;
        } else {
            // Manejar caso de operación no válida
            return "redirect:/seleccion-operacion?error";
        }
    }
}
 //Fin del nuevo controlador   

    // Método para procesar la creación de un nuevo cliente
    @PostMapping("/clientes/crear")
    public String crearCliente(@ModelAttribute("clienteForm") @Valid ClienteForm clienteForm,
            BindingResult result,
            Model model) {
        if (result.hasErrors()) {
            return "crearCliente"; // Vuelve al formulario en caso de errores de validación
        }

        try {
            Cliente nuevoCliente = convertirFormACliente(clienteForm);
            clienteService.crearCliente(nuevoCliente);
            model.addAttribute("mensaje", "Cliente creado con éxito");
            return "redirect:/clientes/crear?success"; // Redirige al formulario con un mensaje de éxito
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el cliente: " + e.getMessage());
            return "crearCliente"; // Muestra el error si algo falla
        }
    }

    // Método para obtener las cuentas de un cliente por su identidad
    @GetMapping("/clientes/cuentas")
    public String obtenerCuentas(String identidad, Model model) {
        try {
            List<Cuenta> cuentas = clienteService.obtenerCuentasPorCliente(identidad);
            model.addAttribute("cuentas", cuentas);
            return "listarCuentas"; // Página que muestra la lista de cuentas
        } catch (Exception e) {
            model.addAttribute("error", "Error al obtener las cuentas del cliente: " + e.getMessage());
            return "resultados";
        }
    }

    // Método para mostrar el formulario para crear una cuenta
    @GetMapping("/clientes/crear-cuenta")
    public String mostrarFormularioCrearCuenta(Model model) {
        model.addAttribute("accountForm", new AccountForm()); // Asegúrate de tener una clase AccountForm
        return "crear-cuenta"; // Retorna la vista del formulario de creación de cuentas
    }

    // Método para crear una cuenta para un cliente
    @PostMapping("/clientes/crear-cuenta")
    public String crearCuenta(String identificacion, String pin, String montoInicial, Model model) {
        try {
            Cliente cliente = clienteService.obtenerClientePorIdentificacion(identificacion);
            if (cliente == null) {
                model.addAttribute("error", "Cliente no encontrado");
                return "resultados";
            }

            Cuenta nuevaCuenta = cuentaService.crearCuenta(
                    cliente, pin, new BigDecimal(montoInicial));
            model.addAttribute("mensaje", "Cuenta creada con éxito");
            return "resultados";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear la cuenta: " + e.getMessage());
            return "resultados";
        }
    }

    // Conversión de ClienteForm a Cliente
    private Cliente convertirFormACliente(ClienteForm form) {
        Cliente cliente = new Cliente();
        cliente.setCodigoCliente("CL" + generateRandomCode());
        cliente.setNombreCompleto(form.getNombre());
        cliente.setCategoria(Cliente.Categoria.valueOf(form.getCategoria()));
        cliente.setTelefono(form.getTelefono());
        cliente.setCorreo(form.getCorreo());
        cliente.setIdentificacion(form.getIdentificacion());

        if ("Fisico".equals(form.getCategoria())) {
            cliente.setFechaNacimiento(Date.valueOf(form.getFechaNacimiento()));
        } else if ("Juridico".equals(form.getCategoria())) {
            cliente.setTipoNegocio(form.getTipoNegocio());
            cliente.setRazonSocial(form.getRazonSocial());
        }

        cliente.setMaxCuentas(form.getMaxCuentas());
        return cliente;
    }

    // Generador de códigos aleatorios
    private String generateRandomCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
}
