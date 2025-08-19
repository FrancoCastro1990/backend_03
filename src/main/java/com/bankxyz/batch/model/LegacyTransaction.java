package com.bankxyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_legacy")
public class LegacyTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String txId;
    private String accountNumber;
    private LocalDate txDate;
    private String description;
    private BigDecimal amount;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public LocalDate getTxDate() { return txDate; }
    public void setTxDate(LocalDate txDate) { this.txDate = txDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
