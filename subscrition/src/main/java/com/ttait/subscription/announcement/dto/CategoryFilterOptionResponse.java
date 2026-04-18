package com.ttait.subscription.announcement.dto;

import java.util.List;

public record CategoryFilterOptionResponse(
        List<CategoryFilterOption> items
) {
}
