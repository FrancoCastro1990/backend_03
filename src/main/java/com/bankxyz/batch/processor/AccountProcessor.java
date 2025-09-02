package com.bankxyz.batch.processor;

import com.bankxyz.batch.dto.AccountCsv;
import com.bankxyz.batch.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Procesador SIMPLIFICADO para intereses.csv
 * REQUERIMIENTO: "Aplicar intereses sobre cuentas y actualizar el saldo final en base de datos"
 */
@Component
public class AccountProcessor implements ItemProcessor<AccountCsv, Account> {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountProcessor.class);
    
    // Tasas de interés por tipo de cuenta
    private static final BigDecimal SAVINGS_RATE = new BigDecimal("0.025"); // 2.5% anual
    private static final BigDecimal CHECKING_RATE = new BigDecimal("0.01"); // 1% anual
    private static final BigDecimal BUSINESS_RATE = new BigDecimal("0.035"); // 3.5% anual

    @Override
    public Account process(AccountCsv item) throws Exception {
        try {
            // Validar campos básicos
            if (item.getCuenta_id() == null || item.getCuenta_id().trim().isEmpty()) {
                logger.warn("⚠️ Cuenta sin número, omitiendo: {}", item);
                return null;
            }

            // Validar balance inicial
            BigDecimal initialBalance = parseBalance(item.getSaldo());
            if (initialBalance == null) {
                logger.warn("⚠️ Balance inválido para cuenta {}: {}", item.getCuenta_id(), item.getSaldo());
                return null;
            }

            // Validar tipo de cuenta
            String accountType = validateAccountType(item.getTipo());
            if (accountType == null) {
                logger.warn("⚠️ Tipo de cuenta inválido {}: {}", item.getCuenta_id(), item.getTipo());
                return null;
            }

            // Validar edad
            Integer age = parseAge(item.getEdad());
            if (age != null && (age < 0 || age > 150)) {
                logger.warn("⚠️ Edad inválida para cuenta {}: {}", item.getCuenta_id(), age);
                return null;
            }

            // 🎯 REQUERIMIENTO PRINCIPAL: CALCULAR Y APLICAR INTERESES
            BigDecimal interestRate = getInterestRate(accountType);
            BigDecimal monthlyInterest = calculateMonthlyInterest(initialBalance, interestRate);
            BigDecimal finalBalance = initialBalance.add(monthlyInterest);

            logger.info("💰 INTERÉS CALCULADO - Cuenta: {}, Balance inicial: ${}, Interés: ${}, Balance final: ${}", 
                item.getCuenta_id(), initialBalance, monthlyInterest, finalBalance);

            // Crear cuenta con balance actualizado
            Account account = new Account();
            account.setAccountNumber(item.getCuenta_id());
            account.setOwnerName(item.getNombre());
            account.setType(accountType);
            account.setBalance(finalBalance); // ✅ SALDO FINAL ACTUALIZADO
            account.setAge(age);

            return account;

        } catch (Exception e) {
            logger.error("❌ Error procesando cuenta {}: {}", item.getCuenta_id(), e.getMessage());
            return null;
        }
    }

    /**
     * 🎯 CALCULA INTERÉS MENSUAL según tipo de cuenta
     */
    private BigDecimal calculateMonthlyInterest(BigDecimal balance, BigDecimal annualRate) {
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO; // No aplicar intereses a balances negativos o cero
        }

        // Interés mensual = (balance * tasa anual) / 12
        BigDecimal monthlyRate = annualRate.divide(new BigDecimal("12"), 6, RoundingMode.HALF_UP);
        return balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 🎯 OBTIENE TASA DE INTERÉS según tipo de cuenta
     */
    private BigDecimal getInterestRate(String accountType) {
        return switch (accountType) {
            case "savings" -> SAVINGS_RATE;
            case "checking" -> CHECKING_RATE;
            case "business" -> BUSINESS_RATE;
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal parseBalance(String balanceStr) {
        if (balanceStr == null || balanceStr.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(balanceStr.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String validateAccountType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }

        String cleanType = type.trim().toLowerCase();
        
        return switch (cleanType) {
            case "savings", "ahorro" -> "savings";
            case "checking", "corriente" -> "checking";
            case "business", "empresarial" -> "business";
            default -> null; // Rechazar tipos inválidos
        };
    }

    private Integer parseAge(String ageStr) {
        if (ageStr == null || ageStr.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(ageStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}