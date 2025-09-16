package com.bankxyz.batch.repository;

import com.bankxyz.batch.model.LegacyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegacyTransactionRepository extends JpaRepository<LegacyTransaction, Long> {
    List<LegacyTransaction> findByAccountNumber(String accountNumber);
}
