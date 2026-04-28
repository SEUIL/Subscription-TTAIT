package com.ttait.subscription.external.support;

import com.ttait.subscription.announcement.domain.AnnouncementCategory;
import com.ttait.subscription.announcement.domain.MatchSource;
import com.ttait.subscription.announcement.domain.Announcement;
import com.ttait.subscription.user.domain.enums.CategoryCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CategoryDetector {

    private static final Map<CategoryCode, List<String>> KEYWORDS = Map.of(
            CategoryCode.YOUTH, List.of("청년", "만 19", "만19", "대학생", "사회초년생", "청년매입", "청년전용"),
            CategoryCode.NEWLYWED, List.of("신혼부부", "예비신혼부부", "혼인 7년", "혼인7년", "신혼", "신생아"),
            CategoryCode.HOMELESS, List.of("무주택", "무주택세대구성원"),
            CategoryCode.ELDERLY, List.of("고령자", "만 65세", "만65세", "고령", "노인"),
            CategoryCode.LOW_INCOME, List.of("기초생활수급자", "차상위", "저소득", "기초수급"),
            CategoryCode.MULTI_CHILD, List.of("다자녀", "미성년 자녀 3인", "미성년자녀3인", "다자녀가구")
    );

    public List<AnnouncementCategory> detect(Announcement announcement, String combinedText) {
        if (combinedText == null || combinedText.isBlank()) {
            return List.of();
        }

        List<AnnouncementCategory> result = new ArrayList<>();

        for (Map.Entry<CategoryCode, List<String>> entry : KEYWORDS.entrySet()) {
            CategoryCode code = entry.getKey();
            List<String> keywords = entry.getValue();

            List<String> matched = keywords.stream()
                    .filter(combinedText::contains)
                    .toList();

            if (!matched.isEmpty()) {
                result.add(AnnouncementCategory.builder()
                        .announcement(announcement)
                        .categoryCode(code)
                        .matchSource(MatchSource.RULE)
                        .matchReason(String.join(", ", matched))
                        .score(matched.size())
                        .build());
            }
        }

        return result;
    }
}
