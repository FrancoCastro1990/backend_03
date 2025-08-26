package com.bankxyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class BatchStepListener implements StepExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchStepListener.class);
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        logger.info(">>> INICIANDO STEP: {}", stepName);
        logger.info("Configuración de chunk para step {}: commit-interval configurado", stepName);
    }
    
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        
        logger.info(">>> STEP COMPLETADO: {}", stepName);
        logger.info("Registros leídos: {}", stepExecution.getReadCount());
        logger.info("Registros escritos: {}", stepExecution.getWriteCount());
        logger.info("Registros omitidos: {}", stepExecution.getSkipCount());
        logger.info("Commits realizados: {}", stepExecution.getCommitCount());
        logger.info("Rollbacks: {}", stepExecution.getRollbackCount());
        logger.info("Status de salida: {}", stepExecution.getExitStatus());
        
        // Calcular eficiencia del procesamiento
        if (stepExecution.getReadCount() > 0) {
            double eficiencia = (double) stepExecution.getWriteCount() / stepExecution.getReadCount() * 100;
            logger.info("Eficiencia del procesamiento: {:.2f}%", eficiencia);
        }
        
        // Log de errores si los hay
        if (stepExecution.getSkipCount() > 0) {
            logger.warn("ADVERTENCIA: {} registros fueron omitidos durante el procesamiento", stepExecution.getSkipCount());
        }
        
        if (stepExecution.getRollbackCount() > 0) {
            logger.warn("ADVERTENCIA: {} rollbacks ocurrieron durante el procesamiento", stepExecution.getRollbackCount());
        }
        
        return stepExecution.getExitStatus();
    }
}