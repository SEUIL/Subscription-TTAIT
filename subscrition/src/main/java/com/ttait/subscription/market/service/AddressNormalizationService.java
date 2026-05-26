package com.ttait.subscription.market.service;

import com.ttait.subscription.announcement.domain.AnnouncementUnit;
import com.ttait.subscription.market.domain.LawdCodeMapping;
import com.ttait.subscription.market.repository.LawdCodeMappingRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AddressNormalizationService {

    private static final String NO_ADDRESS_MESSAGE = "주소 없음";
    private static final String NO_LAWD_CODE_MESSAGE = "법정동 코드 매핑 없음";

    private final LawdCodeMappingRepository lawdCodeMappingRepository;

    public AddressNormalizationService(LawdCodeMappingRepository lawdCodeMappingRepository) {
        this.lawdCodeMappingRepository = lawdCodeMappingRepository;
    }

    public void normalizeUnitAddress(AnnouncementUnit unit) {
        LocalDateTime normalizedAt = LocalDateTime.now();
        String normalizedAddress = normalizeAddress(unit.getFullAddress());
        if (!StringUtils.hasText(normalizedAddress)) {
            unit.markAddressNoAddress(NO_ADDRESS_MESSAGE, normalizedAt);
            return;
        }

        AddressTokens tokens = parseTokens(unit, normalizedAddress);
        lawdCodeMappingRepository.findFirstByRegionLevel2AndLegalDongNameAndActiveTrue(
                        tokens.regionLevel2(),
                        tokens.legalDongName())
                .ifPresentOrElse(
                        mapping -> applyMapping(unit, normalizedAddress, mapping, normalizedAt),
                        () -> unit.markAddressNoLawdCode(normalizedAddress, NO_LAWD_CODE_MESSAGE, normalizedAt)
                );
    }

    public String normalizeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            return null;
        }
        return address.trim().replaceAll("\\s+", " ");
    }

    private AddressTokens parseTokens(AnnouncementUnit unit, String normalizedAddress) {
        String[] tokens = Arrays.stream(normalizedAddress.split(" "))
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        String inferredRegionLevel2 = inferRegionLevel2(tokens);
        String regionLevel2 = normalizeToken(unit.getRegionLevel2());
        if (!StringUtils.hasText(regionLevel2) || !isRegionLevel2Token(regionLevel2)) {
            regionLevel2 = inferredRegionLevel2;
        }
        String legalDongName = inferLegalDongName(tokens, regionLevel2);
        return new AddressTokens(regionLevel2, legalDongName);
    }

    private String inferRegionLevel2(String[] tokens) {
        if (tokens.length == 0) {
            return null;
        }
        if (tokens.length >= 2 && isProvinceLevelToken(tokens[0]) && isRegionLevel2Token(tokens[1])) {
            return tokens[1];
        }
        if (isRegionLevel2Token(tokens[0])) {
            return tokens[0];
        }
        if (tokens.length >= 2 && isRegionLevel2Token(tokens[1])) {
            return tokens[1];
        }
        return null;
    }

    private String inferLegalDongName(String[] tokens, String regionLevel2) {
        if (StringUtils.hasText(regionLevel2)) {
            for (int index = 0; index < tokens.length; index++) {
                if (regionLevel2.equals(tokens[index]) && index + 1 < tokens.length) {
                    return tokens[index + 1];
                }
            }
        }
        return tokens.length > 1 ? tokens[1] : null;
    }

    private String normalizeToken(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    private void applyMapping(AnnouncementUnit unit,
                              String normalizedAddress,
                              LawdCodeMapping mapping,
                              LocalDateTime normalizedAt) {
        unit.markAddressResolved(
                normalizedAddress,
                mapping.getLegalDongCode(),
                mapping.getLawdCd(),
                normalizedAt
        );
    }

    private record AddressTokens(String regionLevel2, String legalDongName) {
    }
}
