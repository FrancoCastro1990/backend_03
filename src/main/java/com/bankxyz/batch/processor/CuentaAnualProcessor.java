package com.bankxyz.batch.processor;

import com.bankxyz.batch.dto.CuentaAnualCsv;
import com.bankxyz.batch.model.AnnualAccountData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Procesador SIMPLIFICADO para cuentas_anuales.csv
 * REQUERIMIENTO: "Compilar datos anuales para cada cuenta y generar un informe detallado para auditorías"
 */
@Component
public class CuentaAnualProcessor implements ItemProcessor<CuentaAnualCsv, AnnualAccountData> {
    
    private static final Logger logger = LoggerFactory.getLogger(CuentaAnualProcessor.class);
    
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    @Override
    public AnnualAccountData process(CuentaAnualCsv item) throws Exception {
        try {
            // Validar campos básicos
            if (item.getCuenta_id() == null || item.getCuenta_id().trim().isEmpty()) {
                logger.warn("⚠️ Registro sin número de cuenta, omitiendo: {}", item);
                return null;
            }

            // Parsear y validar fecha
            LocalDate fecha = parseDate(item.getFecha());
            if (fecha == null) {
                logger.warn("⚠️ Fecha inválida para cuenta {}: {}", item.getCuenta_id(), item.getFecha());
                return null;
            }

            // Extraer año
            Integer year = fecha.getYear();
            
            // Validar año razonable
            if (year < 2020 || year > LocalDate.now().getYear()) {
                logger.warn("⚠️ Año inválido para cuenta {}: {}", item.getCuenta_id(), year);
                return null;
            }

            // Parsear monto (para cuentas anuales solo hay un monto)
            BigDecimal monto = parseAmount(item.getMonto());
            String transaccion = item.getTransaccion();
            
            // Inicializar depósitos y retiros
            BigDecimal depositos = BigDecimal.ZERO;
            BigDecimal retiros = BigDecimal.ZERO;
            
            // Analizar tipo de transacción para clasificar como depósito o retiro
            if (transaccion != null && monto != null) {
                String tipoLimpio = transaccion.trim().toLowerCase();
                switch (tipoLimpio) {
                    case "deposito", "depósito", "compra" -> depositos = monto.abs();
                    case "retiro", "pago" -> retiros = monto.abs();
                }
            }

            // Calcular balance de cierre (simplificado)
            BigDecimal openingBalance = BigDecimal.ZERO; // No disponible en CSV
            BigDecimal closingBalance = depositos.subtract(retiros);

            // 🎯 REQUERIMIENTO: GENERAR INFORME DETALLADO PARA AUDITORÍAS
            logger.info("📊 INFORME ANUAL - Cuenta: {}, Año: {}, Depósitos: ${}, Retiros: ${}, Balance final: ${}", 
                item.getCuenta_id(), year, depositos, retiros, closingBalance);
            
            // Log detallado para auditoría
            if (depositos.compareTo(new BigDecimal("100000")) > 0 || retiros.compareTo(new BigDecimal("100000")) > 0) {
                logger.warn("🔍 AUDITORÍA - Cuenta {} requiere revisión: Movimientos altos en {}", item.getCuenta_id(), year);
            }

            // Crear datos anuales compilados usando campos directos
            AnnualAccountData annualData = new AnnualAccountData();
            annualData.year = year;
            annualData.accountNumber = item.getCuenta_id();
            annualData.openingBalance = openingBalance;
            annualData.totalDeposits = depositos;
            annualData.totalWithdrawals = retiros;
            annualData.closingBalance = closingBalance;
            annualData.auditDate = LocalDate.now();

            return annualData;

        } catch (Exception e) {
            logger.error("❌ Error procesando datos anuales cuenta {}: {}", item.getCuenta_id(), e.getMessage());
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // Manejar fechas inválidas
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
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(amountStr.trim()).abs(); // Usar valor absoluto para evitar negativos
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}