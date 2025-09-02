package com.bankxyz.batch.job;

import com.bankxyz.batch.config.AppProperties;
import com.bankxyz.batch.dto.AccountCsv;
import com.bankxyz.batch.dto.CuentaAnualCsv;
import com.bankxyz.batch.dto.TransactionCsv;
import com.bankxyz.batch.listener.BatchJobListener;
import com.bankxyz.batch.listener.BatchStepListener;
import com.bankxyz.batch.model.Account;
import com.bankxyz.batch.model.AnnualAccountData;
import com.bankxyz.batch.model.LegacyTransaction;
import com.bankxyz.batch.policy.CustomSkipPolicy;
import com.bankxyz.batch.processor.AccountProcessor;
import com.bankxyz.batch.processor.CuentaAnualProcessor;
import com.bankxyz.batch.processor.TransactionProcessor;
import com.bankxyz.batch.writer.AccountUpsertWriter;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;


@Configuration
public class BatchJobsConfig {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobsConfig.class);
    
    private final AppProperties props;
    private final BatchJobListener jobListener;
    private final BatchStepListener stepListener;
    private final CustomSkipPolicy customSkipPolicy;
    private final AccountProcessor accountProcessor;
    private final TransactionProcessor transactionProcessor;
    private final CuentaAnualProcessor cuentaAnualProcessor;
    private final AccountUpsertWriter accountUpsertWriter;

    public BatchJobsConfig(AppProperties props, 
                          BatchJobListener jobListener,
                          BatchStepListener stepListener,
                          CustomSkipPolicy customSkipPolicy,
                          AccountProcessor accountProcessor,
                          TransactionProcessor transactionProcessor,
                          CuentaAnualProcessor cuentaAnualProcessor,
                          AccountUpsertWriter accountUpsertWriter) {
        this.props = props;
        this.jobListener = jobListener;
        this.stepListener = stepListener;
        this.customSkipPolicy = customSkipPolicy;
        this.accountProcessor = accountProcessor;
        this.transactionProcessor = transactionProcessor;
        this.cuentaAnualProcessor = cuentaAnualProcessor;
        this.accountUpsertWriter = accountUpsertWriter;
    }

    /* ---------------- TaskExecutor para Procesamiento Paralelo Optimizado ---------------- */
    
    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración optimizada para procesamiento de archivos CSV
        executor.setCorePoolSize(3);        // 3 hilos core como requisito
        executor.setMaxPoolSize(5);         // Máximo 5 hilos para picos de carga
        executor.setQueueCapacity(100);     // Cola más grande para mejor throughput
        executor.setThreadNamePrefix("batch-csv-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // Más tiempo para finalización limpia
        
        // Configuración de rechazo para manejo de sobrecarga
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        
        logger.info("✅ TaskExecutor configurado: Core={}, Max={}, Queue={} para procesamiento paralelo optimizado", 
                   executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
    
    /* ---------------- Configuraciones de Chunk Size Dinámico ---------------- */
    
    private int calculateOptimalChunkSize(String jobType) {
        // Ajustar chunk size según el tipo de job y complejidad de procesamiento
        return switch (jobType) {
            case "transactions" -> 10; // Transacciones son más ligeras
            case "accounts" -> 5;      // Cuentas tienen más validaciones
            case "annual" -> 8;        // Estados anuales son medianamente complejos
            default -> 5;
        };
    }

    /* ---------------- Readers ---------------- */

    @Bean
    public FlatFileItemReader<AccountCsv> accountReader() {
        return new FlatFileItemReaderBuilder<AccountCsv>()
                .name("accountReader")
                .resource(new FileSystemResource(props.getDataDir() + "/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                // ✅ CORREGIDO: Nombres de campos actualizados para coincidir con CSV real
                .names("cuenta_id","nombre","saldo","edad","tipo")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(AccountCsv.class);
                }})
                .build();
    }

    @Bean
    public FlatFileItemReader<TransactionCsv> transactionReader() {
        return new FlatFileItemReaderBuilder<TransactionCsv>()
                .name("transactionReader")
                .resource(new FileSystemResource(props.getDataDir() + "/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                // ✅ CORREGIDO: Nombres de campos actualizados para coincidir con CSV real
                .names("id","fecha","monto","tipo")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TransactionCsv.class);
                }})
                .build();
    }

    /* ---------------- Writers ---------------- */

    @Bean
    public JpaItemWriter<Account> accountWriter(EntityManagerFactory emf) {
        JpaItemWriter<Account> w = new JpaItemWriter<>();
        w.setEntityManagerFactory(emf);
        return w;
    }

    @Bean
    public JpaItemWriter<LegacyTransaction> transactionWriter(EntityManagerFactory emf) {
        JpaItemWriter<LegacyTransaction> w = new JpaItemWriter<>();
        w.setEntityManagerFactory(emf);
        return w;
    }

    /* ---------------- Steps Eliminados - Razón: Creaban Mapeos Artificiales ---------------- */
    
    // ❌ ELIMINADO: loadAccountsStep - Creaba cuentas ACC001-ACC010 artificiales
    // ❌ ELIMINADO: loadTransactionsStep - Mapeaba transacciones a cuentas inexistentes
    // 
    // ✅ NUEVO ENFOQUE: Cada Job procesa su CSV independientemente
    // - monthlyInterestJob → intereses.csv → Crea cuentas reales (101-150)
    // - dailyReportJob → transacciones.csv → Transacciones independientes
    // - annualAccountsJob → cuentas_anuales.csv → Datos anuales independientes

    /* ---------------- Jobs Mejorados con Listeners y Políticas de Re-ejecución ---------------- */

    // Job 1: Reporte de Transacciones Independientes - SIMPLIFICADO
    @Bean
    public Job dailyReportJob(JobRepository jobRepository, 
                             PlatformTransactionManager txManager,
                             TaskExecutor batchTaskExecutor,
                             FlatFileItemReader<TransactionCsv> transactionReader,
                             EntityManagerFactory emf) {

        // WRITER SIMPLIFICADO - guarda transacciones en transaction_legacy
        JpaItemWriter<LegacyTransaction> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);

        // PROCESSOR SIMPLIFICADO - usa directamente el procesador (detecta anomalías internamente)
        ItemProcessor<TransactionCsv, LegacyTransaction> processor = transactionProcessor;

        Step step = new StepBuilder("dailyReportStep", jobRepository)
                .<TransactionCsv, LegacyTransaction>chunk(calculateOptimalChunkSize("transactions"), txManager)  
                .reader(transactionReader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor)           
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              
                .retryLimit(3)
                .retry(Exception.class)
                .listener(stepListener)
                .build();

        return new JobBuilder("dailyReportJob", jobRepository)
                .start(step)
                .listener(jobListener)                     
                .build();
    }

    // Job 2: Cálculo de Intereses desde intereses.csv - SIMPLIFICADO
    @Bean
    public Job monthlyInterestJob(JobRepository jobRepository, 
                                 PlatformTransactionManager txManager,
                                 TaskExecutor batchTaskExecutor,
                                 FlatFileItemReader<AccountCsv> accountReader,
                                 EntityManagerFactory emf) {

        // WRITER PERSONALIZADO - UPSERT para actualizar saldos sin errores de clave duplicada
        ItemWriter<Account> writer = accountUpsertWriter;

        // PROCESSOR SIMPLIFICADO - usa directamente el procesador (calcula intereses internamente)  
        ItemProcessor<AccountCsv, Account> processor = accountProcessor;

        Step step = new StepBuilder("monthlyInterestStep", jobRepository)
                .<AccountCsv, Account>chunk(calculateOptimalChunkSize("accounts"), txManager)  
                .reader(accountReader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor)           
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              
                .retryLimit(3)
                .retry(Exception.class)
                .listener(stepListener)
                .build();

        return new JobBuilder("monthlyInterestJob", jobRepository)
                .start(step)
                .listener(jobListener)                     
                .build();
    }

    // ❌ JOB ELIMINADO: annualStatementJob 
    // MOTIVO: Mezcla artificialmente transacciones.csv con cuentas
    // Las transacciones son independientes y no deben asociarse a cuentas
    
    // Job 3: Procesamiento de Cuentas Anuales Independientes - SIMPLIFICADO
    @Bean
    public Job annualAccountsJob(JobRepository jobRepository, 
                                PlatformTransactionManager txManager,
                                TaskExecutor batchTaskExecutor,
                                EntityManagerFactory emf) {

        // Reader para cuentas anuales independientes
        FlatFileItemReader<CuentaAnualCsv> reader = new FlatFileItemReaderBuilder<CuentaAnualCsv>()
                .name("cuentaAnualCsvReader")
                .resource(new FileSystemResource(props.getDataDir() + "/cuentas_anuales.csv"))
                .delimited()
                .names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
                .linesToSkip(1)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(CuentaAnualCsv.class);
                }})
                .build();

        // Writer
        JpaItemWriter<AnnualAccountData> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);

        // Processor SIMPLIFICADO - usa directamente el procesador sin servicios extra
        ItemProcessor<CuentaAnualCsv, AnnualAccountData> processor = cuentaAnualProcessor;

        Step step = new StepBuilder("annualAccountsStep", jobRepository)
                .<CuentaAnualCsv, AnnualAccountData>chunk(calculateOptimalChunkSize("annual"), txManager)  
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor)           
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              
                .retryLimit(3)
                .retry(Exception.class)
                .listener(stepListener)
                .build();

        return new JobBuilder("annualAccountsJob", jobRepository)
                .start(step)
                .listener(jobListener)                     
                .build();
    }
}
