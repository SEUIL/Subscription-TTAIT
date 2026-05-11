package com.example.demo.announcement.service;

import com.example.demo.announcement.domain.Announcement;
import com.example.demo.announcement.domain.AnnouncementCategoryTag;
import com.example.demo.announcement.domain.TagSource;
import com.example.demo.announcement.repository.AnnouncementCategoryTagRepository;
import com.example.demo.announcement.repository.AnnouncementRepository;
import com.example.demo.user.domain.CategoryCode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnnouncementTaggingService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementCategoryTagRepository announcementCategoryTagRepository;

    public AnnouncementTaggingService(AnnouncementRepository announcementRepository,
                                      AnnouncementCategoryTagRepository announcementCategoryTagRepository) {
        this.announcementRepository = announcementRepository;
        this.announcementCategoryTagRepository = announcementCategoryTagRepository;
    }

    public int retagAll() {
        int processed = 0;
        int page = 0;
        Page<Announcement> announcementPage;
        do {
            announcementPage = announcementRepository.findByDeletedFalse(PageRequest.of(page, 100));
            for (Announcement announcement : announcementPage.getContent()) {
                retag(announcement);
                processed++;
            }
            page++;
        } while (announcementPage.hasNext());
        return processed;
    }

    public void retag(Announcement announcement) {
        announcementCategoryTagRepository.findByAnnouncementId(announcement.getId())
                .forEach(announcementCategoryTagRepository::delete);

        for (AnnouncementCategoryTag tag : buildTags(announcement)) {
            announcementCategoryTagRepository.save(tag);
        }
    }

    private List<AnnouncementCategoryTag> buildTags(Announcement announcement) {
        String source = ((announcement.getNoticeName() == null ? "" : announcement.getNoticeName()) + " "
                + (announcement.getSupplyTypeRaw() == null ? "" : announcement.getSupplyTypeRaw()) + " "
                + (announcement.getSupplyTypeNormalized() == null ? "" : announcement.getSupplyTypeNormalized()))
                .toLowerCase(Locale.ROOT);

        Map<CategoryCode, Integer> scores = new EnumMap<>(CategoryCode.class);

        applyRule(scores, source, CategoryCode.YOUTH, "청년", 70);
        applyRule(scores, source, CategoryCode.YOUTH, "대학생", 60);
        applyRule(scores, source, CategoryCode.YOUTH, "청년 매입임대", 80);
        applyRule(scores, source, CategoryCode.YOUTH, "행복주택", 20);

        applyRule(scores, source, CategoryCode.NEWLYWED, "신혼", 80);
        applyRule(scores, source, CategoryCode.NEWLYWED, "신생아", 60);
        applyRule(scores, source, CategoryCode.NEWLYWED, "신혼희망타운", 90);

        applyRule(scores, source, CategoryCode.HOMELESS, "무주택", 50);
        applyRule(scores, source, CategoryCode.ELDERLY, "고령자", 80);

        applyRule(scores, source, CategoryCode.LOW_INCOME, "기초생활수급", 80);
        applyRule(scores, source, CategoryCode.LOW_INCOME, "차상위", 80);
        applyRule(scores, source, CategoryCode.LOW_INCOME, "저소득", 80);
        applyRule(scores, source, CategoryCode.LOW_INCOME, "영구임대", 80);

        applyRule(scores, source, CategoryCode.MULTI_CHILD, "다자녀", 80);

        List<AnnouncementCategoryTag> tags = new ArrayList<>();
        scores.forEach((categoryCode, score) -> tags.add(AnnouncementCategoryTag.builder()
                .announcement(announcement)
                .categoryCode(categoryCode)
                .tagSource(TagSource.RULE)
                .tagScore(score)
                .build()));
        return tags;
    }

    private void applyRule(Map<CategoryCode, Integer> scores, String sourceText, CategoryCode categoryCode,
                           String keyword, int score) {
        if (!sourceText.contains(keyword.toLowerCase(Locale.ROOT))) {
            return;
        }
        scores.merge(categoryCode, score, Math::max);
    }
}
