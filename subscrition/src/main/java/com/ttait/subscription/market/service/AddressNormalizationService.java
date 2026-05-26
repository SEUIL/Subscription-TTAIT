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
        String regionLevel2 = StringUtils.hasText(unit.getRegionLevel2()) ? unit.getRegionLevel2().trim() : null;
        if (!StringUtils.hasText(regionLevel2) && tokens.length > 0) {
            regionLevel2 = tokens[0];
        }
        String legalDongName = tokens.length > 1 ? tokens[1] : null;
        return new AddressTokens(regionLevel2, legalDongName);
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
