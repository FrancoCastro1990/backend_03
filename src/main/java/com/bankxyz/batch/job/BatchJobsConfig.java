package com.bankxyz.batch.job;

import com.bankxyz.batch.config.AppProperties;
import com.bankxyz.batch.dto.AccountCsv;
import com.bankxyz.batch.dto.CuentaAnualCsv;
import com.bankxyz.batch.dto.TransactionCsv;
import com.bankxyz.batch.listener.BatchJobListener;
import com.bankxyz.batch.listener.BatchStepListener;
import com.bankxyz.batch.model.*;
import com.bankxyz.batch.policy.CustomSkipPolicy;
import com.bankxyz.batch.processor.CuentaAnualProcessor;
import com.bankxyz.batch.processor.EnhancedAccountProcessor;
import com.bankxyz.batch.processor.EnhancedTransactionProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Configuration
public class BatchJobsConfig {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobsConfig.class);
    
    private final AppProperties props;
    private final BatchJobListener jobListener;
    private final BatchStepListener stepListener;
    private final CustomSkipPolicy customSkipPolicy;
    private final EnhancedAccountProcessor enhancedAccountProcessor;
    private final EnhancedTransactionProcessor enhancedTransactionProcessor;

    public BatchJobsConfig(AppProperties props, 
                          BatchJobListener jobListener,
                          BatchStepListener stepListener,
                          CustomSkipPolicy customSkipPolicy,
                          EnhancedAccountProcessor enhancedAccountProcessor,
                          EnhancedTransactionProcessor enhancedTransactionProcessor) {
        this.props = props;
        this.jobListener = jobListener;
        this.stepListener = stepListener;
        this.customSkipPolicy = customSkipPolicy;
        this.enhancedAccountProcessor = enhancedAccountProcessor;
        this.enhancedTransactionProcessor = enhancedTransactionProcessor;
    }

    /* ---------------- TaskExecutor para Procesamiento Paralelo (3 hilos) ---------------- */
    
    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);        // 3 hilos como requisito
        executor.setMaxPoolSize(3);         // Máximo 3 hilos
        executor.setQueueCapacity(50);      // Cola para tareas pendientes
        executor.setThreadNamePrefix("batch-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        logger.info("TaskExecutor configurado: 3 hilos para procesamiento paralelo");
        return executor;
    }

    /* ---------------- Readers ---------------- */

    @Bean
    public FlatFileItemReader<AccountCsv> accountReader() {
        return new FlatFileItemReaderBuilder<AccountCsv>()
                .name("accountReader")
                .resource(new FileSystemResource(props.getDataDir() + "/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("account_number","owner_name","type","balance")
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
                .names("tx_id","account_number","tx_date","description","amount")
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

    /* ---------------- Steps Mejorados con Paralelismo y Políticas Personalizadas ---------------- */

    @Bean
    public Step loadAccountsStep(JobRepository jobRepository, 
                                PlatformTransactionManager txManager,
                                TaskExecutor batchTaskExecutor,
                                FlatFileItemReader<AccountCsv> accountReader,
                                JpaItemWriter<Account> accountWriter) {
        return new StepBuilder("loadAccountsStep", jobRepository)
                .<AccountCsv, Account>chunk(5, txManager)  // Chunk size = 5 según requisito
                .reader(accountReader)
                .processor(enhancedAccountProcessor)       // Procesador mejorado
                .writer(accountWriter)
                .taskExecutor(batchTaskExecutor)           // 3 hilos paralelos
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              // Política personalizada
                .retryLimit(3)                             // Reintentos automáticos
                .retry(Exception.class)
                .listener(stepListener)                    // Listener para logs
                .build();
    }

    @Bean
    public Step loadTransactionsStep(JobRepository jobRepository, 
                                    PlatformTransactionManager txManager,
                                    TaskExecutor batchTaskExecutor,
                                    FlatFileItemReader<TransactionCsv> transactionReader,
                                    JpaItemWriter<LegacyTransaction> transactionWriter) {
        return new StepBuilder("loadTransactionsStep", jobRepository)
                .<TransactionCsv, LegacyTransaction>chunk(5, txManager)  // Chunk size = 5
                .reader(transactionReader)
                .processor(enhancedTransactionProcessor)   // Procesador mejorado
                .writer(transactionWriter)
                .taskExecutor(batchTaskExecutor)           // 3 hilos paralelos
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              // Política personalizada
                .retryLimit(3)                             // Reintentos automáticos
                .retry(Exception.class)
                .listener(stepListener)                    // Listener para logs
                .build();
    }

    /* ---------------- Jobs Mejorados con Listeners y Políticas de Re-ejecución ---------------- */

    // Job 1: Reporte de Transacciones Diarias - MEJORADO
    @Bean
    public Job dailyReportJob(JobRepository jobRepository, 
                             PlatformTransactionManager txManager,
                             TaskExecutor batchTaskExecutor,
                             FlatFileItemReader<TransactionCsv> transactionReader,
                             EntityManagerFactory emf) {

        JpaItemWriter<DailyTransactionReport> reportWriter = new JpaItemWriter<>();
        reportWriter.setEntityManagerFactory(emf);

        ItemProcessor<TransactionCsv, DailyTransactionReport> reportProcessor = new ItemProcessor<>() {
            @Override
            public DailyTransactionReport process(TransactionCsv item) throws Exception {
                // Usar el procesador mejorado para validaciones básicas
                LegacyTransaction validatedTx = enhancedTransactionProcessor.process(item);
                if (validatedTx == null) {
                    return null; // skip invalid records
                }
                
                DailyTransactionReport r = new DailyTransactionReport();
                r.setReportDate(validatedTx.getTxDate());
                r.setAccountNumber(validatedTx.getAccountNumber());
                r.setTotalAmount(validatedTx.getAmount());
                r.setTxCount(1);
                
                // Detección mejorada de anomalías
                BigDecimal amt = validatedTx.getAmount();
                boolean anomaly = amt.abs().compareTo(new BigDecimal("100000")) > 0 || 
                                 amt.compareTo(BigDecimal.ZERO) == 0 ||
                                 validatedTx.getTxDate().isAfter(LocalDate.now());
                r.setAnomalies(anomaly ? 1 : 0);
                
                if (anomaly) {
                    logger.warn("Anomalía detectada en reporte diario: TX {} - Monto: {}", 
                        validatedTx.getTxId(), amt);
                }
                
                return r;
            }
        };

        Step step = new StepBuilder("dailyReportStep", jobRepository)
                .<TransactionCsv, DailyTransactionReport>chunk(5, txManager)  // Chunk size = 5
                .reader(transactionReader)
                .processor(reportProcessor)
                .writer(reportWriter)
                .taskExecutor(batchTaskExecutor)           // Procesamiento paralelo
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              // Política personalizada
                .retryLimit(3)
                .retry(Exception.class)
                .listener(stepListener)
                .build();

        return new JobBuilder("dailyReportJob", jobRepository)
                .start(step)
                .listener(jobListener)                     // Listener para métricas
                .build();
    }

    // Job 2: Cálculo de Intereses Mensuales - MEJORADO
    @Bean
    public Job monthlyInterestJob(JobRepository jobRepository, 
                                 PlatformTransactionManager txManager,
                                 TaskExecutor batchTaskExecutor,
                                 FlatFileItemReader<AccountCsv> accountReader,
                                 EntityManagerFactory emf) {

        JpaItemWriter<MonthlyInterest> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);

        ItemProcessor<AccountCsv, MonthlyInterest> processor = new ItemProcessor<>() {
            @Override
            public MonthlyInterest process(AccountCsv item) throws Exception {
                // Usar el procesador mejorado para validaciones
                Account validatedAccount = enhancedAccountProcessor.process(item);
                if (validatedAccount == null) {
                    return null; // skip invalid records
                }
                
                MonthlyInterest mi = new MonthlyInterest();
                String month = System.getProperty("month", YearMonth.now().toString());
                mi.setMonthYear(month);
                mi.setAccountNumber(validatedAccount.getAccountNumber());
                
                BigDecimal balance = validatedAccount.getBalance();
                
                // Aplicar tasas de interés diferenciadas por tipo de cuenta
                BigDecimal rate = calculateInterestRate(validatedAccount.getType(), balance);
                BigDecimal interest = balance.multiply(rate);
                
                mi.setInterestApplied(interest);
                mi.setFinalBalance(balance.add(interest));
                
                logger.debug("Interés calculado para cuenta {}: {} (tasa: {})", 
                    validatedAccount.getAccountNumber(), interest, rate);
                
                return mi;
            }
            
            private BigDecimal calculateInterestRate(String accountType, BigDecimal balance) {
                // Tasas diferenciadas según tipo de cuenta y saldo
                return switch (accountType.toLowerCase()) {
                    case "savings" -> balance.compareTo(new BigDecimal("50000")) > 0 ? 
                                     new BigDecimal("0.004") : new BigDecimal("0.003");
                    case "checking" -> new BigDecimal("0.001");
                    case "business" -> balance.compareTo(new BigDecimal("100000")) > 0 ? 
                                      new BigDecimal("0.0035") : new BigDecimal("0.002");
                    case "credit" -> new BigDecimal("0.0015");
                    default -> new BigDecimal("0.002");
                };
            }
        };

        Step step = new StepBuilder("monthlyInterestStep", jobRepository)
                .<AccountCsv, MonthlyInterest>chunk(5, txManager)  // Chunk size = 5
                .reader(accountReader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor)           // Procesamiento paralelo
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              // Política personalizada
                .retryLimit(3)
                .retry(Exception.class)
                .listener(stepListener)
                .build();

        return new JobBuilder("monthlyInterestJob", jobRepository)
                .start(step)
                .listener(jobListener)                     // Listener para métricas
                .build();
    }

    // Job 3: Estados de Cuenta Anuales - MEJORADO
    @Bean
    public Job annualStatementJob(JobRepository jobRepository, 
                                 PlatformTransactionManager txManager,
                                 TaskExecutor batchTaskExecutor,
                                 FlatFileItemReader<TransactionCsv> transactionReader,
                                 EntityManagerFactory emf) {

        JpaItemWriter<AnnualStatement> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);

        ItemProcessor<TransactionCsv, AnnualStatement> processor = new ItemProcessor<>() {
            @Override
            public AnnualStatement process(TransactionCsv item) throws Exception {
                // Usar el procesador mejorado para validaciones
                LegacyTransaction validatedTx = enhancedTransactionProcessor.process(item);
                if (validatedTx == null) {
                    return null; // skip invalid records
                }
                
                AnnualStatement as = new AnnualStatement();
                int year = validatedTx.getTxDate().getYear();
                as.setYear(year);
                as.setAccountNumber(validatedTx.getAccountNumber());
                
                // Clasificación mejorada de transacciones
                BigDecimal amt = validatedTx.getAmount();
                as.setOpeningBalance(BigDecimal.ZERO); // Se calculará en agregación posterior
                
                if (amt.signum() > 0) {
                    as.setTotalDeposits(amt);
                    as.setTotalWithdrawals(BigDecimal.ZERO);
                } else {
                    as.setTotalDeposits(BigDecimal.ZERO);
                    as.setTotalWithdrawals(amt.abs());
                }
                
                as.setClosingBalance(BigDecimal.ZERO); // Se calculará en agregación posterior
                
                // Log para transacciones de alto valor
                if (amt.abs().compareTo(new BigDecimal("50000")) > 0) {
                    logger.info("Transacción de alto valor en estado anual: {} - Cuenta: {} - Monto: {}", 
                        validatedTx.getTxId(), validatedTx.getAccountNumber(), amt);
                }
                
                return as;
            }
        };

        Step step = new StepBuilder("annualStatementStep", jobRepository)
                .<TransactionCsv, AnnualStatement>chunk(5, txManager)  // Chunk size = 5
                .reader(transactionReader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor)           // Procesamiento paralelo
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              // Política personalizada
                .retryLimit(3)
                .retry(Exception.class)
                .listener(stepListener)
                .build();

        return new JobBuilder("annualStatementJob", jobRepository)
                .start(step)
                .listener(jobListener)                     // Listener para métricas
                .build();
    }

    // Job 4: Procesamiento de Cuentas Anuales desde CSV específico - NUEVO
    @Bean
    public Job annualAccountsJob(JobRepository jobRepository, 
                                PlatformTransactionManager txManager,
                                TaskExecutor batchTaskExecutor,
                                EntityManagerFactory emf) {

        // Reader para cuentas anuales
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
        JpaItemWriter<AnnualStatement> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);

        // Processor específico para cuentas anuales
        CuentaAnualProcessor processor = new CuentaAnualProcessor();

        Step step = new StepBuilder("annualAccountsStep", jobRepository)
                .<CuentaAnualCsv, AnnualStatement>chunk(5, txManager)  // Chunk size = 5
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(batchTaskExecutor)           // Procesamiento paralelo
                .faultTolerant()
                .skipPolicy(customSkipPolicy)              // Política personalizada
                .retryLimit(3)
                .retry(Exception.class)
                .listener(stepListener)
                .build();

        return new JobBuilder("annualAccountsJob", jobRepository)
                .start(step)
                .listener(jobListener)                     // Listener para métricas
                .build();
    }
}
