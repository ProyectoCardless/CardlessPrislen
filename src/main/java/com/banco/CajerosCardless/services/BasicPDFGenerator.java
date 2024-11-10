/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.services;

import com.banco.CajerosCardless.models.EstadoCuenta;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.OutputStream;
import com.banco.CajerosCardless.models.Transaccion;
import java.util.List;

public class BasicPDFGenerator {

    public void generatePDF(EstadoCuenta estadoCuenta, List<Transaccion> transacciones, OutputStream outputStream)
            throws DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // Añadir información básica del estado de cuenta
        document.add(new Paragraph("Estado de Cuenta"));
        document.add(new Paragraph("Número de Cuenta: " + estadoCuenta.getNumeroCuenta()));
        document.add(new Paragraph("Saldo: " + estadoCuenta.getSaldo()));
        document.add(new Paragraph("Estatus: " + estadoCuenta.getEstatus()));
        document.add(new Paragraph("Fecha de Creación: " + estadoCuenta.getFechaCreacion()));

        // Añadir la lista de transacciones
        document.add(new Paragraph("\nTransacciones:"));
        for (Transaccion transaccion : transacciones) {
            document.add(new Paragraph("Tipo: " + transaccion.getTipo()));
            document.add(new Paragraph("Monto: " + transaccion.getMonto()));
            document.add(new Paragraph("Fecha: " + transaccion.getFecha()));
            document.add(new Paragraph("Comisión Aplicada: " + transaccion.getComisionAplicada()));
            document.add(new Paragraph("---------------------------"));
        }

        document.close();
    }
}
