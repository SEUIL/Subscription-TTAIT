package com.ttait.subscription.external.lh;

import com.ttait.subscription.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "lh_import_candidate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lh_import_candidate_pan_id",
                columnNames = "pan_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LhImportCandidate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pan_id", nullable = false, length = 100)
    private String panId;

    @Column(name = "ccr_cnnt_sys_ds_cd", length = 20)
    private String ccrCnntSysDsCd;

    @Column(name = "spl_inf_tp_cd", length = 20)
    private String splInfTpCd;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "region_level1", length = 100)
    private String regionLevel1;

    @Column(name = "notice_status_raw", length = 50)
    private String noticeStatusRaw;

    @Column(name = "source_notice_url", length = 500)
    private String sourceNoticeUrl;

    @Column(name = "pdf_url", length = 1000)
    private String pdfUrl;

    @Column(name = "is_land_notice", nullable = false)
    private boolean landNotice;

    @Column(name = "is_commercial_notice", nullable = false)
    private boolean commercialNotice;

    @Column(name = "already_imported", nullable = false)
    private boolean alreadyImported;

    @Column(name = "can_parse", nullable = false)
    private boolean canParse;

    @Column(name = "dedupe_status", length = 50)
    private String dedupeStatus;

    @Column(name = "skip_reason", length = 500)
    private String skipReason;

    @Column(name = "list_raw_json", columnDefinition = "TEXT")
    private String listRawJson;

    @Column(name = "detail_raw_json", columnDefinition = "TEXT")
    private String detailRawJson;

    @Column(name = "list_hash", length = 64)
    private String listHash;

    @Column(name = "detail_hash", length = 64)
    private String detailHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LhImportCandidateStatus status;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    public LhImportCandidate(String panId) {
        this.panId = panId;
        this.status = LhImportCandidateStatus.COLLECTED;
        this.collectedAt = LocalDateTime.now();
    }

    public void updateCollected(String ccrCnntSysDsCd,
                                String splInfTpCd,
                                String title,
                                String regionLevel1,
                                String noticeStatusRaw,
                                String sourceNoticeUrl,
                                 String pdfUrl,
                                 boolean landNotice,
                                 boolean commercialNotice,
                                 boolean alreadyImported,
                                 boolean canParse,
                                 String dedupeStatus,
                                 String skipReason,
                                 String listRawJson,
                                 String detailRawJson,
                                 String listHash,
                                String detailHash) {
        this.ccrCnntSysDsCd = ccrCnntSysDsCd;
        this.splInfTpCd = splInfTpCd;
        this.title = title;
        this.regionLevel1 = regionLevel1;
        this.noticeStatusRaw = noticeStatusRaw;
        this.sourceNoticeUrl = sourceNoticeUrl;
        this.pdfUrl = pdfUrl;
        this.landNotice = landNotice;
        this.commercialNotice = commercialNotice;
        this.alreadyImported = alreadyImported;
        this.canParse = canParse;
        this.dedupeStatus = dedupeStatus;
        this.skipReason = skipReason;
        this.listRawJson = listRawJson;
        this.detailRawJson = detailRawJson;
        this.listHash = listHash;
        this.detailHash = detailHash;
        this.status = landNotice || commercialNotice ? LhImportCandidateStatus.SKIPPED : LhImportCandidateStatus.COLLECTED;
        this.collectedAt = LocalDateTime.now();
    }

    public void updateCollected(String ccrCnntSysDsCd,
                                String splInfTpCd,
                                String title,
                                String regionLevel1,
                                String noticeStatusRaw,
                                String sourceNoticeUrl,
                                String pdfUrl,
                                boolean landNotice,
                                boolean alreadyImported,
                                boolean canParse,
                                String dedupeStatus,
                                String listRawJson,
                                String detailRawJson,
                                String listHash,
                                String detailHash) {
        updateCollected(ccrCnntSysDsCd, splInfTpCd, title, regionLevel1, noticeStatusRaw, sourceNoticeUrl, pdfUrl,
                landNotice, false, alreadyImported, canParse, dedupeStatus, null, listRawJson, detailRawJson,
                listHash, detailHash);
    }

    public void updateCollected(String title,
                                String region,
                                String sourceNoticeUrl,
                                 String pdfUrl,
                                 boolean landNotice,
                                 boolean commercialNotice,
                                 boolean alreadyImported,
                                 boolean canParse,
                                 String dedupeStatus,
                                 String skipReason,
                                 String itemJson,
                                 String detailJson) {
        updateCollected(null, null, title, region, null, sourceNoticeUrl, pdfUrl, landNotice, commercialNotice,
                alreadyImported, canParse, dedupeStatus, skipReason, itemJson, detailJson, null, null);
    }

    public void updateCollected(String title,
                                String region,
                                String sourceNoticeUrl,
                                String pdfUrl,
                                boolean landNotice,
                                boolean alreadyImported,
                                boolean canParse,
                                String dedupeStatus,
                                String itemJson,
                                String detailJson) {
        updateCollected(title, region, sourceNoticeUrl, pdfUrl, landNotice, false, alreadyImported, canParse,
                dedupeStatus, null, itemJson, detailJson);
    }

    public String getRegion() {
        return regionLevel1;
    }

    public String getItemJson() {
        return listRawJson;
    }

    public String getDetailJson() {
        return detailRawJson;
    }

    public void markImported() {
        this.status = LhImportCandidateStatus.IMPORTED;
    }

    public void markSkipped() {
        this.status = LhImportCandidateStatus.SKIPPED;
    }

    public void markFailed() {
        this.status = LhImportCandidateStatus.FAILED;
    }
}
