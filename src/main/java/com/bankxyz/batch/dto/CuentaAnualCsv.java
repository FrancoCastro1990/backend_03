package com.bankxyz.batch.dto;

/**
 * DTO para archivos CSV de cuentas anuales
 * Formato: cuenta_id,fecha,transaccion,monto,descripcion
 */
public class CuentaAnualCsv {
    private String cuenta_id;
    private String fecha;
    private String transaccion;
    private String monto;
    private String descripcion;
    
    // Constructors
    public CuentaAnualCsv() {}
    
    public CuentaAnualCsv(String cuenta_id, String fecha, String transaccion, String monto, String descripcion) {
        this.cuenta_id = cuenta_id;
        this.fecha = fecha;
        this.transaccion = transaccion;
        this.monto = monto;
        this.descripcion = descripcion;
    }
    
    // Getters and Setters
    public String getCuenta_id() {
        return cuenta_id;
    }
    
    public void setCuenta_id(String cuenta_id) {
        this.cuenta_id = cuenta_id;
    }
    
    public String getFecha() {
        return fecha;
    }
    
    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
    
    public String getTransaccion() {
        return transaccion;
    }
    
    public void setTransaccion(String transaccion) {
        this.transaccion = transaccion;
    }
    
    public String getMonto() {
        return monto;
    }
    
    public void setMonto(String monto) {
        this.monto = monto;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    @Override
    public String toString() {
        return String.format("CuentaAnualCsv{cuenta_id='%s', fecha='%s', transaccion='%s', monto='%s', descripcion='%s'}",
                cuenta_id, fecha, transaccion, monto, descripcion);
    }
}