package com.ttait.subscription.schedule;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ttait.subscription.external.service.NoticeImportOrchestrator;
import com.ttait.subscription.external.service.NoticeImportOrchestrator.ImportResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeCollectionSchedulerTest {

    private static final int BATCH_SIZE = 100;

    @Mock
    private NoticeImportOrchestrator orchestrator;

    private NoticeCollectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NoticeCollectionScheduler(orchestrator);
    }

    @Test
    void unchangedOnlyPageDoesNotStopLaterPageAndEmptyPageStopsCollection() {
        given(orchestrator.importLhNoticesForScheduler(1, BATCH_SIZE))
                .willReturn(result(0, 0, 100, 100, 100, 100, false));
        given(orchestrator.importLhNoticesForScheduler(2, BATCH_SIZE))
                .willReturn(result(1, 0, 100, 100, 0, 0, false));
        given(orchestrator.importLhNoticesForScheduler(3, BATCH_SIZE))
                .willReturn(result(0, 0, 0, 0, 0, 0, true));

        scheduler.collectLhNotices();

        then(orchestrator).should().importLhNoticesForScheduler(1, BATCH_SIZE);
        then(orchestrator).should().importLhNoticesForScheduler(2, BATCH_SIZE);
        then(orchestrator).should().importLhNoticesForScheduler(3, BATCH_SIZE);
        then(orchestrator).should(never()).importLhNoticesForScheduler(4, BATCH_SIZE);
    }

    @Test
    void emptyFirstPageStopsCollectionImmediately() {
        given(orchestrator.importLhNoticesForScheduler(1, BATCH_SIZE))
                .willReturn(result(0, 0, 0, 0, 0, 0, true));

        scheduler.collectLhNotices();

        then(orchestrator).should().importLhNoticesForScheduler(1, BATCH_SIZE);
        then(orchestrator).should(never()).importLhNoticesForScheduler(2, BATCH_SIZE);
    }

    @Test
    void schedulerUsesSchedulerImportPathInsteadOfLegacyImport() {
        given(orchestrator.importLhNoticesForScheduler(1, BATCH_SIZE))
                .willReturn(result(0, 0, 0, 0, 0, 0, true));

        scheduler.collectLhNotices();

        then(orchestrator).should().importLhNoticesForScheduler(1, BATCH_SIZE);
        then(orchestrator).should(never()).importLhNotices(anyInt(), anyInt());
    }

    private ImportResult result(int imported,
                                int failed,
                                int fetched,
                                int scanned,
                                int unchanged,
                                int geminiSkipped,
                                boolean endOfList) {
        return new ImportResult(imported, failed, fetched, scanned, 0, unchanged, geminiSkipped, 0, endOfList);
    }
}
