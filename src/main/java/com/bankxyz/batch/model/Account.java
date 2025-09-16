package com.bankxyz.batch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "account")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    @NotNull(message = "Account number cannot be null")
    @Size(min = 1, max = 50, message = "Account number must be between 1 and 50 characters")
    private String accountNumber;

    @NotNull(message = "Owner name cannot be null")
    @Size(min = 1, max = 100, message = "Owner name must be between 1 and 100 characters")
    private String ownerName;

    @NotNull(message = "Account type cannot be null")
    private String type;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Balance must be non-negative")
    private BigDecimal balance = BigDecimal.ZERO;

    // ✅ NUEVO CAMPO
    @Column(name = "age")
    @Min(value = 0, message = "Age must be non-negative")
    @Max(value = 150, message = "Age must be less than or equal to 150")
    private Integer age;

    // Constructores
    public Account() {}

    public Account(String accountNumber, String ownerName, String type, BigDecimal balance, Integer age) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
        this.type = type;
        this.balance = balance;
        this.age = age;
    }

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

    // ✅ NUEVO GETTER Y SETTER
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    @Override
    public String toString() {
        return String.format("Account{id=%d, accountNumber='%s', ownerName='%s', type='%s', balance=%s, age=%d}", 
            id, accountNumber, ownerName, type, balance, age);
    }
}
