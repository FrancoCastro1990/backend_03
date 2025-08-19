package com.bankxyz.batch.dto;

public class AccountCsv {
    private String account_number;
    private String owner_name;
    private String type;
    private String balance;

    public String getAccount_number() { return account_number; }
    public void setAccount_number(String account_number) { this.account_number = account_number; }
    public String getOwner_name() { return owner_name; }
    public void setOwner_name(String owner_name) { this.owner_name = owner_name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getBalance() { return balance; }
    public void setBalance(String balance) { this.balance = balance; }
}
