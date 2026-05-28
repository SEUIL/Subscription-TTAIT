package com.ttait.subscription.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import org.junit.jupiter.api.Test;

class RtmsCollectionDtoJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestDefaultsPageNoAndNumOfRowsWhenOmitted() throws Exception {
        RtmsCollectionRequest request = objectMapper.readValue("""
                {
                  "sourceType":"APT_RENT",
                  "lawdCd":"41570",
                  "dealYm":"202405",
                  "ignored":"value"
                }
                """, RtmsCollectionRequest.class);

        assertThat(request.sourceType()).isEqualTo(RtmsSourceType.APT_RENT);
        assertThat(request.pageNoOrDefault()).isEqualTo(1);
        assertThat(request.numOfRowsOrDefault()).isEqualTo(100);
    }

    @Test
    void responseSerializesCollectionSummaryFields() throws Exception {
        String json = objectMapper.writeValueAsString(new RtmsCollectionResponse(
                "APT_RENT", "41570", "202405", "SUCCESS", 2, 1, 1, 0, null));

        assertThat(json).contains("\"sourceType\":\"APT_RENT\"");
        assertThat(json).contains("\"lawdCd\":\"41570\"");
        assertThat(json).contains("\"dealYm\":\"202405\"");
        assertThat(json).contains("\"status\":\"SUCCESS\"");
        assertThat(json).contains("\"fetchedCount\":2");
        assertThat(json).contains("\"savedCount\":1");
        assertThat(json).contains("\"duplicateCount\":1");
        assertThat(json).contains("\"failedCount\":0");
    }
}
