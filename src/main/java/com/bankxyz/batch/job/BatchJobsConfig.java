package com.bankxyz.batch.job;

import com.bankxyz.batch.config.AppProperties;
import com.bankxyz.batch.dto.AccountCsv;
import com.bankxyz.batch.dto.TransactionCsv;
import com.bankxyz.batch.model.*;
import com.bankxyz.batch.repository.AccountRepository;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class BatchJobsConfig {

    private final AppProperties props;
    private final AccountRepository accountRepository;

    public BatchJobsConfig(AppProperties props, AccountRepository accountRepository) {
        this.props = props;
        this.accountRepository = accountRepository;
    }

    /* ---------------- Readers ---------------- */

    @Bean
    public FlatFileItemReader<AccountCsv> accountReader() {
        return new FlatFileItemReaderBuilder<AccountCsv>()
                .name("accountReader")
                .resource(new FileSystemResource(props.getDataDir() + "/accounts.csv"))
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
                .resource(new FileSystemResource(props.getDataDir() + "/transactions.csv"))
                .linesToSkip(1)
                .delimited()
                .names("tx_id","account_number","tx_date","description","amount")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TransactionCsv.class);
                }})
                .build();
    }

    /* ---------------- Processors ---------------- */

    @Bean
    public ItemProcessor<AccountCsv, Account> accountProcessor() {
        return item -> {
            // Basic validation
            if (item.getAccount_number() == null || item.getAccount_number().isBlank()) {
                return null; // skip
            }
            Account acc = accountRepository.findByAccountNumber(item.getAccount_number())
                    .orElse(new Account());
            acc.setAccountNumber(item.getAccount_number());
            acc.setOwnerName(item.getOwner_name());
            acc.setType(item.getType());
            try {
                acc.setBalance(new BigDecimal(item.getBalance()));
            } catch (Exception e) {
                // default 0 if invalid
                acc.setBalance(BigDecimal.ZERO);
            }
            return acc;
        };
    }

    @Bean
    public ItemProcessor<TransactionCsv, LegacyTransaction> transactionProcessor() {
        return item -> {
            // Validate data, handle errors and normalization
            if (item.getTx_id() == null || item.getAccount_number() == null || item.getTx_date() == null) {
                return null; // skip invalid
            }
            LegacyTransaction tx = new LegacyTransaction();
            tx.setTxId(item.getTx_id().trim());
            tx.setAccountNumber(item.getAccount_number().trim());

            try {
                tx.setTxDate(LocalDate.parse(item.getTx_date()));
            } catch (Exception e) {
                return null; // skip bad date
            }

            tx.setDescription(item.getDescription() == null ? "" : item.getDescription());
            try {
                tx.setAmount(new BigDecimal(item.getAmount()));
            } catch (Exception e) {
                return null; // skip bad amount
            }
            return tx;
        };
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

    /* ---------------- Steps ---------------- */

    @Bean
    public Step loadAccountsStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                                 FlatFileItemReader<AccountCsv> accountReader,
                                 ItemProcessor<AccountCsv, Account> accountProcessor,
                                 JpaItemWriter<Account> accountWriter) {
        return new StepBuilder("loadAccountsStep", jobRepository)
                .<AccountCsv, Account>chunk(500, txManager)
                .reader(accountReader)
                .processor(accountProcessor)
                .writer(accountWriter)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step loadTransactionsStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                                     FlatFileItemReader<TransactionCsv> transactionReader,
                                     ItemProcessor<TransactionCsv, LegacyTransaction> transactionProcessor,
                                     JpaItemWriter<LegacyTransaction> transactionWriter) {
        return new StepBuilder("loadTransactionsStep", jobRepository)
                .<TransactionCsv, LegacyTransaction>chunk(1000, txManager)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .faultTolerant()
                .skipLimit(500)
                .skip(Exception.class)
                .build();
    }

    /* ---------------- Jobs ---------------- */

    // Job 1: Reporte de Transacciones Diarias
    @Bean
    public Job dailyReportJob(JobRepository jobRepository, PlatformTransactionManager txManager,
                              FlatFileItemReader<TransactionCsv> transactionReader,
                              EntityManagerFactory emf) {

        JpaItemWriter<DailyTransactionReport> reportWriter = new JpaItemWriter<>();
        reportWriter.setEntityManagerFactory(emf);

        ItemProcessor<TransactionCsv, DailyTransactionReport> reportProcessor = new ItemProcessor<>() {
            // We aggregate per (date, account). For simplicity, we emit 1 report per line and rely on SQL aggregation later
            @Override
            public DailyTransactionReport process(TransactionCsv item) throws Exception {
                DailyTransactionReport r = new DailyTransactionReport();
                r.setReportDate(LocalDate.parse(item.getTx_date()));
                r.setAccountNumber(item.getAccount_number());
                try {
                    r.setTotalAmount(new BigDecimal(item.getAmount()));
                } catch (Exception e) {
                    r.setTotalAmount(BigDecimal.ZERO);
                }
                r.setTxCount(1);
                // anomaly heuristic: very large withdrawals or negative amounts
                BigDecimal amt = new BigDecimal(item.getAmount());
                boolean anomaly = amt.abs().compareTo(new BigDecimal("1000000")) > 0 || amt.compareTo(BigDecimal.ZERO) < 0;
                r.setAnomalies(anomaly ? 1 : 0);
                return r;
            }
        };

        Step step = new StepBuilder("dailyReportStep", jobRepository)
                .<TransactionCsv, DailyTransactionReport>chunk(1000, txManager)
                .reader(transactionReader)
                .processor(reportProcessor)
                .writer(reportWriter)
                .faultTolerant()
                .skipLimit(500)
                .skip(Exception.class)
                .build();

        return new JobBuilder("dailyReportJob", jobRepository)
                .start(step)
                .build();
    }

    // Job 2: Cálculo de Intereses Mensuales
    @Bean
    public Job monthlyInterestJob(JobRepository jobRepository, PlatformTransactionManager txManager,
                                  FlatFileItemReader<AccountCsv> accountReader,
                                  EntityManagerFactory emf) {

        JpaItemWriter<MonthlyInterest> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);

        ItemProcessor<AccountCsv, MonthlyInterest> processor = new ItemProcessor<>() {
            @Override
            public MonthlyInterest process(AccountCsv item) throws Exception {
                MonthlyInterest mi = new MonthlyInterest();
                String month = System.getProperty("month", YearMonth.now().toString()); // e.g., 2024-07
                mi.setMonthYear(month);
                mi.setAccountNumber(item.getAccount_number());
                BigDecimal balance;
                try { balance = new BigDecimal(item.getBalance()); } catch (Exception e) { balance = BigDecimal.ZERO; }
                BigDecimal rate = item.getType()!=null && item.getType().toLowerCase().contains("savings") ? new BigDecimal("0.003") : new BigDecimal("0.002");
                BigDecimal interest = balance.multiply(rate);
                mi.setInterestApplied(interest);
                mi.setFinalBalance(balance.add(interest));
                return mi;
            }
        };

        Step step = new StepBuilder("monthlyInterestStep", jobRepository)
                .<AccountCsv, MonthlyInterest>chunk(500, txManager)
                .reader(accountReader)
                .processor(processor)
                .writer(writer)
                .build();

        return new JobBuilder("monthlyInterestJob", jobRepository)
                .start(step)
                .build();
    }

    // Job 3: Estados de Cuenta Anuales
    @Bean
    public Job annualStatementJob(JobRepository jobRepository, PlatformTransactionManager txManager,
                                  FlatFileItemReader<TransactionCsv> transactionReader,
                                  EntityManagerFactory emf) {

        JpaItemWriter<AnnualStatement> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(emf);

        ItemProcessor<TransactionCsv, AnnualStatement> processor = new ItemProcessor<>() {
            // For simplicity, emit one row per transaction and deduplicate later with SQL views in analytics
            @Override
            public AnnualStatement process(TransactionCsv item) throws Exception {
                AnnualStatement as = new AnnualStatement();
                int year = LocalDate.parse(item.getTx_date()).getYear();
                as.setYear(year);
                as.setAccountNumber(item.getAccount_number());
                // naive mapping: deposits positive, withdrawals negative
                BigDecimal amt = new BigDecimal(item.getAmount());
                as.setOpeningBalance(BigDecimal.ZERO);
                as.setTotalDeposits(amt.signum() > 0 ? amt : BigDecimal.ZERO);
                as.setTotalWithdrawals(amt.signum() < 0 ? amt.abs() : BigDecimal.ZERO);
                as.setClosingBalance(BigDecimal.ZERO);
                return as;
            }
        };

        Step step = new StepBuilder("annualStatementStep", jobRepository)
                .<TransactionCsv, AnnualStatement>chunk(1000, txManager)
                .reader(transactionReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(500)
                .skip(Exception.class)
                .build();

        return new JobBuilder("annualStatementJob", jobRepository)
                .start(step)
                .build();
    }
}
