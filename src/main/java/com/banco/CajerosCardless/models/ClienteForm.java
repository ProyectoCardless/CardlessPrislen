package com.banco.CajerosCardless.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ClienteForm {

    private String fechaNacimiento;
    private String tipoNegocio;
    private String razonSocial;
    private int maxCuentas = 5;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

   
    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

   
    @Pattern(regexp = "^\\d{8,15}$", message = "El teléfono debe contener entre 8 y 15 dígitos")
    private String telefono;

    
    @Email(message = "El correo electrónico no es válido")
    private String correo;

   
    @NotBlank(message = "La identificación es obligatoria")
    private String identificacion;


    public String getIdentificacion() {
        return identificacion;
    }
    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }
    public String getFechaNacimiento() {
        return fechaNacimiento;
    }
    public void setFechaNacimiento(String fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }
    public String getTipoNegocio() {
        return tipoNegocio;
    }
    public void setTipoNegocio(String tipoNegocio) {
        this.tipoNegocio = tipoNegocio;
    }
    public String getRazonSocial() {
        return razonSocial;
    }
    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }
    public int getMaxCuentas() {
        return maxCuentas;
    }
    public void setMaxCuentas(int maxCuentas) {
        this.maxCuentas = maxCuentas;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    public String getCorreo() {
        return correo;
    }
    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
