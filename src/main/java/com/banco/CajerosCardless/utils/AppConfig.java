/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/springframework/Configuration.java to edit this template
 */
package com.banco.CajerosCardless.utils;

import com.banco.CajerosCardless.services.TranslatedPDFGenerator;
import com.banco.CajerosCardless.utils.OpenAITranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.banco.CajerosCardless.observers.CsvLogObserver;
import com.banco.CajerosCardless.observers.JsonLogObserver;
import com.banco.CajerosCardless.observers.PositionalLogObserver;
import com.banco.CajerosCardless.services.UserActionLogger;
@Configuration
public class AppConfig {

    @Bean
    public OpenAITranslationService openAITranslationService() {
        return new OpenAITranslationService();
    }

    @Bean
    public TranslatedPDFGenerator translatedPDFGenerator() {
        return new TranslatedPDFGenerator(openAITranslationService());
    }
     @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
    
    @Bean
    public UserActionLogger userActionLogger(ObjectMapper objectMapper) {  
        UserActionLogger logger = new UserActionLogger();
        logger.addObserver(new JsonLogObserver(objectMapper)); 
        logger.addObserver(new CsvLogObserver());
        logger.addObserver(new PositionalLogObserver());
        return logger;
    }
}

