package com.example.demo.external.service;

import com.example.demo.announcement.domain.Announcement;
import com.example.demo.announcement.domain.SourceType;
import com.example.demo.external.lh.LhApiClient;
import com.example.demo.external.myhome.MyHomeApiClient;
import com.example.demo.external.myhome.dto.MyHomeNoticeApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class NoticeImportOrchestrator {

    private final LhApiClient lhApiClient;
    private final MyHomeApiClient myHomeApiClient;
    private final NoticeImportPersistenceService persistenceService;

    public NoticeImportOrchestrator(LhApiClient lhApiClient,
                                    MyHomeApiClient myHomeApiClient,
                                    NoticeImportPersistenceService persistenceService) {
        this.lhApiClient = lhApiClient;
        this.myHomeApiClient = myHomeApiClient;
        this.persistenceService = persistenceService;
    }

    public int importMyHomeNotices(int page, int size) {
        MyHomeNoticeApiResponse response = myHomeApiClient.fetchNoticeList(page, size);
        persistenceService.saveRaw(SourceType.MYHOME, "MYHOME_NOTICE_LIST", null, response);

        int count = 0;
        if (response != null && response.response() != null && response.response().body() != null
                && response.response().body().item() != null) {
            for (MyHomeNoticeApiResponse.Item item : response.response().body().item()) {
                persistenceService.upsertMyHome(item);
                count++;
            }
        }
        return count;
    }

    public int importLhNotices(int page, int size) {
        JsonNode response = lhApiClient.fetchNoticeList(page, size);
        persistenceService.saveRaw(SourceType.LH, "LH_NOTICE_LIST", null, response);

        JsonNode dsList = persistenceService.findArray(response, "dsList");
        int count = 0;
        if (dsList != null) {
            for (JsonNode item : dsList) {
                Announcement announcement = persistenceService.upsertLh(item);
                String ccrCnntSysDsCd = persistenceService.text(item, "CCR_CNNT_SYS_DS_CD");
                String splInfTpCd = persistenceService.text(item, "SPL_INF_TP_CD");
                if (ccrCnntSysDsCd != null && splInfTpCd != null) {
                    importLhDetail(announcement.getSourceNoticeId(), ccrCnntSysDsCd, splInfTpCd);
                }
                count++;
            }
        }
        return count;
    }

    public void importLhDetail(String panId, String ccrCnntSysDsCd, String splInfTpCd) {
        JsonNode response = lhApiClient.fetchNoticeDetail(panId, ccrCnntSysDsCd, splInfTpCd);
        persistenceService.saveRaw(SourceType.LH, "LH_NOTICE_DETAIL", panId, response);
        persistenceService.upsertLhDetail(panId, response);
    }
}
