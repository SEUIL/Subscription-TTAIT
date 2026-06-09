package com.ttait.subscription.announcement.service;

import com.ttait.subscription.announcement.dto.RecommendationFactorResponse;
import com.ttait.subscription.announcement.dto.RecommendationItemResponse;
import java.util.List;

record RecommendationEvaluationResult(
        boolean recommended,
        RecommendationItemResponse item,
        List<RecommendationFactorResponse> factors
) {
    static RecommendationEvaluationResult notRecommended(List<RecommendationFactorResponse> factors) {
        return new RecommendationEvaluationResult(false, null, List.copyOf(factors));
    }
}
