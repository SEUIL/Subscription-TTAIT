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
        int totalFetched = 0;
        int totalScanned = 0;
        int totalImported = 0;
        int totalUnchanged = 0;
        int totalGeminiSkipped = 0;
        int totalFailed = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            ImportResult result = orchestrator.importLhNoticesForScheduler(page, BATCH_SIZE);
            totalFetched += result.fetched();
            totalScanned += result.scanned();
            totalImported += result.imported();
            totalUnchanged += result.unchanged();
            totalGeminiSkipped += result.geminiSkipped();
            totalFailed += result.failed();

            if (result.endOfList()) {
                log.info("LH notice list ended at page={}, stopping", page);
                break;
            }
        }

        log.info(
                "Daily LH collection completed: fetched={}, scanned={}, imported={}, unchanged={}, geminiSkipped={}, failed={}",
                totalFetched,
                totalScanned,
                totalImported,
                totalUnchanged,
                totalGeminiSkipped,
                totalFailed
        );
    }
}
