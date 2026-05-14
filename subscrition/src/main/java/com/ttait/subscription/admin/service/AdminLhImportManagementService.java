package com.ttait.subscription.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.admin.dto.LhCandidateCollectionResponse;
import com.ttait.subscription.admin.dto.LhImportCandidateListResponse;
import com.ttait.subscription.admin.dto.LhImportCandidateResponse;
import com.ttait.subscription.admin.dto.LhImportRunResult;
import com.ttait.subscription.admin.dto.LhSelectedImportRequest;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.lh.LhApiClient;
import com.ttait.subscription.external.lh.LhImportCandidate;
import com.ttait.subscription.external.lh.LhImportCandidateRepository;
import com.ttait.subscription.external.lh.LhImportCandidateStatus;
import com.ttait.subscription.external.service.LhImportDecisionType;
import com.ttait.subscription.external.service.NoticeImportOrchestrator;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminLhImportManagementService {

    private final LhApiClient lhApiClient;
    private final LhImportCandidateRepository candidateRepository;
    private final NoticeImportOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final CanonicalJsonHasher canonicalJsonHasher;

    public AdminLhImportManagementService(LhApiClient lhApiClient,
                                          LhImportCandidateRepository candidateRepository,
                                          NoticeImportOrchestrator orchestrator,
                                          ObjectMapper objectMapper,
                                          CanonicalJsonHasher canonicalJsonHasher) {
        this.lhApiClient = lhApiClient;
        this.candidateRepository = candidateRepository;
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
        this.canonicalJsonHasher = canonicalJsonHasher;
    }

    @Transactional
    public LhCandidateCollectionResponse collectCandidates(int page, int size) {
        JsonNode response = lhApiClient.fetchNoticeList(page, size);
        JsonNode dsList = findArray(response, "dsList");
        if (dsList == null || dsList.isEmpty()) {
            return new LhCandidateCollectionResponse(0, 0, 0, List.of());
        }

        int scanned = 0;
        int skippedLand = 0;
        List<LhImportCandidateResponse> candidates = new ArrayList<>();
        for (JsonNode item : dsList) {
            scanned++;
            String panId = text(item, "PAN_ID");
            if (panId == null) {
                continue;
            }

            boolean landNotice = isLandNotice(item);
            JsonNode detailResponse = null;
            if (!landNotice) {
                detailResponse = fetchDetail(item);
            }

            NoticeImportOrchestrator.CandidateScanResult scan = orchestrator.scanLhCandidate(item, detailResponse, false);
            if (scan.decision().decision() == LhImportDecisionType.LAND_SKIP) {
                skippedLand++;
                landNotice = true;
            }

            LhImportCandidate candidate = candidateRepository.findByPanId(panId)
                    .orElseGet(() -> new LhImportCandidate(panId));
            candidate.updateCollected(
                    text(item, "CCR_CNNT_SYS_DS_CD"),
                    text(item, "SPL_INF_TP_CD"),
                    text(item, "PAN_NM"),
                    text(item, "CNP_CD_NM"),
                    text(item, "PAN_SS"),
                    text(item, "DTL_URL"),
                    scan.pdfUrl(),
                    landNotice,
                    scan.decision().announcementId() != null,
                    !landNotice && scan.pdfUrl() != null && !scan.pdfUrl().isBlank(),
                    scan.decision().decision().name(),
                    writeJson(item),
                    writeJson(detailResponse),
                    canonicalJsonHasher.hash(item),
                    canonicalJsonHasher.hash(detailResponse)
            );
            LhImportCandidate saved = candidateRepository.save(candidate);
            candidates.add(toResponse(saved));
        }

        return new LhCandidateCollectionResponse(dsList.size(), scanned, skippedLand, candidates);
    }

    @Transactional(readOnly = true)
    public LhImportCandidateListResponse listCandidates(int page, int size, String status) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<LhImportCandidate> candidates = status == null || status.isBlank()
                ? candidateRepository.findAll(pageable)
                : candidateRepository.findByStatus(parseStatus(status), pageable);

        return new LhImportCandidateListResponse(
                candidates.getContent().stream().map(this::toResponse).toList(),
                candidates.getTotalElements()
        );
    }

    @Transactional
    public LhImportRunResult importSelected(LhSelectedImportRequest request) {
        List<Long> candidateIds = request == null ? null : request.candidateIds();
        if (candidateIds == null || candidateIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "candidateIds is required");
        }

        List<LhImportCandidate> candidates = candidateRepository.findByIdIn(candidateIds);
        ensureAllCandidatesExist(candidateIds, candidates);

        RunCounters counters = new RunCounters(candidateIds.size());
        for (LhImportCandidate candidate : candidates) {
            if (candidate.isLandNotice()) {
                counters.skippedLand++;
                candidate.markSkipped();
                candidateRepository.save(candidate);
                continue;
            }

            NoticeImportOrchestrator.ImportResult result = orchestrator.importPreparedLhNotices(
                    List.of(new NoticeImportOrchestrator.PreparedLhNotice(
                            readJson(candidate.getItemJson()),
                            readJson(candidate.getDetailJson())
                    )),
                    request.forceOrDefault()
            );
            counters.add(result);
            if (result.failed() > 0) {
                candidate.markFailed();
            } else {
                candidate.markImported();
            }
            candidateRepository.save(candidate);
        }

        return counters.toResult();
    }

    public LhImportRunResult forceReparse(Long announcementId) {
        orchestrator.reimportAnnouncement(announcementId);
        return new LhImportRunResult(1, 1, 0, 0, 0, 1, 1, 0);
    }

    private JsonNode fetchDetail(JsonNode item) {
        String panId = text(item, "PAN_ID");
        String ccrCnntSysDsCd = text(item, "CCR_CNNT_SYS_DS_CD");
        String splInfTpCd = text(item, "SPL_INF_TP_CD");
        if (panId == null || ccrCnntSysDsCd == null || splInfTpCd == null) {
            return null;
        }
        return lhApiClient.fetchNoticeDetail(panId, ccrCnntSysDsCd, splInfTpCd);
    }

    private LhImportCandidateResponse toResponse(LhImportCandidate candidate) {
        return new LhImportCandidateResponse(
                candidate.getId(),
                candidate.getPanId(),
                candidate.getTitle(),
                candidate.getRegion(),
                candidate.getStatus().name(),
                candidate.getSourceNoticeUrl(),
                candidate.getPdfUrl(),
                candidate.isLandNotice(),
                candidate.isAlreadyImported(),
                candidate.isCanParse(),
                candidate.getDedupeStatus()
        );
    }

    private LhImportCandidateStatus parseStatus(String status) {
        try {
            return LhImportCandidateStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid candidate status: " + status);
        }
    }

    private void ensureAllCandidatesExist(List<Long> candidateIds, List<LhImportCandidate> candidates) {
        Set<Long> foundIds = new HashSet<>();
        for (LhImportCandidate candidate : candidates) {
            foundIds.add(candidate.getId());
        }
        for (Long candidateId : candidateIds) {
            if (!foundIds.contains(candidateId)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "LH import candidate not found: " + candidateId);
            }
        }
    }

    private JsonNode findArray(JsonNode root, String fieldName) {
        if (root == null || !root.isArray()) {
            return null;
        }
        for (JsonNode node : root) {
            JsonNode candidate = node.get(fieldName);
            if (candidate != null && candidate.isArray()) {
                return candidate;
            }
        }
        return null;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) {
            return null;
        }
        String value = node.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isLandNotice(JsonNode item) {
        return "01".equals(text(item, "UPP_AIS_TP_CD"));
    }

    private String writeJson(JsonNode node) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to serialize LH candidate JSON");
        }
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to parse stored LH candidate JSON");
        }
    }

    private static class RunCounters {
        private final int fetched;
        private int scanned;
        private int skippedLand;
        private int unchanged;
        private int geminiSkipped;
        private int imported;
        private int reparsed;
        private int failed;

        private RunCounters(int fetched) {
            this.fetched = fetched;
            this.scanned = fetched;
        }

        private void add(NoticeImportOrchestrator.ImportResult result) {
            skippedLand += result.skippedLand();
            unchanged += result.unchanged();
            geminiSkipped += result.geminiSkipped();
            imported += result.imported();
            reparsed += result.reparsed();
            failed += result.failed();
        }

        private LhImportRunResult toResult() {
            return new LhImportRunResult(fetched, scanned, skippedLand, unchanged, geminiSkipped, imported, reparsed, failed);
        }
    }
}
