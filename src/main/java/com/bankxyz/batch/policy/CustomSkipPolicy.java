package com.bankxyz.batch.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.time.format.DateTimeParseException;

/**
 * Política personalizada de skip para manejar errores específicos en el procesamiento de datos bancarios.
 * Implementa tolerancia a fallos controlada con logging detallado.
 */
@Component
public class CustomSkipPolicy implements SkipPolicy {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomSkipPolicy.class);
    
    @Override
    public boolean shouldSkip(Throwable exception, long skipCount) throws SkipLimitExceededException {
        
        // Errores críticos que NO deben ser omitidos
        if (exception instanceof FileNotFoundException) {
            logger.error("ARCHIVO NO ENCONTRADO - Error crítico que detiene el procesamiento: {}", 
                exception.getMessage());
            return false;
        }
        
        if (exception instanceof OutOfMemoryError) {
            logger.error("ERROR DE MEMORIA - Error crítico que detiene el procesamiento");
            return false;
        }
        
        // Errores de datos que SÍ pueden ser omitidos con logging
        if (exception instanceof NumberFormatException) {
            logger.warn("REGISTRO OMITIDO #{}: Formato de número inválido - {}", 
                skipCount + 1, exception.getMessage());
            return true;
        }
        
        if (exception instanceof DateTimeParseException) {
            logger.warn("REGISTRO OMITIDO #{}: Formato de fecha inválido - {}", 
                skipCount + 1, exception.getMessage());
            return true;
        }
        
        if (exception instanceof IllegalArgumentException) {
            logger.warn("REGISTRO OMITIDO #{}: Argumento inválido - {}", 
                skipCount + 1, exception.getMessage());
            return true;
        }
        
        // Errores de validación de negocio
        if (exception.getMessage() != null) {
            if (exception.getMessage().contains("balance") || exception.getMessage().contains("amount")) {
                logger.warn("REGISTRO OMITIDO #{}: Error de validación de montos - {}", 
                    skipCount + 1, exception.getMessage());
                return true;
            }
            
            if (exception.getMessage().contains("account") || exception.getMessage().contains("ID")) {
                logger.warn("REGISTRO OMITIDO #{}: Error de validación de cuenta - {}", 
                    skipCount + 1, exception.getMessage());
                return true;
            }
        }
        
        // Límite de tolerancia - no omitir más del 10% de los registros
        if (skipCount >= 100) {
            logger.error("LÍMITE DE ERRORES EXCEDIDO: {} registros omitidos. Deteniendo procesamiento.", skipCount);
            throw new SkipLimitExceededException(skipCount, exception);
        }
        
        // Cualquier otro error - log y omitir
        logger.warn("REGISTRO OMITIDO #{}: Error no categorizado - {}", 
            skipCount + 1, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        
        return true;
    }
}