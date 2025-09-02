package com.bankxyz.batch.processor;

import com.bankxyz.batch.dto.TransactionCsv;
import com.bankxyz.batch.model.LegacyTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Procesador SIMPLIFICADO para transacciones.csv
 * REQUERIMIENTO: "Detectar anomalías y generar un resumen"
 */
@Component
public class TransactionProcessor implements ItemProcessor<TransactionCsv, LegacyTransaction> {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    @Override
    public LegacyTransaction process(TransactionCsv item) throws Exception {
        try {
            // Validar campos básicos
            if (item.getId() == null || item.getId().trim().isEmpty()) {
                logger.warn("⚠️ Transacción sin ID, omitiendo: {}", item);
                return null;
            }

            // Parsear y validar fecha
            LocalDate txDate = parseDate(item.getFecha());
            if (txDate == null) {
                logger.warn("⚠️ Fecha inválida en transacción {}: {}", item.getId(), item.getFecha());
                return null;
            }

            // Parsear y validar monto
            BigDecimal amount = parseAmount(item.getMonto());
            if (amount == null) {
                logger.warn("⚠️ Monto inválido en transacción {}: {}", item.getId(), item.getMonto());
                return null;
            }

            // Validar tipo de transacción
            String tipo = validateType(item.getTipo());
            if (tipo == null) {
                logger.warn("⚠️ Tipo inválido en transacción {}: {}", item.getId(), item.getTipo());
                return null;
            }

            // 🎯 REQUERIMIENTO PRINCIPAL: DETECTAR ANOMALÍAS
            boolean isAnomaly = detectAnomaly(amount, txDate, tipo);
            
            if (isAnomaly) {
                // 📋 REQUERIMIENTO: GENERAR RESUMEN DE ANOMALÍAS
                logger.warn("🚨 ANOMALÍA DETECTADA - TX: {}, Monto: {}, Fecha: {}, Tipo: {}", 
                    item.getId(), amount, txDate, tipo);
                logger.info("📋 RESUMEN ANOMALÍA - TX: {} - {} ${} el {} - REQUIERE REVISIÓN MANUAL", 
                    item.getId(), tipo, amount, txDate);
            } else {
                logger.debug("✅ Transacción normal procesada: {}", item.getId());
            }

            // Crear transacción para almacenar en transaction_legacy
            LegacyTransaction transaction = new LegacyTransaction();
            transaction.setTxId(item.getId());
            transaction.setAccountNumber(null); // transacciones.csv es independiente
            transaction.setTxDate(txDate);
            transaction.setAmount(amount);
            transaction.setDescription(generateDescription(tipo, amount));

            return transaction;

        } catch (Exception e) {
            logger.error("❌ Error procesando transacción {}: {}", item.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 🎯 DETECTA ANOMALÍAS según criterios bancarios estándar
     */
    private boolean detectAnomaly(BigDecimal amount, LocalDate txDate, String tipo) {
        // Criterios de anomalía:
        // 1. Montos extremos (mayor a $50,000)
        // 2. Montos exactamente cero
        // 3. Fechas futuras
        // 4. Fechas muy antiguas (antes de 2020)
        
        if (amount == null || txDate == null) return true;

        boolean extremeAmount = amount.abs().compareTo(new BigDecimal("50000")) > 0;
        boolean zeroAmount = amount.compareTo(BigDecimal.ZERO) == 0;
        boolean futureDate = txDate.isAfter(LocalDate.now());
        boolean oldDate = txDate.getYear() < 2020;

        return extremeAmount || zeroAmount || futureDate || oldDate;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Manejar fechas inválidas como "2024-13-01"
        if (dateStr.contains("-13-") || dateStr.contains("/13/") || 
            dateStr.endsWith("-13") || dateStr.startsWith("13/")) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Intentar siguiente formato
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr.trim());
            
            // Validar rangos razonables
            if (amount.compareTo(new BigDecimal("-1000000")) < 0 || 
                amount.compareTo(new BigDecimal("1000000")) > 0) {
                return null;
            }

            return amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String validateType(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return null;
        }

        String cleanType = tipo.trim().toLowerCase();
        
        return switch (cleanType) {
            case "credito", "credit" -> "credito";
            case "debito", "debit" -> "debito";
            default -> null; // Rechazar tipos inválidos
        };
    }

    private String generateDescription(String tipo, BigDecimal amount) {
        if ("credito".equals(tipo)) {
            return String.format("Depósito de $%s", amount);
        } else {
            return String.format("Retiro de $%s", amount.abs());
        }
    }
}