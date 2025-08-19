package com.bankxyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "annual_statement")
public class AnnualStatement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer year;
    private String accountNumber;
    private BigDecimal openingBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal closingBalance;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(BigDecimal totalDeposits) { this.totalDeposits = totalDeposits; }
    public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
    public void setTotalWithdrawals(BigDecimal totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
}
