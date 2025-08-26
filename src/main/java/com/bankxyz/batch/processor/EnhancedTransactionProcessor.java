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
import java.util.regex.Pattern;

/**
 * Procesador mejorado de transacciones con validaciones exhaustivas de negocio.
 * Implementa detección de anomalías y reglas de consistencia.
 */
@Component
public class EnhancedTransactionProcessor implements ItemProcessor<TransactionCsv, LegacyTransaction> {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedTransactionProcessor.class);
    
    // Patrones y constantes de validación
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^ACC\\d{3,10}$");
    private static final Pattern TX_ID_PATTERN = Pattern.compile("^[A-Z0-9]{6,20}$");
    
    // Límites de negocio para detección de anomalías
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00"); // 1M
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("-1000000.00"); // -1M
    private static final BigDecimal SUSPICIOUS_AMOUNT_THRESHOLD = new BigDecimal("100000.00"); // 100K
    
    // Formatters de fecha soportados
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };
    
    @Override
    public LegacyTransaction process(TransactionCsv item) throws Exception {
        
        // 1. Validación de campos obligatorios
        validateRequiredFields(item);
        
        // 2. Normalización y validación de ID de transacción
        String txId = normalizeTxId(item.getTx_id());
        validateTxId(txId);
        
        // 3. Normalización y validación de número de cuenta
        String accountNumber = normalizeAccountNumber(item.getAccount_number());
        validateAccountNumber(accountNumber);
        
        // 4. Parsing y validación de fecha
        LocalDate txDate = parseAndValidateDate(item.getTx_date());
        
        // 5. Parsing y validación de monto
        BigDecimal amount = parseAndValidateAmount(item.getAmount());
        
        // 6. Validación de descripción
        String description = normalizeDescription(item.getDescription());
        
        // 7. Detección de anomalías
        detectAnomalies(txId, accountNumber, amount, txDate);
        
        // 8. Crear entidad
        LegacyTransaction transaction = new LegacyTransaction();
        transaction.setTxId(txId);
        transaction.setAccountNumber(accountNumber);
        transaction.setTxDate(txDate);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        
        logger.debug("Transacción procesada: {} - {} - {} - {}", 
            txId, accountNumber, amount, txDate);
        
        return transaction;
    }
    
    private void validateRequiredFields(TransactionCsv item) {
        if (item.getTx_id() == null || item.getTx_id().trim().isEmpty()) {
            throw new IllegalArgumentException("ID de transacción es obligatorio");
        }
        if (item.getAccount_number() == null || item.getAccount_number().trim().isEmpty()) {
            throw new IllegalArgumentException("Número de cuenta es obligatorio");
        }
        if (item.getTx_date() == null || item.getTx_date().trim().isEmpty()) {
            throw new IllegalArgumentException("Fecha de transacción es obligatoria");
        }
        if (item.getAmount() == null || item.getAmount().trim().isEmpty()) {
            throw new IllegalArgumentException("Monto de transacción es obligatorio");
        }
    }
    
    private String normalizeTxId(String txId) {
        return txId.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
    
    private void validateTxId(String txId) {
        if (!TX_ID_PATTERN.matcher(txId).matches()) {
            throw new IllegalArgumentException("Formato de ID de transacción inválido: " + txId);
        }
    }
    
    private String normalizeAccountNumber(String accountNumber) {
        return accountNumber.trim().toUpperCase();
    }
    
    private void validateAccountNumber(String accountNumber) {
        if (!ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
            throw new IllegalArgumentException("Formato de número de cuenta inválido: " + accountNumber);
        }
    }
    
    private LocalDate parseAndValidateDate(String dateStr) {
        dateStr = dateStr.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                
                // Validar que la fecha esté en un rango razonable
                LocalDate now = LocalDate.now();
                LocalDate minDate = now.minusYears(50);
                LocalDate maxDate = now.plusDays(1);
                
                if (date.isBefore(minDate) || date.isAfter(maxDate)) {
                    throw new IllegalArgumentException("Fecha fuera del rango válido: " + dateStr);
                }
                
                return date;
            } catch (DateTimeParseException e) {
                // Continuar con el siguiente formato
            }
        }
        
        throw new DateTimeParseException("Formato de fecha no soportado: " + dateStr, dateStr, 0);
    }
    
    private BigDecimal parseAndValidateAmount(String amountStr) {
        try {
            // Limpiar el string (remover espacios, comas, símbolos de moneda)
            String cleanAmount = amountStr.trim()
                .replaceAll("[,\\s]", "")
                .replaceAll("[$€£¥]", "");
            
            BigDecimal amount = new BigDecimal(cleanAmount);
            
            // Validar límites
            if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0 || 
                amount.compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
                throw new IllegalArgumentException("Monto fuera de los límites permitidos: " + amount);
            }
            
            return amount;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Formato de monto inválido: " + amountStr);
        }
    }
    
    private String normalizeDescription(String description) {
        if (description == null) {
            return "Sin descripción";
        }
        
        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return "Sin descripción";
        }
        
        // Limitar longitud
        if (normalized.length() > 255) {
            normalized = normalized.substring(0, 255);
        }
        
        return normalized;
    }
    
    private void detectAnomalies(String txId, String accountNumber, BigDecimal amount, LocalDate txDate) {
        // Detectar transacciones sospechosas por monto
        if (amount.abs().compareTo(SUSPICIOUS_AMOUNT_THRESHOLD) > 0) {
            logger.warn("ANOMALÍA DETECTADA - Transacción de monto alto: {} - Cuenta: {} - Monto: {}", 
                txId, accountNumber, amount);
        }
        
        // Detectar transacciones con monto exactamente cero
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            logger.warn("ANOMALÍA DETECTADA - Transacción con monto cero: {} - Cuenta: {}", 
                txId, accountNumber);
        }
        
        // Detectar transacciones en fechas futuras
        if (txDate.isAfter(LocalDate.now())) {
            logger.warn("ANOMALÍA DETECTADA - Transacción en fecha futura: {} - Cuenta: {} - Fecha: {}", 
                txId, accountNumber, txDate);
        }
        
        // Detectar patrones sospechosos en montos (números redondos muy grandes)
        if (isRoundSuspiciousAmount(amount)) {
            logger.warn("ANOMALÍA DETECTADA - Monto redondo sospechoso: {} - Cuenta: {} - Monto: {}", 
                txId, accountNumber, amount);
        }
    }
    
    private boolean isRoundSuspiciousAmount(BigDecimal amount) {
        BigDecimal abs = amount.abs();
        return abs.compareTo(new BigDecimal("10000")) >= 0 && 
               abs.remainder(new BigDecimal("10000")).compareTo(BigDecimal.ZERO) == 0;
    }
}