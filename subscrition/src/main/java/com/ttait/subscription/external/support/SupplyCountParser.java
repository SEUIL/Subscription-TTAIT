package com.ttait.subscription.external.support;

import com.ttait.subscription.announcement.domain.ConfidenceLevel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SupplyCountParser {

    // "총 N호", "N호 공급", "공급호수 N", "N세대 모집" 등 명시적 공급 수량 패턴 (콤마 포함 숫자 허용)
    private static final Pattern HIGH_CONFIDENCE = Pattern.compile(
            "(?:총\\s*|공급호수\\s*|모집호수\\s*)([\\d,]+)\\s*(?:호|세대|가구)|" +
            "([\\d,]+)\\s*(?:호|세대|가구)\\s*(?:공급|모집|선정)"
    );

    // 단순 "N호" 패턴 (호실/동 번호일 수 있어 낮은 신뢰도)
    private static final Pattern LOW_CONFIDENCE = Pattern.compile("([\\d,]{1,6})\\s*호");

    // 호실 번호로 보이는 패턴 (3자리 이상 호실 번호: 101호, 102호 등)
    private static final Pattern ROOM_NUMBER = Pattern.compile("\\b(\\d{3,4})\\s*호\\b");

    public record ParseResult(Integer count, ConfidenceLevel confidence, String basis) {
    }

    public ParseResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParseResult(null, ConfidenceLevel.LOW, raw);
        }

        // 명시적 공급 수량 패턴 우선
        Matcher highMatcher = HIGH_CONFIDENCE.matcher(raw);
        if (highMatcher.find()) {
            String numStr = highMatcher.group(1) != null ? highMatcher.group(1) : highMatcher.group(2);
            Integer count = parseNumber(numStr);
            if (count != null) {
                return new ParseResult(count, ConfidenceLevel.HIGH, highMatcher.group());
            }
        }

        // 호실 번호 패턴 제거 후 단순 N호 탐색
        String cleaned = ROOM_NUMBER.matcher(raw).replaceAll("");
        Matcher lowMatcher = LOW_CONFIDENCE.matcher(cleaned);
        if (lowMatcher.find()) {
            Integer count = parseNumber(lowMatcher.group(1));
            if (count != null && count > 0 && count < 10000) {
                return new ParseResult(count, ConfidenceLevel.LOW, lowMatcher.group());
            }
        }

        return new ParseResult(null, ConfidenceLevel.LOW, raw);
    }

    private Integer parseNumber(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
