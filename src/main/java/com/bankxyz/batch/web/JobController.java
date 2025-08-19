package com.bankxyz.batch.web;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

    private final JobLauncher jobLauncher;
    private final Job dailyReportJob;
    private final Job monthlyInterestJob;
    private final Job annualStatementJob;

    public JobController(JobLauncher jobLauncher, 
                        @Qualifier("dailyReportJob") Job dailyReportJob, 
                        @Qualifier("monthlyInterestJob") Job monthlyInterestJob, 
                        @Qualifier("annualStatementJob") Job annualStatementJob) {
        this.jobLauncher = jobLauncher;
        this.dailyReportJob = dailyReportJob;
        this.monthlyInterestJob = monthlyInterestJob;
        this.annualStatementJob = annualStatementJob;
    }

    @GetMapping("/jobs/run")
    public ResponseEntity<String> run(@RequestParam String name) throws Exception {
        Job job = switch (name) {
            case "dailyReportJob" -> dailyReportJob;
            case "monthlyInterestJob" -> monthlyInterestJob;
            case "annualStatementJob" -> annualStatementJob;
            default -> throw new IllegalArgumentException("Unknown job: " + name);
        };
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        JobExecution exec = jobLauncher.run(job, params);
        return ResponseEntity.ok("Started " + name + " with status " + exec.getStatus());
    }
}
