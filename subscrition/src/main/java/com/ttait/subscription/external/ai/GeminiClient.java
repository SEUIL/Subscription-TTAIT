package com.ttait.subscription.external.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private static final String SYSTEM_PROMPT = """
            You extract housing announcement info from Korean public housing notice PDFs.
            Return only valid JSON with this exact shape:
            {
              "noticeType": "임대"|"분양"|"분양전환"|"잔여세대"|"기타",
              "applicationPeriod": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "supplyHouseholdCount": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "depositMonthlyRent": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "depositAmountManwon": number|null,
              "monthlyRentAmountManwon": number|null,
              "salePriceMinManwon": number|null,
              "salePriceMaxManwon": number|null,
              "salePriceRaw": {"value": string|null, "confidence": number, "sourcePage": number|null},
              "scheduleDetails": [
                {"scheduleType": string, "startDate": string|null, "endDate": string|null}
              ],
              "importantNotes": {"value": string|null, "confidence": number, "sourcePage": number|null},
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
            - noticeType: check keywords first — "분양전환" → "분양전환", "잔여세대" → "잔여세대", else "임대"/"분양"/"기타".
            - scheduleDetails: extract ALL schedule items as separate array entries (청약신청/순번추첨/사전개방/계약체결/상시계약 등). Do NOT collapse into one. startDate/endDate as ISO or original text. endDate null for single-date events.
            - applicationPeriod: keep for backward compatibility, set to the main application period value.
            - salePriceMinManwon / salePriceMaxManwon: extract from "분양가격" or "분양금액" section in 만원 units (e.g. "1억8000만원" → 18000). null for 임대 notices.
            - salePriceRaw: verbatim text of the sale price section. null for 임대 notices.
            - depositAmountManwon: deposit in 만원 units. Valid only for noticeType=임대. null for 분양/분양전환.
            - monthlyRentAmountManwon: monthly rent in 만원 units. Valid only for noticeType=임대. null otherwise.
            - importantNotes: extract key caution/notice items (유의사항, 주의사항 section).
            - maritalTargetType: SINGLE(미혼), MARRIED(기혼일반), NEWLYWED(신혼부부 혼인n년이내), ENGAGED(예비신혼부부), ANY(무관). null if not mentioned.
            - marriageYearLimit: e.g. "혼인 7년 이내" → 7. null if not mentioned.
            - homelessRequired: true if 무주택 is required. null if not mentioned.
            - lowIncomeRequired: true if 기초수급자/차상위/저소득 is required. null if not mentioned.
            - elderlyRequired: true if targeting 고령자/만65세이상. null if not mentioned.
            - eligibilityRaw: verbatim full text of the eligibility/qualification section (신청자격, 입주자격 등). null if not found.
            - specialSupplyRaw: verbatim text of 특별공급/우선공급 conditions. null if not present.
            """;

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiClient(RestClient restClient, GeminiProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public PdfParseResult parsePdf(byte[] pdfBytes) {
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("inline_data", Map.of(
                                "mime_type", "application/pdf",
                                "data", base64
                        )),
                        Map.of("text", "위 PDF에서 공고 정보를 추출하세요.")
                ))),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        return callGemini(body, "pdf", pdfBytes.length);
    }

    public PdfParseResult parseText(String pdfText) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", pdfText)))),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        return callGemini(body, "text", pdfText.length());
    }

    private PdfParseResult callGemini(Map<String, Object> body, String mode, int size) {
        try {
            JsonNode response = restClient.post()
                    .uri(BASE_URL + "/models/" + properties.model() + ":generateContent?key=" + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String content = response.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text").asText();
            return objectMapper.readValue(content, PdfParseResult.class);

        } catch (Exception e) {
            log.error("Gemini parsing failed: mode={}, size={}", mode, size, e);
            return null;
        }
    }
}
