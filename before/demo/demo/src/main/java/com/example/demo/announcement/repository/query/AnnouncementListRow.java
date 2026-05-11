package com.example.demo.announcement.repository.query;

import com.example.demo.announcement.domain.AnnouncementStatus;
import java.time.LocalDate;

public record AnnouncementListRow(
        Long announcementId,
        String noticeName,
        String providerName,
        String supplyType,
        String houseType,
        String regionLevel1,
        String regionLevel2,
        String complexName,
        Long depositAmount,
        Long monthlyRentAmount,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        AnnouncementStatus noticeStatus,
        boolean saved,
        long recommendScore
) {
}
