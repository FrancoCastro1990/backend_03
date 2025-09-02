package com.bankxyz.batch.writer;

import com.bankxyz.batch.model.Account;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Writer personalizado que realiza UPSERT (INSERT o UPDATE) para cuentas
 * REQUERIMIENTO: "actualizar el saldo final en base de datos"
 */
@Component
public class AccountUpsertWriter implements ItemWriter<Account> {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountUpsertWriter.class);
    
    private final EntityManagerFactory entityManagerFactory;
    
    public AccountUpsertWriter(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void write(Chunk<? extends Account> chunk) throws Exception {
        EntityManager em = entityManagerFactory.createEntityManager();
        
        try {
            em.getTransaction().begin();
            
            for (Account account : chunk) {
                try {
                    // Buscar si la cuenta ya existe por account_number
                    Account existingAccount = em.createQuery(
                        "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber", Account.class)
                        .setParameter("accountNumber", account.getAccountNumber())
                        .getResultStream()
                        .findFirst()
                        .orElse(null);
                    
                    if (existingAccount != null) {
                        // 🎯 UPDATE: Actualizar saldo final (requerimiento principal)
                        existingAccount.setBalance(account.getBalance());
                        existingAccount.setOwnerName(account.getOwnerName());
                        existingAccount.setType(account.getType());
                        existingAccount.setAge(account.getAge());
                        
                        em.merge(existingAccount);
                        logger.debug("💰 CUENTA ACTUALIZADA: {} - Nuevo balance: ${}", 
                            account.getAccountNumber(), account.getBalance());
                        
                    } else {
                        // INSERT: Crear nueva cuenta
                        em.persist(account);
                        logger.debug("🆕 CUENTA CREADA: {} - Balance inicial: ${}", 
                            account.getAccountNumber(), account.getBalance());
                    }
                    
                } catch (PersistenceException e) {
                    logger.warn("⚠️ Error procesando cuenta {}: {}", account.getAccountNumber(), e.getMessage());
                    // Continuar con el siguiente registro
                }
            }
            
            em.getTransaction().commit();
            logger.debug("✅ Chunk de {} cuentas procesado exitosamente", chunk.size());
            
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            logger.error("❌ Error procesando chunk de cuentas: {}", e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }
}