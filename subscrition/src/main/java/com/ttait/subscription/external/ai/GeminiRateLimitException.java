package com.ttait.subscription.external.ai;

public class GeminiRateLimitException extends RuntimeException {
    public GeminiRateLimitException(int maxRetries) {
        super("Gemini API 429 rate limit exhausted after " + maxRetries + " retries");
    }
}
