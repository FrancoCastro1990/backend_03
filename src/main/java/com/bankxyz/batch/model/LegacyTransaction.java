package com.bankxyz.batch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_legacy")
public class LegacyTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Transaction ID cannot be null")
    @Size(min = 1, max = 50, message = "Transaction ID must be between 1 and 50 characters")
    private String txId;

    @Size(max = 50, message = "Account number must be less than or equal to 50 characters")
    private String accountNumber;

    @NotNull(message = "Transaction date cannot be null")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate txDate;

    @Size(max = 255, message = "Description must be less than or equal to 255 characters")
    private String description;

    @NotNull(message = "Amount cannot be null")
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
