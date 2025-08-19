package com.bankxyz.batch.dto;

public class TransactionCsv {
    private String tx_id;
    private String account_number;
    private String tx_date; // yyyy-MM-dd
    private String description;
    private String amount;

    public String getTx_id() { return tx_id; }
    public void setTx_id(String tx_id) { this.tx_id = tx_id; }
    public String getAccount_number() { return account_number; }
    public void setAccount_number(String account_number) { this.account_number = account_number; }
    public String getTx_date() { return tx_date; }
    public void setTx_date(String tx_date) { this.tx_date = tx_date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
}
