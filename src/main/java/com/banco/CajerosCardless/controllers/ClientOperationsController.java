package com.banco.CajerosCardless.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.banco.CajerosCardless.services.CuentaService;
import com.banco.CajerosCardless.models.Cliente;
import com.banco.CajerosCardless.models.Cuenta;
import com.banco.CajerosCardless.models.EstadoCuenta;
import com.banco.CajerosCardless.services.ClienteService;

@Controller
public class ClientOperationsController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CuentaService cuentaService;

    // Método para mostrar el dashboard del cliente
    @GetMapping("/clientes/dashboard")
    public String mostrarDashboard(Model model) {
        return "dashboard"; // Retorna la vista del dashboard
    }

    @GetMapping("/")
    public String home(Model model) {
        return "home";
    }

    // Método para cambiar el teléfono del cliente
    @PostMapping("/clientes/cambiar-telefono")
    public String cambiarTelefono(@RequestParam String identidad, @RequestParam String nuevoTelefono, Model model) {
        try {
            String mensaje = clienteService.cambiarTelefonoCliente(identidad, nuevoTelefono);
            model.addAttribute("successMessage", "cambio elaborado correctamente: " + mensaje);
            return "resultadoDashboard"; // Página que muestra el resultado
        } catch (Exception e) {
            model.addAttribute("error", "Error al cambiar el teléfono: " + e.getMessage());
            return "resultadoDashboard";
        }
    }

    @PostMapping({ "/clientes/cambiar-pin" })
    public String cambiarPin(@RequestParam String numeroCuenta, @RequestParam String pinActual,
            @RequestParam String nuevoPin, Model model) {
        try {
            System.out.println("HOLAAAAAAA");

            String mensaje = this.cuentaService.cambiarPin(numeroCuenta, pinActual, nuevoPin);
            model.addAttribute("successMessage", "Cambio de PIN realizado correctamente: " + mensaje);
            return "resultadoDashboard";
        } catch (Exception var5) {
            model.addAttribute("error", "Error al cambiar el PIN: " + var5.getMessage());
            return "resultadoDashboard";
        }
    }
    
    @PostMapping("/clientes/consultar-estado-cuenta-dolarizada")
    public String consultarEstadoCuentaDolarizada(@RequestParam String numeroCuenta, @RequestParam String pin,
            Model model) {
        try {
            // Llama al servicio para obtener el estado de cuenta en dólares
            EstadoCuenta estadoCuentaDolarizada = cuentaService.consultarEstadoCuentaDolarizada(numeroCuenta, pin);

            // Envía el PDF por correo con el estado de cuenta dolarizado
            cuentaService.enviarPdfPorCorreo(estadoCuentaDolarizada);

            // Crea un mapa con la información de la cuenta en dólares
            Map<String, Object> cuentaInfo = new HashMap<>();
            cuentaInfo.put("numeroCuenta", estadoCuentaDolarizada.getNumeroCuenta());
            cuentaInfo.put("saldoDolarizado", estadoCuentaDolarizada.getSaldo());
            cuentaInfo.put("estatus", estadoCuentaDolarizada.getEstatus());
            cuentaInfo.put("fechaCreacion", estadoCuentaDolarizada.getFechaCreacion());
            
            // Agrega otros detalles necesarios de estadoCuentaDolarizada a
            // cuentaInfoDolarizada

            // Añade la información de la cuenta al modelo
            model.addAttribute("cuentaInfo", cuentaInfo);
            model.addAttribute("successMessage", "Estado de cuenta en dólares enviado correctamente por correo.");

            return "estadoCuentaDolarizada"; // Nombre de la vista donde mostrarás la información dolarizada
        } catch (Exception e) {
            model.addAttribute("error", "Error al consultar el estado de la cuenta en dólares: " + e.getMessage());
            return "resultadoDashboard";
        }
    }
    
    @GetMapping("/clientes/cuentas-por-identidad")
    public String consultarCuentasPorIdentidad(@RequestParam String identidad, Model model) {
        try {
            // Buscar al cliente por su identidad
            Cliente cliente = clienteService.buscarClientePorIdentidad(identidad);

            // Obtener todas las cuentas del cliente
            List<Cuenta> cuentas = cuentaService.obtenerCuentasPorCliente(cliente);

            // Añadir información del cliente y cuentas al modelo
            model.addAttribute("cliente", cliente);
            model.addAttribute("cuentas", cuentas);

            return "cuentasCliente"; // Nombre de la vista donde se mostrarán las cuentas del cliente
        } catch (Exception e) {
            // Manejar caso cuando no se encuentra el cliente
            model.addAttribute("error", "Error al consultar las cuentas del cliente: " + e.getMessage());
            return "resultadoDashboard";
        }
    }

    @PostMapping("/clientes/consultar-estado-cuenta")
    public String consultarEstadoCuenta(@RequestParam String numeroCuenta, @RequestParam String pin, Model model) {
        try {
            // Llama al servicio para obtener el estado de cuenta
            EstadoCuenta estadoCuenta = cuentaService.consultarEstadoCuenta(numeroCuenta, pin);

            // Envía el PDF por correo
            cuentaService.enviarPdfPorCorreo(estadoCuenta);

            // Crea un mapa de cuentaInfo similar a lo que se hace en obtenerEstatusCuenta
            Map<String, Object> cuentaInfo = new HashMap<>();
            cuentaInfo.put("numeroCuenta", estadoCuenta.getNumeroCuenta());
            cuentaInfo.put("saldo", estadoCuenta.getSaldo());
            cuentaInfo.put("estatus", estadoCuenta.getEstatus());
            cuentaInfo.put("fechaCreacion", estadoCuenta.getFechaCreacion());
            // Añade cualquier otro detalle necesario de estadoCuenta a cuentaInfo

            // Añade la información de la cuenta al modelo
            model.addAttribute("cuentaInfo", cuentaInfo);
            model.addAttribute("successMessage", "Estado de cuenta enviado correctamente por correo.");

            return "estadoCuenta"; // Nombre de la vista donde mostrarás la información
        } catch (Exception e) {
            model.addAttribute("error", "Error al consultar el estado de la cuenta: " + e.getMessage());
            return "resultadoDashboard";
        }
    }


    // Método para cambiar el correo del cliente
    @PostMapping("/clientes/cambiar-correo")
    public String cambiarCorreo(@RequestParam String identidad, @RequestParam String nuevoCorreo, Model model) {
        try {
            String mensaje = clienteService.cambiarCorreoCliente(identidad, nuevoCorreo);

            model.addAttribute("successMessage", "cambio elaborado correctamente: " + mensaje);
            return "resultadoDashboard"; // Página que muestra el resultado
        } catch (Exception e) {
            model.addAttribute("error", "Error al cambiar el correo: " + e.getMessage());
            return "resultadoDashboard";
        }
    }

    @GetMapping("/clientes/estatus-cuenta")
    public String obtenerEstatusCuenta(@RequestParam String numeroCuenta, Model model) {
        try {
            Map<String, Object> cuentaInfo = clienteService.obtenerDetallesCuenta(numeroCuenta); // Este método devuelve
                                                                                                 // detalles completos
            model.addAttribute("cuentaInfo", cuentaInfo);
            return "estadoCuenta"; // Página exclusiva para mostrar el estado de cuenta
        } catch (Exception e) {
            model.addAttribute("error", "Error al obtener el estatus de la cuenta: " + e.getMessage());
            return "resultadoDashboard"; // En caso de error, se muestra en la vista general
        }
    }

    // Método para eliminar una cuenta del cliente
    @PostMapping("/clientes/eliminar-cuenta")
    public String eliminarCuenta(@RequestParam String numeroCuenta, @RequestParam String pin, Model model) {
        try {
            String mensaje = cuentaService.eliminarCuenta(numeroCuenta, pin);
            model.addAttribute("successMessage", "Cuenta eliminada elaborada correctamente: " + mensaje);
            return "resultadoDashboard"; // Página que muestra el resultado
        } catch (Exception e) {
            model.addAttribute("error", "Error al eliminar la cuenta: " + e.getMessage());
            return "resultadoDashboard";
        }
    }
}
