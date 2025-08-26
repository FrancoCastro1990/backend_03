package com.bankxyz.batch.processor;

import com.bankxyz.batch.dto.AccountCsv;
import com.bankxyz.batch.model.Account;
import com.bankxyz.batch.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Procesador mejorado de cuentas con validaciones exhaustivas de negocio.
 * Implementa reglas de consistencia y tolerancia a fallos.
 */
@Component
public class EnhancedAccountProcessor implements ItemProcessor<AccountCsv, Account> {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedAccountProcessor.class);
    
    // Patrones de validación
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^ACC\\d{3,10}$");
    private static final Pattern OWNER_NAME_PATTERN = Pattern.compile("^[a-zA-ZáéíóúñÁÉÍÓÚÑ\\s]{2,100}$");
    
    // Límites de negocio
    private static final BigDecimal MAX_BALANCE = new BigDecimal("10000000.00"); // 10M
    private static final BigDecimal MIN_BALANCE = new BigDecimal("-100000.00"); // -100K (sobregiro permitido)
    
    private final AccountRepository accountRepository;
    
    public EnhancedAccountProcessor(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    @Override
    public Account process(AccountCsv item) throws Exception {
        
        // 1. Validación de campos obligatorios
        if (item.getAccount_number() == null || item.getAccount_number().trim().isEmpty()) {
            throw new IllegalArgumentException("Número de cuenta es obligatorio");
        }
        
        if (item.getOwner_name() == null || item.getOwner_name().trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre del propietario es obligatorio");
        }
        
        if (item.getType() == null || item.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo de cuenta es obligatorio");
        }
        
        // 2. Validación de formato de número de cuenta
        String accountNumber = item.getAccount_number().trim().toUpperCase();
        if (!ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches()) {
            throw new IllegalArgumentException("Formato de número de cuenta inválido: " + accountNumber);
        }
        
        // 3. Validación de nombre del propietario
        String ownerName = item.getOwner_name().trim();
        if (!OWNER_NAME_PATTERN.matcher(ownerName).matches()) {
            throw new IllegalArgumentException("Formato de nombre inválido: " + ownerName);
        }
        
        // 4. Validación de tipo de cuenta
        String accountType = item.getType().trim().toLowerCase();
        if (!isValidAccountType(accountType)) {
            throw new IllegalArgumentException("Tipo de cuenta inválido: " + accountType);
        }
        
        // 5. Validación y conversión de balance
        BigDecimal balance;
        try {
            balance = new BigDecimal(item.getBalance().trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Formato de balance inválido: " + item.getBalance());
        }
        
        // 6. Validación de límites de balance
        if (balance.compareTo(MAX_BALANCE) > 0) {
            throw new IllegalArgumentException("Balance excede el límite máximo: " + balance);
        }
        
        if (balance.compareTo(MIN_BALANCE) < 0) {
            throw new IllegalArgumentException("Balance por debajo del límite mínimo: " + balance);
        }
        
        // 7. Verificar si la cuenta ya existe (para actualización vs creación)
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElse(new Account());
        
        // 8. Configurar la entidad
        account.setAccountNumber(accountNumber);
        account.setOwnerName(ownerName);
        account.setType(normalizeAccountType(accountType));
        account.setBalance(balance);
        
        // 9. Log del procesamiento exitoso
        if (account.getId() == null) {
            logger.debug("Nueva cuenta procesada: {} - {} - {}", accountNumber, ownerName, balance);
        } else {
            logger.debug("Cuenta actualizada: {} - {} - {}", accountNumber, ownerName, balance);
        }
        
        return account;
    }
    
    private boolean isValidAccountType(String type) {
        return type.equals("savings") || type.equals("checking") || 
               type.equals("business") || type.equals("credit") ||
               type.equals("ahorro") || type.equals("prestamo") || 
               type.equals("hipoteca") || type.equals("corriente");
    }
    
    private String normalizeAccountType(String type) {
        return switch (type.toLowerCase()) {
            case "savings", "ahorro" -> "savings";
            case "checking", "corriente" -> "checking";
            case "business", "empresarial", "negocio", "hipoteca" -> "business";
            case "credit", "credito", "prestamo" -> "credit";
            default -> type;
        };
    }
}