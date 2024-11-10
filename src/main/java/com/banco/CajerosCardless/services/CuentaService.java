/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.services;

import com.banco.CajerosCardless.models.Cliente;
import com.banco.CajerosCardless.models.Comision;
import com.banco.CajerosCardless.models.Cuenta;
import com.banco.CajerosCardless.models.EstadoCuenta;
import com.banco.CajerosCardless.models.Transaccion;
import com.banco.CajerosCardless.observers.CsvLogObserver;
import com.banco.CajerosCardless.observers.JsonLogObserver;
import com.banco.CajerosCardless.observers.PositionalLogObserver;
import com.banco.CajerosCardless.repositories.ClienteRepository;
import com.banco.CajerosCardless.repositories.ComisionRepository;
import com.banco.CajerosCardless.repositories.CuentaRepository;
import com.banco.CajerosCardless.repositories.TransaccionRepository;
import com.banco.CajerosCardless.utils.AESCipher;
import com.banco.CajerosCardless.utils.OpenAITranslationService;
import com.banco.CajerosCardless.utils.RandomWordGenerator;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import io.jsonwebtoken.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import jakarta.mail.util.ByteArrayDataSource;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;

import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
@Service
public class CuentaService {

    @Autowired
    private CuentaRepository cuentaRepository;
    private static final int TRANSACCIONES_GRATIS = 3;
    private static final BigDecimal COMISION = new BigDecimal("0.05");

    private UserActionLogger userActionLogger;

    private final SmsService smsService;
    private final Map<String, String> verificationMap = new HashMap<>();
    private final Map<String, Integer> failedAttemptsMap = new HashMap<>();
    @Autowired
    private BasicPDFGenerator basicPDFGenerator;


    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private ComisionRepository comisionRepository;

    @Autowired
    private PalabraClaveService palabraClaveService;

    @Autowired
    private ExchangeRateService exchangeRateService;

     @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private OpenAITranslationService translationService;

    @Autowired
    private TranslatedPDFGenerator translatedPDFGenerator;
    
    private static final BigDecimal TASA_CAMBIO_USD = BigDecimal.valueOf(0.0018);

     @Autowired
    public CuentaService(SmsService smsService, CuentaRepository cuentaRepository,OpenAITranslationService translationService,
            JavaMailSender mailSender, UserActionLogger userActionLogger) {
        this.smsService = smsService;
        this.cuentaRepository = cuentaRepository;
        this.translationService = translationService;
        this.translatedPDFGenerator = new TranslatedPDFGenerator(translationService);
        this.mailSender = mailSender;
        this.userActionLogger = userActionLogger;

    }
    
    public void realizarTransaccion(EstadoCuenta estadoCuenta, BigDecimal monto, Transaccion.Tipo tipo, String action,
            HttpServletRequest request) {
        // Lógica de la transacción
        Transaccion transaccion = new Transaccion();
        transaccion.setCuenta(cuentaRepository.findByNumeroCuenta(estadoCuenta.getNumeroCuenta()));
        transaccion.setMonto(monto);
        transaccion.setTipo(tipo);
        transaccion.setFecha(new Timestamp(System.currentTimeMillis()));

        if (tipo == Transaccion.Tipo.Deposito) {
            estadoCuenta.setSaldo(estadoCuenta.getSaldo().add(monto));
        } else if (tipo == Transaccion.Tipo.Retiro) {
            estadoCuenta.setSaldo(estadoCuenta.getSaldo().subtract(monto));
        } 
        System.out.println(tipo);
        transaccionRepository.save(transaccion);

        // Obtener datos de bitácora
        String ipAddress = getClientIpAddress(request);
        String operatingSystem = getClientOperatingSystem(request);
        String country = getCountryFromIp(ipAddress);

        // Registrar la acción en la bitácora
        userActionLogger.logAction(ipAddress, operatingSystem, country, action);
    }

    public Cuenta obtenerCuentaPorNumero(String numeroCuenta) {
        // Find the account by its number
        return cuentaRepository.findByNumeroCuenta(numeroCuenta);
    }

    public List<Cuenta> obtenerCuentasPorCliente(Cliente cliente) {
        // Busca todas las cuentas que pertenecen al cliente
        return cuentaRepository.findByClienteId(cliente.getId());
    }

    public String getClientIpAddress(HttpServletRequest request) {
    String[] headers = {
        "X-Forwarded-For", "X-REAL-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED",
        "HTTP_VIA", "REMOTE_ADDR"
    };

    for (String header : headers) {
        String ipAddress = request.getHeader(header);
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            // Handle the case where multiple IP addresses are provided (comma-separated)
            return ipAddress.split(",")[0];
        }
    }

    // Fallback to request.getRemoteAddr(), but handle local loopback for testing
    String remoteAddr = request.getRemoteAddr();
    if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "127.0.0.1".equals(remoteAddr)) {
        return "190.10.14.84"; // Replace with any IP address for testing
    }
    
    return remoteAddr;
}

    


    public String getClientOperatingSystem(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent").toLowerCase();

    if (userAgent.contains("windows")) {
        return "Windows";
    } else if (userAgent.contains("mac")) {
        return "MacOS";
    } else if (userAgent.contains("x11") || userAgent.contains("nix") || userAgent.contains("nux")) {
        return "Unix/Linux";
    } else if (userAgent.contains("android")) {
        return "Android";
    } else if (userAgent.contains("iphone")) {
        return "iOS";
    } else {
        return "Unknown";
    }
    }

    public String getCountryFromIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "127.0.0.1".equals(ipAddress)) {
            return "Localhost";
        }

        String apiUrl = "http://ip-api.com/json/" + ipAddress;

        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(apiUrl, String.class);

            JSONObject jsonResponse = new JSONObject(response);
            String country = jsonResponse.optString("country", "Unknown");

            if ("fail".equals(jsonResponse.optString("status"))) {
                return "Unknown";
            }

            return country;
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
    public EstadoCuenta consultarEstadoCuenta(String numeroCuenta, String pin) {
        Cuenta cuenta = validarCuenta(numeroCuenta, pin);

        List<Transaccion> transacciones = transaccionRepository.findByCuenta(cuenta);

        // Retrieve transaction sums, handling possible nulls
        BigDecimal totalDepositos = transaccionRepository.sumarPorTipo(cuenta.getId(), Transaccion.Tipo.Deposito);
        BigDecimal totalRetiros = transaccionRepository.sumarPorTipo(cuenta.getId(), Transaccion.Tipo.Retiro);

        // Default to zero if null
        if (totalDepositos == null)
            totalDepositos = BigDecimal.ZERO;
        if (totalRetiros == null)
            totalRetiros = BigDecimal.ZERO;

        // Create EstadoCuenta object
        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setNumeroCuenta(cuenta.getNumeroCuenta());
        estadoCuenta.setSaldo(cuenta.getSaldo());
        estadoCuenta.setEstatus(cuenta.getEstatus().toString());
        estadoCuenta.setFechaCreacion(cuenta.getFechaCreacion());
        estadoCuenta.setMontoPorDepositos(totalDepositos);
        estadoCuenta.setMontoPorRetiros(totalRetiros);
        estadoCuenta.setTransacciones(transacciones);

        // Get client's email
        String email = cuenta.getCliente().getCorreo();

        // Send PDF by email

        return estadoCuenta;
    }


       public String eliminarCuenta(String numeroCuenta, String pin) {
        Cuenta cuenta = validarCuenta(numeroCuenta, pin); // Validate account number and pin
        BigDecimal saldoActual = cuenta.getSaldo();

        // Confirm deletion
        String mensaje = "Estimado cliente: " + cuenta.getCliente().getNombreCompleto() +
                ", usted está a un paso de eliminar su cuenta " + cuenta.getNumeroCuenta() +
                " cuyo saldo actual es de " + saldoActual;

        // If balance is greater than zero, inform user to withdraw balance
        if (saldoActual.compareTo(BigDecimal.ZERO) > 0) {
            mensaje += ". Tome el dinero que ha sido dispuesto en el dispensador.";
        }

        // Proceed to delete account
        cuentaRepository.delete(cuenta);

        return mensaje + " Cuenta eliminada exitosamente.";
    }
 
    

    public EstadoCuenta consultarEstadoCuentaDolarizada(String numeroCuenta, String pin) {
        Cuenta cuenta = validarCuenta(numeroCuenta, pin);

        // Retrieve the list of transactions associated with the account
        List<Transaccion> transacciones = transaccionRepository.findByCuenta(cuenta);

        // Retrieve transaction sums, handling possible nulls
        BigDecimal totalDepositos = transaccionRepository.sumarPorTipo(cuenta.getId(), Transaccion.Tipo.Deposito);
        BigDecimal totalRetiros = transaccionRepository.sumarPorTipo(cuenta.getId(), Transaccion.Tipo.Retiro);

        // Default to zero if null
        if (totalDepositos == null)
            totalDepositos = BigDecimal.ZERO;
        if (totalRetiros == null)
            totalRetiros = BigDecimal.ZERO;

        // Get the exchange rate from the service
        BigDecimal exchangeRate = exchangeRateService.getCRCExchangeRate();
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("Cannot retrieve exchange rate.");
        }

        // Convert totals to USD by dividing by the exchange rate
        BigDecimal totalDepositosUSD = totalDepositos.divide(exchangeRate, 2, RoundingMode.HALF_UP);
        BigDecimal totalRetirosUSD = totalRetiros.divide(exchangeRate, 2, RoundingMode.HALF_UP);
        BigDecimal saldoUSD = cuenta.getSaldo().divide(exchangeRate, 2, RoundingMode.HALF_UP);

        // Create EstadoCuenta object with dollarized values
        EstadoCuenta estadoCuentaDolarizado = new EstadoCuenta();
        estadoCuentaDolarizado.setNumeroCuenta(cuenta.getNumeroCuenta());
        estadoCuentaDolarizado.setSaldo(saldoUSD);
        estadoCuentaDolarizado.setEstatus(cuenta.getEstatus().toString());
        estadoCuentaDolarizado.setFechaCreacion(cuenta.getFechaCreacion());
        estadoCuentaDolarizado.setMontoPorDepositos(totalDepositosUSD);
        estadoCuentaDolarizado.setMontoPorRetiros(totalRetirosUSD);
        estadoCuentaDolarizado.setTransacciones(transacciones);

        // Get client's email
        String email = cuenta.getCliente().getCorreo();

        // Send PDF by email

        return estadoCuentaDolarizado;
    }

    public String iniciarRetiro(String numeroCuenta, BigDecimal monto) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta != null && cuenta.getSaldo().compareTo(monto) >= 0) {
            String randomWord = RandomWordGenerator.generateWord(7);
            verificationMap.put(numeroCuenta, randomWord);  // Save the word for later verification
            failedAttemptsMap.put(numeroCuenta, 0);  // Reset failed attempts

            smsService.sendSms(cuenta.getCliente().getTelefono(), "Tu palabra de verificación es: " + randomWord);

            return "Palabra enviada. Por favor, verifica.";
        }
        return "Error: Cuenta no encontrada o saldo insuficiente.";
    }
    
    

    public boolean validarPalabra(String numeroCuenta, String palabraIngresada) {
        if (verificationMap.containsKey(numeroCuenta)) {
            String correctWord = verificationMap.get(numeroCuenta);
            if (correctWord.equalsIgnoreCase(palabraIngresada)) {
                verificationMap.remove(numeroCuenta);
                return true;
            } else {
                int attempts = failedAttemptsMap.getOrDefault(numeroCuenta, 0);
                attempts++;
                if (attempts >= 3) {
                    Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
                    cuenta.setEstatus(Cuenta.Estatus.Inactiva);  // Lock the account
                    cuentaRepository.save(cuenta);
                    verificationMap.remove(numeroCuenta);  // Remove verification
                    return false;
                }
                failedAttemptsMap.put(numeroCuenta, attempts);
                return false;
            }
        }
        return false;
    }
    
    public Cuenta crearCuenta(Cliente cliente, String pin, BigDecimal depositoInicial) {
        Cuenta cuenta = new Cuenta();
        cuenta.setCliente(cliente);
        cuenta.setNumeroCuenta(generarNumeroCuenta());
        cuenta.setPinEncriptado(AESCipher.encrypt(pin));
        cuenta.setSaldo(depositoInicial);
        cuenta.setEstatus(Cuenta.Estatus.Activa);        return cuentaRepository.save(cuenta);
    }

    public long contarCuentasPorCliente(Cliente cliente) {
        return cuentaRepository.countByCliente(cliente);
    }


    // Método para validar el PIN
    private boolean validarPIN(String pinIngresado, String pinEncriptado) {
        // Desencripta el PIN almacenado
        String pinDesencriptado = AESCipher.decrypt(pinEncriptado);

        // Compara el PIN ingresado con el desencriptado
        return pinIngresado.equals(pinDesencriptado);
    }

    public Cuenta depositar(String numeroCuenta, BigDecimal monto, String pinIngresado, HttpServletRequest request) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta != null) {
            if (validarPIN(pinIngresado, cuenta.getPinEncriptado())) {
                BigDecimal comision = BigDecimal.ZERO;
                long totalTransacciones = transaccionRepository.countByCuenta(cuenta);

                if (totalTransacciones >= TRANSACCIONES_GRATIS) {
                    comision = monto.multiply(COMISION);
                    monto = monto.subtract(comision);
                }

                BigDecimal crcRate = exchangeRateService.getCRCExchangeRate();
                if (crcRate != null) {
                    BigDecimal montoUSD = monto.divide(crcRate, BigDecimal.ROUND_HALF_EVEN);
                    System.out.println("Deposit amount in USD: " + montoUSD);
                }

                cuenta.setSaldo(cuenta.getSaldo().add(monto));
                cuentaRepository.save(cuenta);

                // Create EstadoCuenta object
                EstadoCuenta estadoCuenta = new EstadoCuenta();
                estadoCuenta.setNumeroCuenta(cuenta.getNumeroCuenta());
                estadoCuenta.setSaldo(cuenta.getSaldo());
                estadoCuenta.setEstatus(cuenta.getEstatus().toString());
                estadoCuenta.setFechaCreacion(cuenta.getFechaCreacion());

                realizarTransaccion(estadoCuenta, monto, Transaccion.Tipo.Deposito, "Depósito", request);
                return cuenta;
            } else {
                throw new IllegalArgumentException("El PIN ingresado es incorrecto.");
            }
        }
        return null;
    }
    
    public void enviarPdfPorCorreo(EstadoCuenta estadoCuenta) {
        // Obtener la cuenta asociada
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(estadoCuenta.getNumeroCuenta());
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta no existe.");
        }

        // Obtener el correo del cliente
        String email = cuenta.getCliente().getCorreo();

        // Obtener las transacciones de la cuenta
        List<Transaccion> transacciones = transaccionRepository.findByCuenta(cuenta);

        // Crear PDFs en español e inglés
        ByteArrayOutputStream pdfOutputStreamEs = new ByteArrayOutputStream();
        ByteArrayOutputStream pdfOutputStreamEn = new ByteArrayOutputStream();

        try {
            // Generar PDF en español
            basicPDFGenerator.generatePDF(estadoCuenta, transacciones, pdfOutputStreamEs);

            // Generar PDF traducido al inglés
            translatedPDFGenerator.generateTranslatedPDF(estadoCuenta, transacciones, pdfOutputStreamEn, "en");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar los PDFs.");
        }

        // Enviar el correo con ambos PDFs adjuntos
        try {
            sendEmailWithAttachment(
                    email,
                    "Estado de Cuenta",
                    "Adjunto encontrará su estado de cuenta en español e inglés.",
                    pdfOutputStreamEs.toByteArray(),
                    pdfOutputStreamEn.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al enviar el correo.");
        }
    }

    private void sendEmailWithAttachment(String to, String subject, String body, byte[] attachmentEs,
            byte[] attachmentEn)
            throws Exception {
        jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("proyectocardless@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body);

        // Attach the Spanish and English PDFs
        helper.addAttachment("EstadoCuenta_ES.pdf", new ByteArrayDataSource(attachmentEs, "application/pdf"));
        helper.addAttachment("EstadoCuenta_EN.pdf", new ByteArrayDataSource(attachmentEn, "application/pdf"));

        // Send the email
        mailSender.send(message);
    }
    public String cambiarPin(String numeroCuenta, String pinActual, String nuevoPin) {
        // Obtener la cuenta por su número
        Cuenta cuenta = obtenerCuentaPorNumero(numeroCuenta);

        // Desencriptar el PIN actual almacenado
        String pinActualDesencriptado = AESCipher.decrypt(cuenta.getPinEncriptado());

        // Validar que el PIN actual ingresado coincida con el desencriptado
        if (!pinActual.equals(pinActualDesencriptado)) {
            throw new IllegalArgumentException("El PIN actual es incorrecto.");
        }

        // Validar el formato del nuevo PIN
        if (!nuevoPin.matches("^\\d{4,6}$")) {
            throw new IllegalArgumentException(
                    "El nuevo PIN no cumple con el formato requerido: debe tener al menos una letra mayúscula, un número y tener 6 caracteres.");
        }

        // Encriptar el nuevo PIN antes de almacenarlo
        String nuevoPinEncriptado = AESCipher.encrypt(nuevoPin);
        cuenta.setPinEncriptado(nuevoPinEncriptado);

        // Guardar la cuenta con el nuevo PIN
        cuentaRepository.save(cuenta);

        return "Estimado cliente: " + cuenta.getCliente().getNombreCompleto() +
                ", el PIN de su cuenta " + cuenta.getNumeroCuenta() + " ha sido cambiado exitosamente.";
    }

    // Valida que el PIN sea un número de 4 a 6 dígitos, por ejemplo
    private boolean validarFormatoPIN(String pin) {
        String regex = "^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{6}$";
        return pin.matches(regex);
    }

    // En CuentaService o un servicio relacionado con retiros
    public Cuenta retirar(String numeroCuenta, BigDecimal monto, String pinIngresado) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta != null) {
            // Validate the PIN
            if (validarPIN(pinIngresado, cuenta.getPinEncriptado())) {
                BigDecimal comision = BigDecimal.ZERO;

                // Count the number of transactions
                long totalTransacciones = transaccionRepository.countByCuenta(cuenta);

                // If more than 3 transactions, apply commission
                if (totalTransacciones >= TRANSACCIONES_GRATIS) {
                    comision = monto.multiply(COMISION);
                    monto = monto.add(comision); // Increase the withdrawal amount
                }

                // Ensure sufficient balance after commission
                if (cuenta.getSaldo().compareTo(monto) >= 0) {
                    cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
                    cuentaRepository.save(cuenta);

                    // Send SMS with verification word
                    String palabraClave = generarSecuenciaNumerosAleatorios(6);
                    smsService.sendSms(cuenta.getCliente().getTelefono(), palabraClave);
                    // Record the transaction
                    Transaccion transaccion = new Transaccion();
                    transaccion.setCuenta(cuenta);
                    transaccion.setTipo(Transaccion.Tipo.Retiro);
                    transaccion.setMonto(monto);
                    transaccion.setComisionAplicada(comision);
                    transaccionRepository.save(transaccion);

                    return cuenta;
                } else {
                    throw new IllegalArgumentException("Saldo insuficiente.");
                }
            } else {
                throw new IllegalArgumentException("El PIN ingresado es incorrecto.");
            }
        }
        return null;
    }

    public void solicitarRetiro(String numeroCuenta, String pinIngresado) {
        Cuenta cuenta = validarCuenta(numeroCuenta, pinIngresado);

        // Generar la palabra clave
        String palabraClave = palabraClaveService.generarPalabraClave();
        System.out.println("Palabra generada: " + palabraClave); // Al generar la palabra

        palabraClaveService.guardarPalabraClave(numeroCuenta, palabraClave);

        // Enviar SMS al cliente
        smsService.sendSms(cuenta.getCliente().getTelefono(), palabraClave);
    }

    public Cuenta validarCuenta(String numeroCuenta, String pinIngresado) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta != null && validarPIN(pinIngresado, cuenta.getPinEncriptado())) {
            return cuenta;
        } else {
            throw new IllegalArgumentException("El PIN ingresado es incorrecto o la cuenta no existe.");
        }
    }

    public Map<String, Object> verificarPalabraYRetirarDolares(String numeroCuenta, String palabraIngresada,
            BigDecimal montoDolares, HttpServletRequest request) {
        // Verificar la palabra clave
        

        String palabraClaveAlmacenada = palabraClaveService.obtenerPalabraClave(numeroCuenta);
        System.out.println("Palabra almacenada: " + verificationMap.get(numeroCuenta)); // Al verificar la palabra
        System.out.println("Palabra ingresada: " + palabraIngresada);
        if (palabraClaveAlmacenada == null || !palabraClaveAlmacenada.equals(palabraIngresada)) {
            throw new IllegalArgumentException("La palabra ingresada es incorrecta.");
        }

        // Buscar la cuenta y validar saldo
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta == null) {
            throw new IllegalArgumentException("Cuenta no encontrada.");
        }

        // Obtener el tipo de cambio de venta del BCCR
        BigDecimal tipoCambioVenta = exchangeRateService.getVentaExchangeRate(); // Método que obtiene el tipo de cambio
                                                                                 // de venta
        BigDecimal montoColones = montoDolares.multiply(tipoCambioVenta);

        // Validar fondos suficientes (conversión a colones)
        if (cuenta.getSaldo().compareTo(montoColones) < 0) {
            throw new IllegalArgumentException("Fondos insuficientes.");
        }

        // Calcular comisión si es necesario
        BigDecimal comision = BigDecimal.ZERO;
        long totalTransacciones = transaccionRepository.countByCuenta(cuenta);
        if (totalTransacciones >= TRANSACCIONES_GRATIS) {
            comision = montoColones.multiply(COMISION); // Comision sobre colones
            montoColones = montoColones.add(comision); // Añadir la comisión al monto total
        }

        // Realizar el retiro en colones
        cuenta.setSaldo(cuenta.getSaldo().subtract(montoColones));
        cuentaRepository.save(cuenta);

        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setNumeroCuenta(cuenta.getNumeroCuenta());
        estadoCuenta.setSaldo(cuenta.getSaldo());

        realizarTransaccion(estadoCuenta, montoColones, Transaccion.Tipo.Retiro, "Retiro", request);

        // Crear y retornar la respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("saldoActual", cuenta.getSaldo());
        response.put("tipoCambioVenta", tipoCambioVenta);
        response.put("montoRetiroColones", montoColones);
        response.put("comisionColones", comision);

        // Eliminar la palabra clave después de procesar el retiro
        verificationMap.remove(numeroCuenta);

        return response;
    }

    public void iniciarTransferencia(String numeroCuentaOrigen, String pinIngresado) {
        Cuenta cuentaOrigen = validarCuenta(numeroCuentaOrigen, pinIngresado);

        // Generar la palabra clave
        String palabraClave = palabraClaveService.generarPalabraClave();
        palabraClaveService.guardarPalabraClave(numeroCuentaOrigen, palabraClave);

        // Enviar SMS al cliente
        smsService.sendSms(cuentaOrigen.getCliente().getTelefono(), palabraClave);
    }


    public Map<String, Object> verificarPalabraYTransferir(String numeroCuentaOrigen, String palabraIngresada,
            BigDecimal monto, String numeroCuentaDestino, HttpServletRequest request) {

        // Verificar palabra clave
        String palabraClaveAlmacenada = palabraClaveService.obtenerPalabraClave(numeroCuentaOrigen);
        if (palabraClaveAlmacenada == null || !palabraClaveAlmacenada.equals(palabraIngresada)) {
            throw new IllegalArgumentException("La palabra ingresada es incorrecta.");
        }

        // Validar cuenta origen y fondos
        Cuenta cuentaOrigen = cuentaRepository.findByNumeroCuenta(numeroCuentaOrigen);
        if (cuentaOrigen == null || cuentaOrigen.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("Cuenta no encontrada o fondos insuficientes.");
        }

        // Validar cuenta destino
        Cuenta cuentaDestino = cuentaRepository.findByNumeroCuenta(numeroCuentaDestino);
        if (cuentaDestino == null || !cuentaDestino.getCliente().equals(cuentaOrigen.getCliente())) {
            throw new IllegalArgumentException("La cuenta destino no pertenece al mismo dueño.");
        }

        // Calcular comisión si aplica
        BigDecimal comision = BigDecimal.ZERO;
        long totalTransacciones = transaccionRepository.countByCuenta(cuentaOrigen);
        if (totalTransacciones >= TRANSACCIONES_GRATIS) {
            comision = monto.multiply(COMISION);
            monto = monto.add(comision); // Ajuste en el monto con la comisión incluida
        }

        // Crear estados de cuenta para registrar en realizarTransaccion
        EstadoCuenta estadoCuentaOrigen = new EstadoCuenta();
        estadoCuentaOrigen.setNumeroCuenta(cuentaOrigen.getNumeroCuenta());
        estadoCuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(monto));

        EstadoCuenta estadoCuentaDestino = new EstadoCuenta();
        estadoCuentaDestino.setNumeroCuenta(cuentaDestino.getNumeroCuenta());
        estadoCuentaDestino.setSaldo(cuentaDestino.getSaldo().add(monto.subtract(comision)));

        // Registrar ambas transacciones (origen y destino)
        realizarTransaccion(estadoCuentaOrigen, monto, Transaccion.Tipo.Transferencia, "Transferencia Origen", request);
        realizarTransaccion(estadoCuentaDestino, monto.subtract(comision), Transaccion.Tipo.Transferencia,
                "Transferencia Destino", request);

        // Aplicar ajustes de saldo y guardar
        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(monto));
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(monto.subtract(comision)));

        cuentaRepository.save(cuentaOrigen);
        cuentaRepository.save(cuentaDestino);

        // Eliminar palabra clave solo después de completar todas las operaciones
        palabraClaveService.eliminarPalabraClave(numeroCuentaOrigen);

        // Respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("saldoOrigen", cuentaOrigen.getSaldo());
        response.put("montoTransferido", monto.subtract(comision));
        response.put("comision", comision);

        return response;
    }

    public Cuenta verificarPalabraYRetirar(String numeroCuenta, String palabraIngresada, BigDecimal monto,
            HttpServletRequest request) {
        // Verify the provided palabra (verification word) for the account
        String palabraClaveAlmacenada = palabraClaveService.obtenerPalabraClave(numeroCuenta);
        if (palabraClaveAlmacenada == null || !palabraClaveAlmacenada.equals(palabraIngresada)) {
            throw new IllegalArgumentException("La palabra ingresada es incorrecta.");
        }

        // Fetch the account information
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta == null) {
            throw new IllegalArgumentException("Cuenta no encontrada.");
        }

        // Ensure sufficient balance
        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("Fondos insuficientes.");
        }

        BigDecimal comision = BigDecimal.ZERO;
        long totalTransacciones = transaccionRepository.countByCuenta(cuenta);
        if (totalTransacciones >= TRANSACCIONES_GRATIS) {
            comision = monto.multiply(COMISION);
            monto = monto.add(comision);
        }

        // Call realizarTransaccion for logging and transaction processing
        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setNumeroCuenta(cuenta.getNumeroCuenta());
        estadoCuenta.setSaldo(cuenta.getSaldo());

        realizarTransaccion(estadoCuenta, monto, Transaccion.Tipo.Retiro, "Retiro", request);

        // Apply the commission and update the account balance
        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuentaRepository.save(cuenta);

        // Remove the verification word after a successful transaction
        palabraClaveService.eliminarPalabraClave(numeroCuenta);

        return cuenta;
    }

    public List<Transaccion> verificarPalabraYObtenerTransacciones(String numeroCuenta, String palabraIngresada) {
        // Verificar la palabra clave
        String palabraClaveAlmacenada = palabraClaveService.obtenerPalabraClave(numeroCuenta);
        if (palabraClaveAlmacenada == null || !palabraClaveAlmacenada.equals(palabraIngresada)) {
            throw new IllegalArgumentException("La palabra ingresada es incorrecta.");
        }

        // Buscar la cuenta
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta == null) {
            throw new IllegalArgumentException("Cuenta no encontrada.");
        }

        // Obtener las transacciones asociadas a la cuenta
        List<Transaccion> transacciones = transaccionRepository.findByCuenta(cuenta);

        // Retornar la lista de transacciones
        return transacciones;
    }

    // Obtener todas las cuentas
    public List<Cuenta> getAllCuentas() {
        return cuentaRepository.findAll();
    }

    private String generarNumeroCuenta() {
        // Implementación del método para generar el número de cuenta único
        return "CTA" + (int) (Math.random() * 1000000);
    }

    public static String generarSecuenciaNumerosAleatorios(int longitud) {
        Random random = new Random();
        StringBuilder secuencia = new StringBuilder();

        for (int i = 0; i < longitud; i++) {
            int numeroAleatorio = random.nextInt(10); // Genera un número entre 0 y 9
            secuencia.append(numeroAleatorio); // Añade el número a la secuencia
        }

        return secuencia.toString(); // Devuelve la secuencia en formato String
    }

    
    public EstadoCuenta obtenerEstadoCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta == null) {
            return null; // Si no se encuentra la cuenta, devolver null o manejar adecuadamente
        }

        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setNumeroCuenta(cuenta.getNumeroCuenta());
        estadoCuenta.setSaldo(cuenta.getSaldo());
        estadoCuenta.setEstatus(cuenta.getEstatus().toString());
        estadoCuenta.setFechaCreacion(cuenta.getFechaCreacion());

        // Obtener todas las transacciones de la cuenta
        List<Transaccion> transacciones = cuenta.getTransacciones();
        estadoCuenta.setTransacciones(transacciones);

        // Calcular el total de depósitos y retiros en colones
        BigDecimal totalDepositos = transaccionRepository.sumarPorTipo(cuenta.getId(), Transaccion.Tipo.Deposito);
        BigDecimal totalRetiros = transaccionRepository.sumarPorTipo(cuenta.getId(), Transaccion.Tipo.Retiro);

        // Convertir a USD
        BigDecimal totalDepositosUSD = totalDepositos.multiply(TASA_CAMBIO_USD).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalRetirosUSD = totalRetiros.multiply(TASA_CAMBIO_USD).setScale(2, RoundingMode.HALF_UP);

        estadoCuenta.setMontoPorDepositos(totalDepositos);
        estadoCuenta.setMontoPorRetiros(totalRetiros);
        estadoCuenta.setMontoDepositosUSD(totalDepositosUSD);
        estadoCuenta.setMontoRetirosUSD(totalRetirosUSD);

        // Obtener las comisiones
        Comision comision = cuenta.getComision();
        if (comision != null) {
            estadoCuenta.setMontoTotalComisiones(comision.getMontoTotal());
            estadoCuenta.setMontoPorRetirosComision(comision.getMontoPorRetiros());
            estadoCuenta.setMontoPorDepositosComision(comision.getMontoPorDepositos());
        }

        return estadoCuenta;
    }

}