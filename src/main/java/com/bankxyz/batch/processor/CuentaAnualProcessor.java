package com.bankxyz.batch.processor;

import com.bankxyz.batch.dto.CuentaAnualCsv;
import com.bankxyz.batch.model.AnnualStatement;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Procesador para cuentas anuales que maneja datos problemáticos
 */
@Component
public class CuentaAnualProcessor implements ItemProcessor<CuentaAnualCsv, AnnualStatement> {
    
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^\\d+$");
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };
    
    @Override
    public AnnualStatement process(CuentaAnualCsv item) throws Exception {
        if (item == null) {
            return null;
        }
        
        // Validaciones y limpieza de datos
        if (!isValidRecord(item)) {
            System.err.println("SKIPPING - Registro inválido: " + item);
            return null;
        }
        
        AnnualStatement statement = new AnnualStatement();
        
        // Limpiar y convertir cuenta_id
        String accountNumber = cleanAccountNumber(item.getCuenta_id());
        statement.setAccountNumber(accountNumber);
        
        // Parsear fecha y extraer año
        LocalDate date = parseDate(item.getFecha());
        if (date != null) {
            statement.setYear(date.getYear());
        } else {
            statement.setYear(2024); // Año por defecto
        }
        
        // Parsear monto
        BigDecimal amount = parseAmount(item.getMonto());
        
        // Clasificar transacción y asignar montos
        String tipoTransaccion = item.getTransaccion() != null ? 
                                item.getTransaccion().toLowerCase().trim() : "";
        
        statement.setOpeningBalance(BigDecimal.ZERO); // Se calculará en agregación
        
        if (amount.compareTo(BigDecimal.ZERO) >= 0 && 
            (tipoTransaccion.contains("deposito") || tipoTransaccion.contains("ingreso"))) {
            statement.setTotalDeposits(amount);
            statement.setTotalWithdrawals(BigDecimal.ZERO);
        } else if (amount.compareTo(BigDecimal.ZERO) < 0 || 
                   tipoTransaccion.contains("retiro") || tipoTransaccion.contains("compra")) {
            statement.setTotalDeposits(BigDecimal.ZERO);
            statement.setTotalWithdrawals(amount.abs());
        } else {
            // Caso neutro
            statement.setTotalDeposits(amount.max(BigDecimal.ZERO));
            statement.setTotalWithdrawals(amount.min(BigDecimal.ZERO).abs());
        }
        
        statement.setClosingBalance(BigDecimal.ZERO); // Se calculará en agregación
        
        // Log para debugging
        System.out.println(String.format("Cuenta anual procesada: %s - %d - %s - %s", 
            accountNumber, statement.getYear(), tipoTransaccion, amount));
        
        return statement;
    }
    
    private boolean isValidRecord(CuentaAnualCsv item) {
        // Validar cuenta_id
        if (item.getCuenta_id() == null || item.getCuenta_id().trim().isEmpty()) {
            return false;
        }
        
        // Validar fecha
        if (item.getFecha() == null || item.getFecha().trim().isEmpty()) {
            return false;
        }
        
        // Validar que tenga al menos tipo de transacción o monto
        boolean hasTipo = item.getTransaccion() != null && !item.getTransaccion().trim().isEmpty();
        boolean hasMonto = item.getMonto() != null && !item.getMonto().trim().isEmpty();
        
        return hasTipo || hasMonto;
    }
    
    private String cleanAccountNumber(String cuentaId) {
        if (cuentaId == null) return "UNKNOWN";
        
        String cleaned = cuentaId.trim();
        
        // Si es solo un número, convertir a formato ACC###
        if (ACCOUNT_PATTERN.matcher(cleaned).matches()) {
            return "ACC" + cleaned;
        }
        
        return cleaned;
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = dateStr.trim();
        
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException e) {
                // Continuar con el siguiente formato
            }
        }
        
        System.err.println("Error parsing date: " + dateStr + " - usando fecha actual");
        return LocalDate.now();
    }
    
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            // Limpiar el string de espacios y caracteres no numéricos excepto . y -
            String cleaned = amountStr.trim().replaceAll("[^\\d.-]", "");
            if (cleaned.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing amount: " + amountStr + " - usando 0.0");
            return BigDecimal.ZERO;
        }
    }
}