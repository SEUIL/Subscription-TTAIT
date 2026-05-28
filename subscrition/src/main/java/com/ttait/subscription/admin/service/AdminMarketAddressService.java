package com.ttait.subscription.admin.service;

import com.ttait.subscription.admin.dto.AddressNormalizationResponse;
import com.ttait.subscription.admin.dto.LawdCodeMappingSeedRequest;
import com.ttait.subscription.admin.dto.LawdCodeMappingSeedResponse;
import com.ttait.subscription.admin.dto.LawdCodeMappingUpsertRequest;
import com.ttait.subscription.admin.dto.LawdCodeMappingUpsertResponse;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.service.AnnouncementUnitAddressEnrichmentService;
import com.ttait.subscription.market.domain.LawdCodeMapping;
import com.ttait.subscription.market.repository.LawdCodeMappingRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminMarketAddressService {

    private final LawdCodeMappingRepository lawdCodeMappingRepository;
    private final AnnouncementUnitAddressEnrichmentService addressEnrichmentService;

    public AdminMarketAddressService(LawdCodeMappingRepository lawdCodeMappingRepository,
                                     AnnouncementUnitAddressEnrichmentService addressEnrichmentService) {
        this.lawdCodeMappingRepository = lawdCodeMappingRepository;
        this.addressEnrichmentService = addressEnrichmentService;
    }

    @Transactional
    public LawdCodeMappingUpsertResponse upsertLawdCodeMappings(LawdCodeMappingUpsertRequest request) {
        List<LawdCodeMappingUpsertRequest.Item> mappings = request == null ? null : request.mappings();
        if (mappings == null || mappings.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "mappings is required");
        }

        int insertedCount = 0;
        int updatedCount = 0;
        for (LawdCodeMappingUpsertRequest.Item item : mappings) {
            validate(item);
            String regionLevel1 = normalize(item.regionLevel1());
            String regionLevel2 = normalize(item.regionLevel2());
            String legalDongName = normalize(item.legalDongName());
            String legalDongCode = item.legalDongCode().trim();

            LawdCodeMapping mapping = lawdCodeMappingRepository
                    .findFirstByRegionLevel2AndLegalDongNameAndLegalDongCode(
                            regionLevel2,
                            legalDongName,
                            legalDongCode)
                    .orElse(null);
            if (mapping == null) {
                lawdCodeMappingRepository.save(LawdCodeMapping.builder()
                        .regionLevel1(regionLevel1)
                        .regionLevel2(regionLevel2)
                        .legalDongName(legalDongName)
                        .legalDongCode(legalDongCode)
                        .active(item.active())
                        .build());
                insertedCount++;
            } else {
                mapping.updateMetadata(regionLevel1, item.active());
                updatedCount++;
            }
        }
        return new LawdCodeMappingUpsertResponse(mappings.size(), insertedCount, updatedCount);
    }

    @Transactional
    public LawdCodeMappingSeedResponse importLawdCodeMappings(LawdCodeMappingSeedRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "content is required");
        }

        LawdCodeSeedParseResult parsed = parseSeedContent(
                request.content(),
                request.delimiter(),
                request.activeOnlyOrDefault()
        );
        if (parsed.items().isEmpty()) {
            return new LawdCodeMappingSeedResponse(
                    parsed.dataLineCount(),
                    0,
                    parsed.skippedCount(),
                    0,
                    0
            );
        }

        LawdCodeMappingUpsertResponse upsertResponse = upsertLawdCodeMappings(
                new LawdCodeMappingUpsertRequest(parsed.items())
        );
        return new LawdCodeMappingSeedResponse(
                parsed.dataLineCount(),
                parsed.items().size(),
                parsed.skippedCount(),
                upsertResponse.insertedCount(),
                upsertResponse.updatedCount()
        );
    }

    public AddressNormalizationResponse normalizeAnnouncementUnits(Long announcementId, boolean retryNoLawdCode) {
        if (announcementId == null || announcementId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "announcementId must be positive");
        }
        AnnouncementUnitAddressEnrichmentService.AddressEnrichmentResult result =
                addressEnrichmentService.enrichUnits(announcementId, retryNoLawdCode);
        return new AddressNormalizationResponse(
                result.announcementId(),
                result.processedCount(),
                result.successCount(),
                result.noAddressCount(),
                result.noLawdCodeCount()
        );
    }

    private LawdCodeSeedParseResult parseSeedContent(String content, String delimiter, boolean activeOnly) {
        List<String> lines = content.lines()
                .map(line -> line.replace("\ufeff", ""))
                .toList();
        int firstLineIndex = firstDataLineIndex(lines);
        if (firstLineIndex < 0) {
            return new LawdCodeSeedParseResult(0, 0, List.of());
        }

        String resolvedDelimiter = resolveDelimiter(delimiter, lines.get(firstLineIndex));
        List<String> firstFields = splitDelimited(lines.get(firstLineIndex), resolvedDelimiter);
        boolean hasHeader = hasSeedHeader(firstFields);
        HeaderIndexes indexes = hasHeader
                ? headerIndexes(firstFields)
                : new HeaderIndexes(0, 1, 2);
        int startIndex = hasHeader ? firstLineIndex + 1 : firstLineIndex;

        int dataLineCount = 0;
        int skippedCount = 0;
        List<LawdCodeMappingUpsertRequest.Item> items = new ArrayList<>();
        for (int lineIndex = startIndex; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            if (!StringUtils.hasText(line)) {
                continue;
            }
            dataLineCount++;
            List<String> fields = splitDelimited(line, resolvedDelimiter);
            if (!hasRequiredFields(fields, indexes)) {
                skippedCount++;
                continue;
            }

            String legalDongCode = normalize(fields.get(indexes.codeIndex()));
            String fullName = normalize(fields.get(indexes.nameIndex()));
            boolean active = isActiveStatus(valueAt(fields, indexes.statusIndex()));
            ParsedLawdAddress address = parseLawdAddress(fullName);
            if (!validSeedRow(legalDongCode, address) || activeOnly && !active) {
                skippedCount++;
                continue;
            }

            items.add(new LawdCodeMappingUpsertRequest.Item(
                    address.regionLevel1(),
                    address.regionLevel2(),
                    address.legalDongName(),
                    legalDongCode,
                    active
            ));
        }
        return new LawdCodeSeedParseResult(dataLineCount, skippedCount, items);
    }

    private int firstDataLineIndex(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            if (StringUtils.hasText(lines.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private String resolveDelimiter(String requestedDelimiter, String sampleLine) {
        if (StringUtils.hasText(requestedDelimiter)) {
            String normalized = requestedDelimiter.trim();
            if ("tab".equalsIgnoreCase(normalized) || "\\t".equals(normalized)) {
                return "\t";
            }
            return normalized.substring(0, 1);
        }
        if (sampleLine.contains("\t")) {
            return "\t";
        }
        if (sampleLine.contains(",")) {
            return ",";
        }
        if (sampleLine.contains("|")) {
            return "|";
        }
        if (sampleLine.contains(";")) {
            return ";";
        }
        return ",";
    }

    private List<String> splitDelimited(String line, String delimiter) {
        char delimiterChar = delimiter.charAt(0);
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == delimiterChar && !quoted) {
                fields.add(normalize(field.toString()));
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        fields.add(normalize(field.toString()));
        return fields;
    }

    private boolean hasSeedHeader(List<String> fields) {
        return fields.stream()
                .map(field -> field.toLowerCase(Locale.ROOT))
                .anyMatch(field -> field.contains("법정동코드")
                        || field.contains("법정동 코드")
                        || field.equals("code")
                        || field.contains("legaldongcode"));
    }

    private HeaderIndexes headerIndexes(List<String> fields) {
        int codeIndex = findHeaderIndex(fields, "법정동코드", "법정동 코드", "code", "legaldongcode");
        int nameIndex = findHeaderIndex(fields, "법정동명", "법정동 주소", "법정동주소", "name", "address");
        int statusIndex = findHeaderIndex(fields, "폐지", "상태", "사용여부", "active");
        if (codeIndex < 0 || nameIndex < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "seed header must include legal dong code and name columns");
        }
        return new HeaderIndexes(codeIndex, nameIndex, statusIndex);
    }

    private int findHeaderIndex(List<String> fields, String... candidates) {
        for (int index = 0; index < fields.size(); index++) {
            String field = fields.get(index).toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
            for (String candidate : candidates) {
                String normalizedCandidate = candidate.toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
                if (field.contains(normalizedCandidate)) {
                    return index;
                }
            }
        }
        return -1;
    }

    private boolean hasRequiredFields(List<String> fields, HeaderIndexes indexes) {
        return fields.size() > indexes.codeIndex()
                && fields.size() > indexes.nameIndex()
                && indexes.codeIndex() >= 0
                && indexes.nameIndex() >= 0;
    }

    private String valueAt(List<String> fields, int index) {
        return index >= 0 && fields.size() > index ? fields.get(index) : null;
    }

    private boolean isActiveStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !(normalized.contains("폐지")
                || normalized.equals("n")
                || normalized.equals("no")
                || normalized.equals("false")
                || normalized.equals("0")
                || normalized.equals("deleted"));
    }

    private ParsedLawdAddress parseLawdAddress(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return null;
        }
        String[] tokens = Arrays.stream(fullName.split(" "))
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        if (tokens.length < 2) {
            return null;
        }

        int regionIndex = inferRegionLevel2Index(tokens);
        if (regionIndex < 0 || regionIndex + 1 >= tokens.length) {
            return null;
        }
        return new ParsedLawdAddress(tokens[0], tokens[regionIndex], tokens[regionIndex + 1]);
    }

    private int inferRegionLevel2Index(String[] tokens) {
        if (tokens.length >= 3 && tokens[0].endsWith("도") && tokens[1].endsWith("시") && tokens[2].endsWith("구")) {
            return 2;
        }
        if (tokens.length >= 2 && tokens[0].endsWith("시") && tokens[1].endsWith("구")) {
            return 1;
        }
        if (tokens.length >= 2 && isProvinceLevelToken(tokens[0]) && isRegionLevel2Token(tokens[1])) {
            return 1;
        }
        if (isRegionLevel2Token(tokens[0])) {
            return 0;
        }
        return -1;
    }

    private boolean validSeedRow(String legalDongCode, ParsedLawdAddress address) {
        return StringUtils.hasText(legalDongCode)
                && legalDongCode.matches("\\d{10}")
                && !legalDongCode.endsWith("00000")
                && address != null
                && StringUtils.hasText(address.regionLevel2())
                && StringUtils.hasText(address.legalDongName());
    }

    private void validate(LawdCodeMappingUpsertRequest.Item item) {
        if (item == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "mapping item is required");
        }
        if (!StringUtils.hasText(item.regionLevel2())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "regionLevel2 is required");
        }
        if (!StringUtils.hasText(item.legalDongName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "legalDongName is required");
        }
        if (!StringUtils.hasText(item.legalDongCode()) || !item.legalDongCode().trim().matches("\\d{10}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "legalDongCode must be 10 digits");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", " ") : null;
    }

    private boolean isProvinceLevelToken(String token) {
        return token.endsWith("도")
                || token.endsWith("광역시")
                || token.endsWith("특별시")
                || token.endsWith("특별자치시")
                || token.endsWith("특별자치도");
    }

    private boolean isRegionLevel2Token(String token) {
        return StringUtils.hasText(token)
                && (token.endsWith("시") || token.endsWith("군") || token.endsWith("구"));
    }

    private record HeaderIndexes(int codeIndex, int nameIndex, int statusIndex) {
    }

    private record ParsedLawdAddress(String regionLevel1, String regionLevel2, String legalDongName) {
    }

    private record LawdCodeSeedParseResult(
            int dataLineCount,
            int skippedCount,
            List<LawdCodeMappingUpsertRequest.Item> items
    ) {
    }
}
