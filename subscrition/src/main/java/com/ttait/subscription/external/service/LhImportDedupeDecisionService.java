package com.ttait.subscription.external.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.announcement.domain.AnnouncementImportFingerprint;
import com.ttait.subscription.announcement.domain.AnnouncementImportParseStatus;
import com.ttait.subscription.announcement.domain.SourceType;
import com.ttait.subscription.announcement.repository.AnnouncementImportFingerprintRepository;
import com.ttait.subscription.announcement.repository.AnnouncementRepository;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LhImportDedupeDecisionService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementImportFingerprintRepository fingerprintRepository;
    private final CanonicalJsonHasher canonicalJsonHasher;

    public LhImportDedupeDecisionService(AnnouncementRepository announcementRepository,
                                         AnnouncementImportFingerprintRepository fingerprintRepository,
                                         CanonicalJsonHasher canonicalJsonHasher) {
        this.announcementRepository = announcementRepository;
        this.fingerprintRepository = fingerprintRepository;
        this.canonicalJsonHasher = canonicalJsonHasher;
    }

    public LhImportDedupeDecision decide(JsonNode item, JsonNode detailResponse, String pdfUrl, boolean force) {
        String panId = text(item, "PAN_ID");
        String itemHash = canonicalJsonHasher.hash(item);
        String detailHash = canonicalJsonHasher.hash(detailResponse);

        if (isLandNotice(item)) {
            return decision(LhImportDecisionType.LAND_SKIP, "LH land notice is outside housing import scope",
                    false, false, false, true, null, panId, itemHash, detailHash, pdfUrl);
        }

        Announcement announcement = findAnnouncement(panId);
        AnnouncementImportFingerprint fingerprint = announcement == null
                ? null
                : fingerprintRepository.findByAnnouncementId(announcement.getId()).orElse(null);
        Long announcementId = announcement == null ? null : announcement.getId();

        if (pdfUrl == null || pdfUrl.isBlank()) {
            boolean preserveParsedData = fingerprint != null && fingerprint.getParseStatus() == AnnouncementImportParseStatus.PARSED;
            return decision(LhImportDecisionType.NO_PDF, "LH detail response has no PDF attachment",
                    true, false, true, preserveParsedData, announcementId, panId, itemHash, detailHash, pdfUrl);
        }

        if (force) {
            return decision(LhImportDecisionType.FORCE_REPARSE, "force=true requested reparse",
                    true, true, true, false, announcementId, panId, itemHash, detailHash, pdfUrl);
        }

        if (announcement == null || fingerprint == null) {
            return decision(LhImportDecisionType.NEW, "no existing LH fingerprint found",
                    true, true, true, false, announcementId, panId, itemHash, detailHash, pdfUrl);
        }

        if (fingerprint.getParseStatus() == AnnouncementImportParseStatus.FAILED) {
            return decision(LhImportDecisionType.FAILED_RETRY, "previous Gemini parse failed",
                    true, true, true, false, announcementId, panId, itemHash, detailHash, pdfUrl);
        }

        if (!Objects.equals(itemHash, fingerprint.getLhItemHash())) {
            return decision(LhImportDecisionType.CHANGED_REPARSE, "LH list item JSON changed",
                    true, true, true, false, announcementId, panId, itemHash, detailHash, pdfUrl);
        }

        if (!Objects.equals(detailHash, fingerprint.getLhDetailHash())) {
            return decision(LhImportDecisionType.CHANGED_REPARSE, "LH detail JSON changed",
                    true, true, true, false, announcementId, panId, itemHash, detailHash, pdfUrl);
        }

        if (!Objects.equals(pdfUrl, fingerprint.getPdfUrl())) {
            return decision(LhImportDecisionType.CHANGED_REPARSE, "LH PDF URL changed",
                    true, true, true, false, announcementId, panId, itemHash, detailHash, pdfUrl);
        }

        return decision(LhImportDecisionType.UNCHANGED_SKIP_GEMINI, "LH fingerprints unchanged and previous parse succeeded",
                true, false, false, true, announcementId, panId, itemHash, detailHash, pdfUrl);
    }

    public void recordChecked(LhImportDedupeDecision decision) {
        if (decision.announcementId() == null) {
            return;
        }
        fingerprintRepository.findByAnnouncementId(decision.announcementId())
                .ifPresent(fingerprint -> {
                    fingerprint.markChecked(LocalDateTime.now());
                    fingerprintRepository.save(fingerprint);
                });
    }

    public void recordSuccess(Announcement announcement, LhImportDedupeDecision decision, String pdfRawJson) {
        AnnouncementImportParseStatus status = decision.decision() == LhImportDecisionType.NO_PDF
                ? AnnouncementImportParseStatus.PENDING
                : AnnouncementImportParseStatus.PARSED;
        LocalDateTime parsedAt = status == AnnouncementImportParseStatus.PARSED ? LocalDateTime.now() : null;
        upsertFingerprint(announcement, decision, canonicalJsonHasher.hashJson(pdfRawJson), status, parsedAt, null);
    }

    public void recordFailure(Announcement announcement, LhImportDedupeDecision decision, String errorMessage) {
        upsertFingerprint(announcement, decision, null, AnnouncementImportParseStatus.FAILED, null, errorMessage);
    }

    private void upsertFingerprint(Announcement announcement,
                                   LhImportDedupeDecision decision,
                                   String pdfAiJsonHash,
                                   AnnouncementImportParseStatus status,
                                   LocalDateTime lastParsedAt,
                                   String errorMessage) {
        if (announcement == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        AnnouncementImportFingerprint fingerprint = fingerprintRepository.findByAnnouncementId(announcement.getId())
                .orElseGet(() -> AnnouncementImportFingerprint.builder()
                        .announcement(announcement)
                        .build());
        fingerprint.updateImportFingerprint(
                decision.lhItemHash(),
                decision.lhDetailHash(),
                decision.pdfUrl(),
                null,
                pdfAiJsonHash,
                status,
                now,
                lastParsedAt,
                errorMessage
        );
        fingerprintRepository.save(fingerprint);
    }

    private Announcement findAnnouncement(String panId) {
        if (panId == null) {
            return null;
        }
        List<Announcement> candidates = announcementRepository.findSourcePairCandidates(SourceType.LH, panId);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private boolean isLandNotice(JsonNode item) {
        return "01".equals(text(item, "UPP_AIS_TP_CD"));
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return null;
        }
        String value = node.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LhImportDedupeDecision decision(LhImportDecisionType decision,
                                            String reason,
                                            boolean shouldFetchDetail,
                                            boolean shouldParseGemini,
                                            boolean shouldPersistOfficial,
                                            boolean preserveExistingParsedData,
                                            Long announcementId,
                                            String panId,
                                            String itemHash,
                                            String detailHash,
                                            String pdfUrl) {
        return new LhImportDedupeDecision(decision, reason, shouldFetchDetail, shouldParseGemini,
                shouldPersistOfficial, preserveExistingParsedData, announcementId, panId, itemHash, detailHash, pdfUrl);
    }
}
