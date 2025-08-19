package com.bankxyz.batch.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "account")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    private String ownerName;
    private String type;

    private BigDecimal balance = BigDecimal.ZERO;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public java.math.BigDecimal getBalance() { return balance; }
    public void setBalance(java.math.BigDecimal balance) { this.balance = balance; }
}
