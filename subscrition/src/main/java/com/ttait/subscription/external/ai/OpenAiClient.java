package com.ttait.subscription.external.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private static final String SYSTEM_PROMPT = """
            You extract housing announcement info from Korean public rental notice PDFs.
            Return only valid JSON with this exact shape:
            {
              "applicationPeriod": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "supplyHouseholdCount": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "depositMonthlyRent": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "depositAmountManwon": number|null,
              "monthlyRentAmountManwon": number|null,
              "incomeAssetCriteria": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "contact": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "eligibility": {
                "ageMin": number|null,
                "ageMax": number|null,
                "ageRawText": string|null,
                "maritalTargetType": "SINGLE"|"MARRIED"|"NEWLYWED"|"ENGAGED"|"ANY"|null,
                "marriageYearLimit": number|null,
                "maritalRawText": string|null,
                "childrenMinCount": number|null,
                "childrenRawText": string|null,
                "homelessRequired": boolean|null,
                "homelessRawText": string|null,
                "lowIncomeRequired": boolean|null,
                "incomeAssetCriteriaRaw": string|null,
                "elderlyRequired": boolean|null,
                "elderlyAgeMin": number|null,
                "elderlyRawText": string|null,
                "eligibilityRaw": string|null,
                "specialSupplyRaw": string|null
              }
            }
            Rules:
            - Keep Korean text as-is.
            - confidence is 0.0 ~ 1.0. sourcePage starts from 1. If unknown, value=null and confidence=0.0.
            - depositAmountManwon: deposit in 만원 units (e.g. "보증금 2,000만원" → 2000). null if percentage-based or unknown.
            - monthlyRentAmountManwon: monthly rent in 만원 units. null if percentage-based or unknown.
            - maritalTargetType: SINGLE(미혼), MARRIED(기혼일반), NEWLYWED(신혼부부 혼인n년이내), ENGAGED(예비신혼부부), ANY(무관). null if not mentioned.
            - marriageYearLimit: e.g. "혼인 7년 이내" → 7. null if not mentioned.
            - homelessRequired: true if 무주택 is required. null if not mentioned.
            - lowIncomeRequired: true if 기초수급자/차상위/저소득 is required. null if not mentioned.
            - elderlyRequired: true if targeting 고령자/만65세이상. null if not mentioned.
            - eligibilityRaw: verbatim full text of the eligibility/qualification section (신청자격, 입주자격 등). null if not found.
            - specialSupplyRaw: verbatim text of 특별공급/우선공급 conditions. null if not present.
            """;

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiClient(RestClient restClient, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public PdfParseResult parse(String pdfText) {
        try {
            Map<String, Object> body = Map.of(
                    "model", properties.model(),
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", pdfText)
                    )
            );

            JsonNode response = restClient.post()
                    .uri(properties.baseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readValue(content, PdfParseResult.class);

        } catch (Exception e) {
            log.error("OpenAI parsing failed", e);
            return null;
        }
    }
}
