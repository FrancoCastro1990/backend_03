package com.bankxyz.batch.runner;

import com.bankxyz.batch.listener.PerformanceMonitorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Runner para ejecutar los jobs de procesamiento de archivos CSV independientes
 * Ejecuta cada job en secuencia con monitoreo de rendimiento
 */
@Component
@Order(1)
public class BatchJobRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobRunner.class);

    private final JobLauncher jobLauncher;
    private final Job dailyReportJob;
    private final Job monthlyInterestJob;
    private final Job annualAccountsJob;
    private final PerformanceMonitorListener<?, ?> performanceMonitor;

    public BatchJobRunner(JobLauncher jobLauncher,
                         Job dailyReportJob,
                         Job monthlyInterestJob,
                         Job annualAccountsJob,
                         PerformanceMonitorListener<?, ?> performanceMonitor) {
        this.jobLauncher = jobLauncher;
        this.dailyReportJob = dailyReportJob;
        this.monthlyInterestJob = monthlyInterestJob;
        this.annualAccountsJob = annualAccountsJob;
        this.performanceMonitor = performanceMonitor;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 Iniciando procesamiento de archivos CSV independientes del Banco XYZ");
        logger.info("📅 Fecha de ejecución: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Verificar argumentos para ejecución selectiva
        boolean runAll = args.length == 0 || contains(args, "all");
        boolean runTransactions = runAll || contains(args, "transactions");
        boolean runInterests = runAll || contains(args, "interests");
        boolean runAnnual = runAll || contains(args, "annual");

        int totalJobs = 0;
        int successfulJobs = 0;

        // Job 1: Procesamiento de Transacciones Diarias (transacciones.csv)
        if (runTransactions) {
            totalJobs++;
            logger.info("\n" + "=".repeat(80));
            logger.info("📊 EJECUTANDO JOB 1/3: Reporte de Transacciones Diarias");
            logger.info("📁 Archivo: transacciones.csv");
            logger.info("🎯 Objetivo: Generar reportes diarios con detección de anomalías");
            logger.info("=".repeat(80));

            if (executeJob(dailyReportJob, "dailyReportJob", "transacciones.csv")) {
                successfulJobs++;
            }
        }

        // Job 2: Cálculo de Intereses Mensuales (intereses.csv)  
        if (runInterests) {
            totalJobs++;
            logger.info("\n" + "=".repeat(80));
            logger.info("💰 EJECUTANDO JOB 2/3: Cálculo de Intereses Mensuales");
            logger.info("📁 Archivo: intereses.csv");
            logger.info("🎯 Objetivo: Calcular intereses y actualizar balances");
            logger.info("=".repeat(80));

            JobParameters interestParams = new JobParametersBuilder()
                .addString("month", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            if (executeJob(monthlyInterestJob, "monthlyInterestJob", "intereses.csv", interestParams)) {
                successfulJobs++;
            }
        }

        // Job 3: Estados de Cuenta Anuales (cuentas_anuales.csv)
        if (runAnnual) {
            totalJobs++;
            logger.info("\n" + "=".repeat(80));
            logger.info("📋 EJECUTANDO JOB 3/3: Estados de Cuenta Anuales");
            logger.info("📁 Archivo: cuentas_anuales.csv");
            logger.info("🎯 Objetivo: Generar estados anuales para auditoría");
            logger.info("=".repeat(80));

            if (executeJob(annualAccountsJob, "annualAccountsJob", "cuentas_anuales.csv")) {
                successfulJobs++;
            }
        }

        // Resumen final
        logger.info("\n" + "=".repeat(80));
        logger.info("🏁 RESUMEN FINAL DE EJECUCIÓN");
        logger.info("=".repeat(80));
        logger.info("✅ Jobs ejecutados exitosamente: {}/{}", successfulJobs, totalJobs);
        logger.info("❌ Jobs con errores: {}", totalJobs - successfulJobs);
        
        if (successfulJobs == totalJobs && totalJobs > 0) {
            logger.info("🎉 TODOS LOS JOBS COMPLETADOS EXITOSAMENTE");
            logger.info("✅ El sistema de migración batch está funcionando correctamente");
            logger.info("📊 Los archivos CSV se han procesado de forma completamente independiente");
        } else if (successfulJobs > 0) {
            logger.warn("⚠️  EJECUCIÓN PARCIAL: {} de {} jobs completados", successfulJobs, totalJobs);
        } else if (totalJobs > 0) {
            logger.error("💥 FALLO TOTAL: Ningún job se completó exitosamente");
        } else {
            logger.info("ℹ️  No se ejecutaron jobs (usar argumentos: transactions, interests, annual, o all)");
        }
        
        logger.info("🔚 Procesamiento de archivos CSV finalizado");
    }

    private boolean executeJob(Job job, String jobName, String fileName) {
        return executeJob(job, jobName, fileName, createDefaultJobParameters());
    }

    private boolean executeJob(Job job, String jobName, String fileName, JobParameters params) {
        try {
            long startTime = System.currentTimeMillis();
            
            logger.info("▶️  Iniciando job: {}", jobName);
            logger.info("📄 Procesando archivo: {}", fileName);
            
            JobExecution execution = jobLauncher.run(job, params);
            
            long duration = System.currentTimeMillis() - startTime;
            BatchStatus status = execution.getStatus();
            
            if (status == BatchStatus.COMPLETED) {
                logger.info("✅ Job {} COMPLETADO EXITOSAMENTE", jobName);
                logger.info("⏱️  Duración: {} ms", duration);
                
                // Log estadísticas de rendimiento
                performanceMonitor.logFinalStats(jobName);
                
                // Log detalles de ejecución
                logExecutionDetails(execution);
                
                return true;
                
            } else if (status == BatchStatus.FAILED) {
                logger.error("❌ Job {} FALLÓ", jobName);
                logExecutionErrors(execution);
                return false;
                
            } else {
                logger.warn("⚠️  Job {} terminó con estado: {}", jobName, status);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("💥 Error ejecutando job {}: {}", jobName, e.getMessage(), e);
            return false;
        }
    }

    private JobParameters createDefaultJobParameters() {
        return new JobParametersBuilder()
            .addString("runDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")))
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();
    }

    private void logExecutionDetails(JobExecution execution) {
        logger.info("📋 Detalles de ejecución:");
        logger.info("   - ID de ejecución: {}", execution.getId());
        logger.info("   - Inicio: {}", execution.getStartTime());
        logger.info("   - Fin: {}", execution.getEndTime());
        logger.info("   - Estado: {}", execution.getStatus());
        
        execution.getStepExecutions().forEach(stepExecution -> {
            logger.info("   - Step '{}': {} registros leídos, {} escritos, {} omitidos", 
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());
        });
    }

    private void logExecutionErrors(JobExecution execution) {
        execution.getAllFailureExceptions().forEach(throwable -> {
            logger.error("   💀 Error: {}", throwable.getMessage());
        });
        
        execution.getStepExecutions().forEach(stepExecution -> {
            stepExecution.getFailureExceptions().forEach(throwable -> {
                logger.error("   💀 Error en step '{}': {}", 
                    stepExecution.getStepName(), throwable.getMessage());
            });
        });
    }

    private boolean contains(String[] args, String value) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}