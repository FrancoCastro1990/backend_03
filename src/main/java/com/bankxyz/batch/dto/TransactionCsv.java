package com.bankxyz.batch.dto;

public class TransactionCsv {
    // Campos actualizados para coincidir con CSV real: id,fecha,monto,tipo
    private String id;           // Cambio: era tx_id
    private String fecha;        // Cambio: era tx_date
    private String monto;        // Cambio: era amount
    private String tipo;         // Cambio: era description
    
    // ❌ ELIMINAR: account_number (no existe en CSV real)
    
    public TransactionCsv() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return String.format("TransactionCsv{id='%s', fecha='%s', monto='%s', tipo='%s'}", 
            id, fecha, monto, tipo);
    }
}
