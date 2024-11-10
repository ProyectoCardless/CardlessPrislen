/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.banco.CajerosCardless.services;

import java.io.ByteArrayOutputStream;

import com.banco.CajerosCardless.models.EstadoCuenta;
import com.banco.CajerosCardless.models.Transaccion;
import com.banco.CajerosCardless.utils.OpenAITranslationService;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import java.io.OutputStream;
import java.util.List;

public class TranslatedPDFGenerator extends BasicPDFGenerator {

    private OpenAITranslationService translationService;

    public TranslatedPDFGenerator(OpenAITranslationService translationService) {
        this.translationService = translationService;
    }

     public void generateTranslatedPDF(EstadoCuenta estadoCuenta, List<Transaccion> transacciones, OutputStream outputStream, String language) throws DocumentException {
        Document document = new Document();
        PdfWriter.getInstance(document, outputStream);
        document.open();

        // Información traducida del estado de cuenta
        document.add(new Paragraph(translationService.translate("Estado de Cuenta", language)));
        document.add(new Paragraph(translationService.translate("Número de Cuenta", language) + ": " + estadoCuenta.getNumeroCuenta()));
        document.add(new Paragraph(translationService.translate("Saldo", language) + ": " + estadoCuenta.getSaldo()));
        document.add(new Paragraph(translationService.translate("Estatus", language) + ": " + estadoCuenta.getEstatus()));
        document.add(new Paragraph(translationService.translate("Fecha de Creación", language) + ": " + estadoCuenta.getFechaCreacion()));

        // Añadir las transacciones traducidas
        document.add(new Paragraph("\n" + translationService.translate("Transacciones", language) + ":"));
        for (Transaccion transaccion : transacciones) {
            document.add(new Paragraph(translationService.translate("Tipo", language) + ": " + translationService.translate(transaccion.getTipo().name(), language)));
            document.add(new Paragraph(translationService.translate("Monto", language) + ": " + transaccion.getMonto()));
            document.add(new Paragraph(translationService.translate("Fecha", language) + ": " + transaccion.getFecha()));
            document.add(new Paragraph(translationService.translate("Comisión Aplicada", language) + ": " + transaccion.getComisionAplicada()));
            document.add(new Paragraph("---------------------------"));
        }

        document.close();
    }
}