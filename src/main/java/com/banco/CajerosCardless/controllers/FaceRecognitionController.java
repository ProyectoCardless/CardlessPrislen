package com.banco.CajerosCardless.controllers;

import com.banco.CajerosCardless.models.User;
import com.banco.CajerosCardless.services.FaceRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

@Controller
public class FaceRecognitionController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private User builtInUser; // Usuario preconstruido

    @GetMapping("/comparar")
    public String showComparisonPage() {
        return "comparar";
    }

    @PostMapping("/comparar")
    public String compareFaceWithSource(Model model) {
        try {
            boolean isSamePerson = faceRecognitionService.compareCapturedImageWithSource();

            if (isSamePerson && builtInUser.getUsername().equals("admin")) {
                return "redirect:/consultarBitacoras"; // Si la autenticación es correcta, redirige a bitácoras
            } else {
                model.addAttribute("resultado", "No hay coincidencia o usuario no autorizado");
            }
        } catch (IOException e) {
            model.addAttribute("error", "Error al procesar las imágenes: " + e.getMessage());
        }
        return "comparar";
    }
}