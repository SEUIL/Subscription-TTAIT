package com.ttait.subscription.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.external.service.NoticeImportOrchestrator;
import java.util.List;
import org.junit.jupiter.api.Test;

class LhImportDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void candidateResponseSerializesStableApidogFields() throws Exception {
        LhImportCandidateResponse response = new LhImportCandidateResponse(
                1L,
                "PAN-001",
                "LH test notice",
                "Seoul",
                "COLLECTED",
                "https://example.com/notice",
                "https://example.com/notice.pdf",
                false,
                true,
                true,
                "UNCHANGED"
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertThat(json.get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("panId").asText()).isEqualTo("PAN-001");
        assertThat(json.get("title").asText()).isEqualTo("LH test notice");
        assertThat(json.get("region").asText()).isEqualTo("Seoul");
        assertThat(json.get("status").asText()).isEqualTo("COLLECTED");
        assertThat(json.get("sourceNoticeUrl").asText()).isEqualTo("https://example.com/notice");
        assertThat(json.get("pdfUrl").asText()).isEqualTo("https://example.com/notice.pdf");
        assertThat(json.get("isLandNotice").asBoolean()).isFalse();
        assertThat(json.get("alreadyImported").asBoolean()).isTrue();
        assertThat(json.get("canParse").asBoolean()).isTrue();
        assertThat(json.get("dedupeStatus").asText()).isEqualTo("UNCHANGED");
    }

    @Test
    void selectedImportRequestDefaultsOmittedForceToFalseForServiceUse() throws Exception {
        LhSelectedImportRequest request = objectMapper.readValue(
                "{\"candidateIds\":[1,2]}",
                LhSelectedImportRequest.class
        );

        assertThat(request.candidateIds()).containsExactly(1L, 2L);
        assertThat(request.force()).isNull();
        assertThat(request.forceOrDefault()).isFalse();

        LhSelectedImportRequest forced = objectMapper.readValue(
                "{\"candidateIds\":[1],\"force\":true}",
                LhSelectedImportRequest.class
        );

        assertThat(forced.forceOrDefault()).isTrue();
    }

    @Test
    void candidateCollectionAndListResponsesSerializeExplicitShapes() throws Exception {
        LhImportCandidateResponse candidate = new LhImportCandidateResponse(
                7L,
                "PAN-007",
                "Candidate notice",
                "Busan",
                "READY",
                "https://example.com/notice/7",
                null,
                false,
                false,
                true,
                "NEW"
        );

        JsonNode collection = objectMapper.readTree(objectMapper.writeValueAsString(
                new LhCandidateCollectionResponse(10, 8, 2, List.of(candidate))
        ));
        JsonNode list = objectMapper.readTree(objectMapper.writeValueAsString(
                new LhImportCandidateListResponse(List.of(candidate), 1L)
        ));

        assertThat(collection.get("fetched").asInt()).isEqualTo(10);
        assertThat(collection.get("scanned").asInt()).isEqualTo(8);
        assertThat(collection.get("skippedLand").asInt()).isEqualTo(2);
        assertThat(collection.get("candidates")).hasSize(1);
        assertThat(list.get("candidates")).hasSize(1);
        assertThat(list.get("totalCount").asLong()).isEqualTo(1L);
    }

    @Test
    void importRunResultSerializesDedupeRichCounters() throws Exception {
        LhImportRunResult result = new LhImportRunResult(
                10,
                9,
                1,
                2,
                2,
                4,
                1,
                1
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(result));

        assertThat(json.get("fetched").asInt()).isEqualTo(10);
        assertThat(json.get("scanned").asInt()).isEqualTo(9);
        assertThat(json.get("skippedLand").asInt()).isEqualTo(1);
        assertThat(json.get("unchanged").asInt()).isEqualTo(2);
        assertThat(json.get("geminiSkipped").asInt()).isEqualTo(2);
        assertThat(json.get("imported").asInt()).isEqualTo(4);
        assertThat(json.get("reparsed").asInt()).isEqualTo(1);
        assertThat(json.get("failed").asInt()).isEqualTo(1);
    }

    @Test
    void oldImportResultKeepsImportedAndFailedJsonCompatibility() throws Exception {
        NoticeImportOrchestrator.ImportResult result = new NoticeImportOrchestrator.ImportResult(3, 1);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(result));

        assertThat(json).hasSize(2);
        assertThat(json.get("imported").asInt()).isEqualTo(3);
        assertThat(json.get("failed").asInt()).isEqualTo(1);
    }
}
