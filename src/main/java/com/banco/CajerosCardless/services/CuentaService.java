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
import com.banco.CajerosCardless.repositories.ClienteRepository;
import com.banco.CajerosCardless.repositories.ComisionRepository;
import com.banco.CajerosCardless.repositories.CuentaRepository;
import com.banco.CajerosCardless.repositories.TransaccionRepository;
import com.banco.CajerosCardless.utils.AESCipher;
import com.banco.CajerosCardless.utils.RandomWordGenerator;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import io.jsonwebtoken.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
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

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
@Service
public class CuentaService {

    @Autowired
    private CuentaRepository cuentaRepository;
    private static final int TRANSACCIONES_GRATIS = 3;
    private static final BigDecimal COMISION = new BigDecimal("0.05");

    private final SmsService smsService;
    private final Map<String, String> verificationMap = new HashMap<>();
    private final Map<String, Integer> failedAttemptsMap = new HashMap<>();

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
    private static final BigDecimal TASA_CAMBIO_USD = BigDecimal.valueOf(0.0018);

     @Autowired
    public CuentaService(SmsService smsService, CuentaRepository cuentaRepository) {
        this.smsService = smsService;
        this.cuentaRepository = cuentaRepository;
    }

    public Cuenta obtenerCuentaPorNumero(String numeroCuenta) {
        // Find the account by its number
        return cuentaRepository.findByNumeroCuenta(numeroCuenta);
    }

    public List<Cuenta> obtenerCuentasPorCliente(Cliente cliente) {
        // Busca todas las cuentas que pertenecen al cliente
        return cuentaRepository.findByClienteId(cliente.getId());
    }

    public EstadoCuenta consultarEstadoCuenta(String numeroCuenta, String pin) {
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
    
    public void enviarPdfPorCorreo(EstadoCuenta estadoCuenta, String email) {
        // Create PDF
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, pdfOutputStream);
            document.open();

            // Add content to the PDF
            document.add(new Paragraph("Estado de Cuenta"));
            document.add(new Paragraph("Número de Cuenta: " + estadoCuenta.getNumeroCuenta()));
            document.add(new Paragraph("Saldo: " + estadoCuenta.getSaldo()));
            document.add(new Paragraph("Estatus: " + estadoCuenta.getEstatus()));
            document.add(new Paragraph("Fecha de Creación: " + estadoCuenta.getFechaCreacion()));
            // Add more details as needed from EstadoCuenta

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        // Send email
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("proyectocardless@gmail.com", "proyectocardless1234");
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("proyectocardless@gmail.com"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject("Estado de Cuenta");

            // Create the email body part
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Adjunto encontrará su estado de cuenta.");

            // Create the attachment body part
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setDataHandler(
                    new DataHandler(new ByteArrayDataSource(pdfOutputStream.toByteArray(), "application/pdf")));
            attachmentBodyPart.setFileName("EstadoCuenta.pdf");

            // Create the multipart message
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentBodyPart);

            // Set the complete message parts
            message.setContent(multipart);

            // Send the message
            Transport.send(message);
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
            // Handle the exception appropriately
        }
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

    public Cuenta depositar(String numeroCuenta, BigDecimal monto, String pinIngresado) {
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
                    monto = monto.subtract(comision); // Reduce deposited amount
                }

                // Convert to USD if required
                BigDecimal crcRate = exchangeRateService.getCRCExchangeRate();
                if (crcRate != null) {
                    @SuppressWarnings("deprecation")
                    BigDecimal montoUSD = monto.divide(crcRate, BigDecimal.ROUND_HALF_EVEN);
                    System.out.println("Deposit amount in USD: " + montoUSD);
                }

                // Update the account balance
                cuenta.setSaldo(cuenta.getSaldo().add(monto));
                cuentaRepository.save(cuenta);

                // Record the transaction
                Transaccion transaccion = new Transaccion();
                transaccion.setCuenta(cuenta);
                transaccion.setTipo(Transaccion.Tipo.Deposito);
                transaccion.setMonto(monto);
                transaccion.setComisionAplicada(comision);
                transaccionRepository.save(transaccion);

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

        // Crear el PDF
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, pdfOutputStream);
            document.open();

            // Añadir contenido al PDF
            document.add(new Paragraph("Estado de Cuenta"));
            document.add(new Paragraph("Número de Cuenta: " + estadoCuenta.getNumeroCuenta()));
            document.add(new Paragraph("Saldo: " + estadoCuenta.getSaldo()));
            document.add(new Paragraph("Estatus: " + estadoCuenta.getEstatus()));
            document.add(new Paragraph("Fecha de Creación: " + estadoCuenta.getFechaCreacion()));
            // Añadir más detalles según sea necesario

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar el PDF.");
        }

        // Enviar el correo con el PDF adjunto
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("proyectocardless@gmail.com", "voin wncz yxra pipr");
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("proyectocardless@gmail.com"));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject("Estado de Cuenta");

            // Crear la parte de cuerpo del mensaje
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Adjunto encontrará su estado de cuenta.");

            // Crear la parte del adjunto
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setDataHandler(
                    new DataHandler(new ByteArrayDataSource(pdfOutputStream.toByteArray(), "application/pdf")));
            attachmentBodyPart.setFileName("EstadoCuenta.pdf");

            // Crear el mensaje multipart
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentBodyPart);

            // Setear el contenido completo del mensaje
            message.setContent(multipart);

            // Enviar el mensaje
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al enviar el correo.");
        }
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
            BigDecimal montoDolares) {
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

        // Guardar la transacción de retiro
        Transaccion transaccion = new Transaccion();
        transaccion.setCuenta(cuenta);
        transaccion.setTipo(Transaccion.Tipo.Retiro);
        transaccion.setMonto(montoColones);
        transaccion.setComisionAplicada(comision);
        transaccionRepository.save(transaccion);

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
            BigDecimal monto, String numeroCuentaDestino) {
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
            monto = monto.add(comision);
        }

        // Realizar transferencia
        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(monto));
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(monto.subtract(comision)));

        cuentaRepository.save(cuentaOrigen);
        cuentaRepository.save(cuentaDestino);

        // Registrar transacción
        Transaccion transaccion = new Transaccion();
        transaccion.setCuenta(cuentaOrigen);
        transaccion.setTipo(Transaccion.Tipo.Transf);
        transaccion.setMonto(monto);
        transaccion.setComisionAplicada(comision);
        transaccionRepository.save(transaccion);

        // Eliminar palabra clave
        palabraClaveService.eliminarPalabraClave(numeroCuentaOrigen);

        // Respuesta
        Map<String, Object> response = new HashMap<>();
        response.put("saldoOrigen", cuentaOrigen.getSaldo());
        response.put("montoTransferido", monto.subtract(comision));
        response.put("comision", comision);

        return response;
    }


    public Cuenta verificarPalabraYRetirar(String numeroCuenta, String palabraIngresada, BigDecimal monto) {
        String palabraClaveAlmacenada = palabraClaveService.obtenerPalabraClave(numeroCuenta);
        if (palabraClaveAlmacenada == null || !palabraClaveAlmacenada.equals(palabraIngresada)) {
            throw new IllegalArgumentException("La palabra ingresada es incorrecta.");
        }

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta);
        if (cuenta == null) {
            throw new IllegalArgumentException("Cuenta no encontrada.");
        }

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("Fondos insuficientes.");
        }
        BigDecimal comision = BigDecimal.ZERO;
        long totalTransacciones = transaccionRepository.countByCuenta(cuenta);
        if (totalTransacciones >= TRANSACCIONES_GRATIS) {
            comision = monto.multiply(COMISION); 
            monto = monto.add(comision); 
        }
        Transaccion transaccion = new Transaccion();
        transaccion.setCuenta(cuenta);
        transaccion.setTipo(Transaccion.Tipo.Retiro);
        transaccion.setMonto(monto);
        transaccion.setComisionAplicada(comision);
        transaccionRepository.save(transaccion);

        // Aplicar lógica de comisiones y realizar el retiro
        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuentaRepository.save(cuenta);

        // Registrar transacción y eliminar palabra clave
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