/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/springframework/Configuration.java to edit this template
 */
package com.banco.CajerosCardless.config;


import com.banco.CajerosCardless.models.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuiltInUserConfig {

    @Bean
    public User builtInUser() {
        User user = new User();
        user.setUsername("admin");
        user.setRole("BITACORA_VIEWER"); 
        return user;
    }
}


