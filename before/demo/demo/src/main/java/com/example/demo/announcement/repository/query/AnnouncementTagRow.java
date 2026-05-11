package com.example.demo.announcement.repository.query;

import com.example.demo.user.domain.CategoryCode;

public record AnnouncementTagRow(
        Long announcementId,
        CategoryCode categoryCode
) {
}
