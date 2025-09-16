package com.bankxyz.batch.web;

import com.bankxyz.batch.model.LegacyTransaction;
import com.bankxyz.batch.repository.LegacyTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private LegacyTransactionRepository legacyTransactionRepository;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<List<LegacyTransaction>> getTransactionsByAccountNumber(@PathVariable String accountNumber) {
        List<LegacyTransaction> transactions = legacyTransactionRepository.findByAccountNumber(accountNumber);
        return ResponseEntity.ok(transactions);
    }
}