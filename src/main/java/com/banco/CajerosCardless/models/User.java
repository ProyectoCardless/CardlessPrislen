package com.banco.CajerosCardless.models;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    
    private String username;
    private String role;
    
    public User() {}

    public User(String username, String role) {
        this.username = username;
        this.role = role;
    }
    
    // Método de verificación de permisos para bitácora
    public boolean hasBitacoraAccess() {
        return "BITACORA_VIEWER".equals(role);
    }
}

