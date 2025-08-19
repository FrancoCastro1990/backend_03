package com.bankxyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_transaction_report")
public class DailyTransactionReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate reportDate;
    private String accountNumber;
    private Integer txCount;
    private BigDecimal totalAmount;
    private Integer anomalies;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public Integer getTxCount() { return txCount; }
    public void setTxCount(Integer txCount) { this.txCount = txCount; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getAnomalies() { return anomalies; }
    public void setAnomalies(Integer anomalies) { this.anomalies = anomalies; }
}
