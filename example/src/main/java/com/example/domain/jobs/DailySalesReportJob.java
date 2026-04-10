package com.example.domain.jobs;

import com.onec.annotations.ScheduledJob;
import com.onec.jobs.BackgroundTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ScheduledJob(name = "DailySalesReport", cron = "0 0 2 * * *")
public class DailySalesReportJob implements BackgroundTask {

    private static final Logger log = LoggerFactory.getLogger(DailySalesReportJob.class);

    @Override
    public void execute() {
        log.info("Generating daily sales report...");
        // In a real application: query SalesRegister, aggregate, generate report
    }
}
