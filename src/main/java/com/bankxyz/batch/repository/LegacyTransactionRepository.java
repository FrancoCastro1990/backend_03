package com.bankxyz.batch.repository;

import com.bankxyz.batch.model.LegacyTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyTransactionRepository extends JpaRepository<LegacyTransaction, Long> {}
