package com.bankxyz.batch.dto;

public class AccountCsv {
    // Campos actualizados para coincidir con CSV real: cuenta_id,nombre,saldo,edad,tipo
    private String cuenta_id;    // Cambio: era account_number
    private String nombre;       // Cambio: era owner_name
    private String saldo;        // Cambio: era balance
    private String edad;         // ✅ NUEVO CAMPO
    private String tipo;         // Sin cambio

    public AccountCsv() {}

    // Getters y Setters
    public String getCuenta_id() { return cuenta_id; }
    public void setCuenta_id(String cuenta_id) { this.cuenta_id = cuenta_id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getSaldo() { return saldo; }
    public void setSaldo(String saldo) { this.saldo = saldo; }

    public String getEdad() { return edad; }
    public void setEdad(String edad) { this.edad = edad; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return String.format("AccountCsv{cuenta_id='%s', nombre='%s', saldo='%s', edad='%s', tipo='%s'}", 
            cuenta_id, nombre, saldo, edad, tipo);
    }
}
