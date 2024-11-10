/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.banco.CajerosCardless.services;

import java.io.ByteArrayOutputStream;

public interface PDFGenerator {
    ByteArrayOutputStream generatePDF(String content);
}
