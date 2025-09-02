package com.bankxyz.batch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad para datos anuales compilados de cuentas_anuales.csv
 * REQUERIMIENTO: "Compilar datos anuales para cada cuenta y generar un informe detallado para auditorías"
 */
@Entity
@Table(name = "annual_account_data")
@Data
@NoArgsConstructor
public class AnnualAccountData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    
    @Column(name = "year", nullable = false)
    public Integer year;
    
    @Column(name = "account_number")
    public String accountNumber;
    
    @Column(name = "opening_balance", precision = 19, scale = 2)
    public BigDecimal openingBalance;
    
    @Column(name = "total_deposits", precision = 19, scale = 2)
    public BigDecimal totalDeposits;
    
    @Column(name = "total_withdrawals", precision = 19, scale = 2)
    public BigDecimal totalWithdrawals;
    
    @Column(name = "closing_balance", precision = 19, scale = 2)
    public BigDecimal closingBalance;
    
    @Column(name = "audit_date")
    public LocalDate auditDate;

}