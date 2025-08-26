package com.bankxyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BatchJobListener implements JobExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchJobListener.class);
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        logger.info("========================================");
        logger.info("INICIANDO JOB: {}", jobName);
        logger.info("Job ID: {}", jobExecution.getJobId());
        logger.info("Hora de inicio: {}", LocalDateTime.now());
        logger.info("Parámetros: {}", jobExecution.getJobParameters().getParameters());
        logger.info("========================================");
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        long durationMs = 0;
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            durationMs = java.time.Duration.between(
                jobExecution.getStartTime().toInstant(java.time.ZoneOffset.UTC),
                jobExecution.getEndTime().toInstant(java.time.ZoneOffset.UTC)
            ).toMillis();
        }
        
        logger.info("========================================");
        logger.info("JOB COMPLETADO: {}", jobName);
        logger.info("Status: {}", jobExecution.getStatus());
        logger.info("Duración total: {} ms", durationMs);
        logger.info("Hora de finalización: {}", LocalDateTime.now());
        
        // Métricas de rendimiento
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            logger.info("STEP: {} - Read: {}, Write: {}, Skip: {}, Commits: {}", 
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                stepExecution.getCommitCount()
            );
        });
        
        if (jobExecution.getStatus().isUnsuccessful()) {
            logger.error("Job falló. Excepciones:");
            jobExecution.getAllFailureExceptions().forEach(throwable -> 
                logger.error("Error: ", throwable)
            );
        }
        logger.info("========================================");
    }
}