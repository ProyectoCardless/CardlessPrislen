
package com.banco.CajerosCardless.controllers;

import com.banco.CajerosCardless.models.Cliente;
import com.banco.CajerosCardless.models.Cuenta;
import com.banco.CajerosCardless.models.EstadoCuenta;
import com.banco.CajerosCardless.models.Transaccion;
import com.banco.CajerosCardless.repositories.CuentaRepository;
import com.banco.CajerosCardless.services.ClienteService;
import com.banco.CajerosCardless.services.CuentaService;
import com.banco.CajerosCardless.services.ExchangeRateService;
import com.banco.CajerosCardless.services.PalabraClaveService;
import com.banco.CajerosCardless.services.SmsService;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController //Controlador principal creado con RESTful
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class BancoController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CuentaService cuentaService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private PalabraClaveService palabraClaveService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private CuentaRepository cuentaRepository;
    // Crear cliente


    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/clientes")
    public Cliente crearCliente(@RequestBody Cliente cliente) {
        return clienteService.crearCliente(cliente);
    }

    @PostMapping("/cuentas")
    public ResponseEntity<?> crearCuenta(@RequestBody Map<String, Object> request) {
        String identificacion = request.get("identificacion").toString();
        String pin = request.get("pin").toString();
        String montoInicialStr = request.get("montoInicial").toString();

        // Validar el cliente
        Cliente cliente = clienteService.obtenerClientePorIdentificacion(identificacion);
        if (cliente == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cliente no encontrado");
        }

        // Validar PIN (debe ser numérico de 4 a 6 dígitos)
        if (!pin.matches("^\\d{4,6}$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El PIN no es válido. Debe tener entre 4 y 6 dígitos.");
        }

        // Validar monto (debe ser un número sin decimales)
        BigDecimal montoInicial;
        try {
            montoInicial = new BigDecimal(montoInicialStr);

            if (montoInicial.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El monto inicial debe ser mayor que 0");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El monto inicial debe ser un número entero.");
        }

        // Verificar el máximo de cuentas (solo para clientes físicos)
        if (cliente.getCategoria().equals("Fisico")) {
            if (cliente.getCategoria().equals("Fisico")) {
                long cuentasCliente = cuentaService.contarCuentasPorCliente(cliente); // Aquí guardamos el número de
                                                                                      // cuentas
                if (cuentasCliente >= cliente.getMaxCuentas()) { // Comparas el número de cuentas con el límite del
                                                                 // cliente
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("El cliente ha alcanzado el número máximo de cuentas permitidas.");
                }
            }
        }

        // Crear la cuenta
        Cuenta nuevaCuenta = cuentaService.crearCuenta(cliente, pin, montoInicial);

        // Respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("numeroCuenta", nuevaCuenta.getNumeroCuenta());
        response.put("estatus", nuevaCuenta.getEstatus());
        response.put("saldoActual", nuevaCuenta.getSaldo());
        response.put("tipoCliente", cliente.getCategoria());
        response.put("nombreCliente", cliente.getNombreCompleto());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{numeroCuenta}/consultar-saldo-divisa")
    public ResponseEntity<Map<String, Object>> consultarSaldoDivisa(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String pin = body.get("pin").toString();

        // Validate account and PIN
        Cuenta cuenta = cuentaService.validarCuenta(numeroCuenta, pin);

        // Get the current balance in CRC
        BigDecimal saldoCRC = cuenta.getSaldo();

        // Get the exchange rate for converting CRC to USD (purchase rate)
        BigDecimal tipoCambioCompra = exchangeRateService.getCRCExchangeRate();

        // Convert balance to USD
        BigDecimal saldoUSD = saldoCRC.divide(tipoCambioCompra, 2, RoundingMode.HALF_UP);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("nombreCliente", cuenta.getCliente().getNombreCompleto());
        response.put("saldoUSD", saldoUSD);
        response.put("tipoCambioCompra", tipoCambioCompra);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{numeroCuenta}/estado-cuenta")
    public ResponseEntity<EstadoCuenta> obtenerEstadoCuenta(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String pin = body.get("pin").toString();

        try {
            // Validate account and pin and obtain account status
            EstadoCuenta estadoCuenta = cuentaService.consultarEstadoCuenta(numeroCuenta, pin);

            return ResponseEntity.ok(estadoCuenta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // Obtener Estado de Cuenta Dolarizado
    @PostMapping("/{numeroCuenta}/estado-cuenta-dolarizado")
    public ResponseEntity<EstadoCuenta> obtenerEstadoCuentaDolarizado(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String pin = body.get("pin").toString();

        try {
            // Validate account and pin and obtain account status with dollarization
            EstadoCuenta estadoCuentaDolarizado = cuentaService.consultarEstadoCuentaDolarizada(numeroCuenta, pin);

            return ResponseEntity.ok(estadoCuentaDolarizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    // Depositar en cuenta
    @PostMapping("/cuentas/{numeroCuenta}/depositar")
    public ResponseEntity<Map<String, Object>> depositar(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        BigDecimal monto = new BigDecimal(body.get("monto").toString());
        String pin = body.get("pin").toString();

        try {
            Cuenta cuenta = cuentaService.depositar(numeroCuenta, monto, pin);
            Map<String, Object> response = new HashMap<>();
            response.put("numeroCuenta", cuenta.getNumeroCuenta());
            response.put("montoRealDepositado", monto); // Devuelve el monto depositado
            response.put("comision",
                    cuenta.getTransacciones().get(cuenta.getTransacciones().size() - 1).getComisionAplicada()); // Comision
                                                                                                                // aplicada
                                                                                                                // si la
                                                                                                                // hay
            response.put("mensaje", "Depósito realizado correctamente.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/cuentas/{numeroCuenta}/depositar-dolares")
    public ResponseEntity<Map<String, Object>> depositarDolares(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        BigDecimal montoUSD = new BigDecimal(body.get("monto").toString());
        String pin = body.get("pin").toString();

        try {
            // Obtener tipo de cambio actual de compra
            BigDecimal tipoCambioCompra = exchangeRateService.getCRCExchangeRate(); // Asegúrate de que no sea nulo
            BigDecimal montoCRC = montoUSD.multiply(tipoCambioCompra); // Convertir a colones

            // Realizar el depósito (validar el PIN, aplicar comisiones, etc.)
            Cuenta cuenta = cuentaService.depositar(numeroCuenta, montoCRC, pin); // Aquí depositamos en colones

            // Crear respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Depósito realizado correctamente");
            response.put("montoDepositadoUSD", montoUSD);
            response.put("tipoCambio", tipoCambioCompra);
            response.put("montoDepositadoCRC", montoCRC);
            response.put("montoRealDepositado", cuenta.getSaldo()); // El saldo actualizado de la cuenta
            response.put("comision",
                    cuenta.getTransacciones().get(cuenta.getTransacciones().size() - 1).getComisionAplicada()); // Obtener
                                                                                                                // última
                                                                                                                // comisión
                                                                                                                // aplicada

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Manejar errores de PIN incorrecto u
                                                                             // otros errores
        }
    }

    @PostMapping("/{numeroCuenta}/solicitar-retiro")
    public ResponseEntity<String> solicitarRetiro(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String pin = body.get("pin").toString();

        try {
            cuentaService.solicitarRetiro(numeroCuenta, pin);
            return ResponseEntity.ok("Mensaje enviado al cliente con la palabra clave.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } 
    }

    @PostMapping("/{numeroCuenta}/solicitar-acceso-transacciones")
    public ResponseEntity<String> solicitarAccesoTransacciones(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String pin = body.get("pin").toString();

        try {
            // Validar la cuenta y el PIN
            cuentaService.validarCuenta(numeroCuenta, pin);

            // Iniciar el proceso de generación y envío de palabra clave por SMS
            cuentaService.iniciarTransferencia(numeroCuenta, pin);

            return ResponseEntity.ok("Se ha enviado una palabra por SMS al cliente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{numeroCuenta}/verificar-palabra-y-obtener-transacciones")
    public ResponseEntity<Object> verificarPalabraYObtenerTransacciones(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String palabraIngresada = body.get("palabraIngresada").toString();

        try {
            // Obtener las transacciones si la palabra clave es correcta
            List<Transaccion> transacciones = cuentaService.verificarPalabraYObtenerTransacciones(numeroCuenta,
                    palabraIngresada);

            // Crear la respuesta con el listado de transacciones
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Transacciones obtenidas correctamente");
            response.put("transacciones", transacciones);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Manejar la excepción y devolver una respuesta de error
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{numeroCuenta}/verificar-palabra-y-retirar")
    public ResponseEntity<Map<String, String>> verificarPalabraYRetirar(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String palabraIngresada = body.get("palabraIngresada").toString();
        BigDecimal monto = new BigDecimal(body.get("monto").toString());

        try {
            Cuenta cuenta = cuentaService.verificarPalabraYRetirar(numeroCuenta, palabraIngresada, monto);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Retiro exitoso");
            response.put("saldoActual", cuenta.getSaldo().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{numeroCuenta}/solicitar-retiro-dolares")
    public ResponseEntity<String> solicitarRetiroDolares(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String pin = body.get("pin").toString();

        try {
            cuentaService.solicitarRetiro(numeroCuenta, pin); // Reutiliza la lógica de solicitud de retiro
            return ResponseEntity.ok("Mensaje enviado al cliente con la palabra clave.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{numeroCuentaOrigen}/iniciar-transferencia")
    public ResponseEntity<String> iniciarTransferencia(@PathVariable String numeroCuentaOrigen,
            @RequestBody Map<String, Object> body) {
        String pin = body.get("pin").toString();

        try {
            cuentaService.iniciarTransferencia(numeroCuentaOrigen, pin);
            return ResponseEntity.ok("Mensaje enviado al cliente con la palabra clave.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/{numeroCuentaOrigen}/verificar-palabra-y-transferir")
    public ResponseEntity<Map<String, String>> verificarPalabraYTransferir(@PathVariable String numeroCuentaOrigen,
            @RequestBody Map<String, Object> body) {
        String palabraIngresada = body.get("palabraIngresada").toString();
        BigDecimal monto = new BigDecimal(body.get("monto").toString());
        String numeroCuentaDestino = body.get("numeroCuentaDestino").toString();

        try {
            Map<String, Object> result = cuentaService.verificarPalabraYTransferir(numeroCuentaOrigen, palabraIngresada,
                    monto, numeroCuentaDestino);

            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Transferencia exitosa");
            response.put("saldoOrigen", result.get("saldoOrigen").toString());
            response.put("montoTransferido", result.get("montoTransferido").toString());
            response.put("comision", result.get("comision").toString());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // Verificar palabra clave y realizar retiro en dólares - Segunda fase
    @PostMapping("/{numeroCuenta}/verificar-palabra-y-retirar-dolares")
    public ResponseEntity<Map<String, String>> verificarPalabraYRetirarDolares(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        String palabraIngresada = body.get("palabraIngresada").toString();
        BigDecimal montoDolares = new BigDecimal(body.get("monto").toString());

        try {
            // Procesar el retiro en dólares
            Map<String, Object> result = cuentaService.verificarPalabraYRetirarDolares(numeroCuenta, palabraIngresada,
                    montoDolares);

            // Crear respuesta
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Retiro exitoso");
            response.put("saldoActual", result.get("saldoActual").toString());
            response.put("tipoCambioVenta", result.get("tipoCambioVenta").toString());
            response.put("montoRetiroColones", result.get("montoRetiroColones").toString());
            response.put("comisionColones", result.get("comisionColones").toString());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    // Retirar de cuenta
    @PostMapping("/cuentas/{numeroCuenta}/retirar")
    public ResponseEntity<Map<String, String>> retirar(@PathVariable String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        BigDecimal monto = new BigDecimal(body.get("monto").toString());
        String pin = body.get("pin").toString();

        try {
            Cuenta cuenta = cuentaService.retirar(numeroCuenta, monto, pin);
            Map<String, String> response = new HashMap<>();
            response.put("mensaje", "Retiro exitoso");
            response.put("saldoActual", cuenta.getSaldo().toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); // Handle insufficient funds or
                                                                                      // PIN errors
        }
    }

    // Obtener todos los clientes
    @GetMapping("/clientes")
    public List<Cliente> getAllClientes() {
        return clienteService.getAllClientes();
    }

    // Obtener todas las cuentas
    @GetMapping("/cuentas")
    public List<Cuenta> getAllCuentas() {
        return cuentaService.getAllCuentas();
    }

    
    // Consultar estado de cuenta
    @GetMapping("/cuentas/{numeroCuenta}/estado")
    public EstadoCuenta obtenerEstadoCuenta(@PathVariable String numeroCuenta) {
        return cuentaService.obtenerEstadoCuenta(numeroCuenta);
    }
}