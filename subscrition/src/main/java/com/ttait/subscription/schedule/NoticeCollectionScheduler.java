package com.ttait.subscription.schedule;

import com.ttait.subscription.external.service.NoticeImportOrchestrator;
import com.ttait.subscription.external.service.NoticeImportOrchestrator.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NoticeCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoticeCollectionScheduler.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_PAGES = 10;

    private final NoticeImportOrchestrator orchestrator;

    public NoticeCollectionScheduler(NoticeImportOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void collectLhNotices() {
        log.info("Starting daily LH notice collection");
        int totalImported = 0;
        int totalFailed = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            ImportResult result = orchestrator.importLhNotices(page, BATCH_SIZE);
            totalImported += result.imported();
            totalFailed += result.failed();

            if (result.imported() == 0 && result.failed() == 0) {
                log.info("No more notices at page={}, stopping", page);
                break;
            }
        }

        log.info("Daily LH collection completed: imported={}, failed={}", totalImported, totalFailed);
    }
}
