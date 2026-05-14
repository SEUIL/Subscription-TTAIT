package com.ttait.subscription.external.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ttait.subscription.external.support.AnnouncementNormalizer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LhUnitCandidateExtractor {

    private final AnnouncementNormalizer normalizer;

    public LhUnitCandidateExtractor(AnnouncementNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public List<LhUnitCandidate> extract(String panId,
                                         JsonNode detailResponse,
                                         String supplyTypeRaw,
                                         String houseTypeRaw,
                                         String regionLevel1) {
        JsonNode dsSbd = findArray(detailResponse, "dsSbd");
        if (dsSbd == null || !dsSbd.isArray()) {
            return List.of();
        }

        List<LhUnitCandidate> candidates = new ArrayList<>();
        int order = 0;
        for (JsonNode row : dsSbd) {
            if (!hasMeaningfulUnitData(row)) {
                continue;
            }
            String complexName = firstNonBlank(text(row, "LCC_NT_NM"), text(row, "SBD_NM"));
            String fullAddress = text(row, "LGDN_ADR");
            String exclusiveAreaText = text(row, "DDO_AR");
            Integer supplyHouseholdCount = intValue(row, "HSH_CNT");
            String sourceUnitKey = buildSourceUnitKey(panId, row, order);
            String effectiveHouseTypeRaw = houseTypeRaw;

            candidates.add(new LhUnitCandidate(
                    sourceUnitKey,
                    order,
                    complexName,
                    fullAddress,
                    regionLevel1,
                    extractRegionLevel2(fullAddress),
                    supplyTypeRaw,
                    normalizer.normalizeSupplyType(supplyTypeRaw),
                    effectiveHouseTypeRaw,
                    normalizer.normalizeHouseType(effectiveHouseTypeRaw),
                    exclusiveAreaText,
                    parseSingleArea(exclusiveAreaText),
                    supplyHouseholdCount,
                    row.toString()
            ));
            order++;
        }
        return candidates;
    }

    private boolean hasMeaningfulUnitData(JsonNode row) {
        return text(row, "LCC_NT_NM") != null
                || text(row, "SBD_NM") != null
                || text(row, "LGDN_ADR") != null
                || text(row, "LGDN_DTL_ADR") != null
                || text(row, "HSH_CNT") != null
                || text(row, "DDO_AR") != null;
    }

    private String buildSourceUnitKey(String panId, JsonNode row, int rowIndex) {
        String seed = String.join("|",
                nullToEmpty(panId),
                nullToEmpty(text(row, "SBD_LGO_NM")),
                nullToEmpty(text(row, "LCC_NT_NM")),
                nullToEmpty(text(row, "SBD_NM")),
                nullToEmpty(text(row, "LGDN_ADR")),
                nullToEmpty(text(row, "DDO_AR")),
                nullToEmpty(text(row, "HSH_CNT")),
                String.valueOf(rowIndex)
        );
        return sha256(seed).substring(0, 32);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build LH unit key", e);
        }
    }

    private JsonNode findArray(JsonNode root, String fieldName) {
        if (root == null || !root.isArray()) return null;
        for (JsonNode node : root) {
            JsonNode candidate = node.get(fieldName);
            if (candidate != null && candidate.isArray()) return candidate;
        }
        return null;
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.get(fieldName) == null || node.get(fieldName).isNull()) return null;
        String value = node.get(fieldName).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer intValue(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value == null) return null;
        try {
            return Integer.parseInt(value.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseSingleArea(String text) {
        if (text == null) return null;
        String[] matches = text.replaceAll("[^0-9.]+", " ").trim().split("\\s+");
        List<String> numbers = new ArrayList<>();
        for (String match : matches) {
            if (!match.isBlank()) numbers.add(match);
        }
        if (numbers.size() != 1) return null;
        try {
            return new BigDecimal(numbers.get(0));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractRegionLevel2(String fullAddress) {
        if (fullAddress == null) return null;
        String[] parts = fullAddress.trim().split("\\s+");
        return parts.length >= 2 ? parts[1] : null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
