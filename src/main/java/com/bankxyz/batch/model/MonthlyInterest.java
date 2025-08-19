package com.bankxyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "monthly_interest")
public class MonthlyInterest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String monthYear;
    private String accountNumber;
    private BigDecimal interestApplied;
    private BigDecimal finalBalance;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMonthYear() { return monthYear; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public BigDecimal getInterestApplied() { return interestApplied; }
    public void setInterestApplied(BigDecimal interestApplied) { this.interestApplied = interestApplied; }
    public BigDecimal getFinalBalance() { return finalBalance; }
    public void setFinalBalance(BigDecimal finalBalance) { this.finalBalance = finalBalance; }
}
