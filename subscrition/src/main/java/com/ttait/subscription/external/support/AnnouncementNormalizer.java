package com.ttait.subscription.external.support;

import com.ttait.subscription.announcement.domain.AnnouncementStatus;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementNormalizer {

    private static final Pattern PAN_ID_PATTERN = Pattern.compile("panId=([^&]+)");

    public String normalizeSupplyType(String raw) {
        if (raw == null || raw.isBlank()) return "기타";
        if (raw.contains("신혼희망타운")) return "신혼희망타운";
        if (raw.contains("국민임대")) return "국민임대";
        if (raw.contains("행복주택")) return "행복주택";
        if (raw.contains("영구임대")) return "영구임대";
        if (raw.contains("매입임대")) return "매입임대";
        if (raw.contains("전세임대")) return "전세임대";
        if (raw.contains("공공분양") || raw.contains("분양")) return "공공분양";
        return "기타";
    }

    public String normalizeHouseType(String raw) {
        if (raw == null || raw.isBlank()) return "기타";
        if (raw.contains("아파트")) return "아파트";
        if (raw.contains("다가구")) return "다가구";
        if (raw.contains("다세대") || raw.contains("연립") || raw.contains("빌라")) return "다세대/연립";
        if (raw.contains("오피스텔")) return "오피스텔";
        return "기타";
    }

    public String normalizeProviderName(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) return fallback;
        String normalized = value.replace(" ", "");
        if (normalized.contains("LH") || normalized.contains("한국토지주택공사")) return "LH";
        if (normalized.contains("SH") || normalized.contains("서울주택도시공사")) return "SH";
        if (normalized.contains("GH") || normalized.contains("경기주택도시공사")) return "GH";
        return value;
    }

    public AnnouncementStatus calculateStatus(LocalDate startDate, LocalDate endDate, String rawStatus) {
        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate)) return AnnouncementStatus.SCHEDULED;
        if (startDate != null && endDate != null && !today.isBefore(startDate) && !today.isAfter(endDate)) return AnnouncementStatus.OPEN;
        if (endDate != null && today.isAfter(endDate)) return AnnouncementStatus.CLOSED;
        if (rawStatus != null && (rawStatus.contains("공고중") || rawStatus.contains("모집중"))) return AnnouncementStatus.OPEN;
        return AnnouncementStatus.SCHEDULED;
    }

    public String extractPanId(String url) {
        if (url == null || url.isBlank()) return null;
        Matcher matcher = PAN_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    public String buildMatchKey(String primaryId, String fallbackSource, String noticeName, LocalDate startDate, LocalDate endDate) {
        if (primaryId != null && !primaryId.isBlank()) return "LH:" + primaryId;
        String normalizedName = noticeName == null ? "" : noticeName.replaceAll("\\s+", "").replaceAll("[^0-9A-Za-z가-힣]", "");
        return fallbackSource + ":" + normalizedName + ":" + startDate + ":" + endDate;
    }
}
