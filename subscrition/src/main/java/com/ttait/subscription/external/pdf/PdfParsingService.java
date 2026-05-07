package com.ttait.subscription.external.pdf;

import com.ttait.subscription.external.ai.GeminiClient;
import com.ttait.subscription.external.ai.GeminiRateLimitException;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfParsingService {

    private static final Logger log = LoggerFactory.getLogger(PdfParsingService.class);
    private static final long GEMINI_CALL_DELAY_MS = 6_000; // 10 RPM 안전 마진 (60s / 10 = 6s)

    private final PdfTextExtractor textExtractor;
    private final GeminiClient geminiClient;

    public PdfParsingService(PdfTextExtractor textExtractor, GeminiClient geminiClient) {
        this.textExtractor = textExtractor;
        this.geminiClient = geminiClient;
    }

    public PdfParseResult parse(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isBlank()) {
            return null;
        }

        // 1차: PDF bytes → Gemini PDF 직접 전송 (표 구조 보존)
        byte[] pdfBytes = textExtractor.downloadBytes(pdfUrl);
        if (pdfBytes != null) {
            log.info("Gemini PDF parse: url={}, bytes={}", pdfUrl, pdfBytes.length);
            sleepForRateLimit();
            try {
                PdfParseResult result = geminiClient.parsePdf(pdfBytes);
                if (result != null) return result;
                log.warn("Gemini PDF parse failed, falling back to text: {}", pdfUrl);
            } catch (GeminiRateLimitException e) {
                // RPD 소진 방지: 429로 인한 실패는 텍스트 fallback 없이 종료
                log.warn("Gemini PDF 429 rate limit exhausted, skipping text fallback to preserve RPD: {}", pdfUrl);
                return null;
            }
        }

        // 2차 fallback: PDF 파싱 불가(표 구조 문제 등) 시에만 텍스트로 재시도
        String text = textExtractor.extract(pdfUrl);
        if (text == null) {
            log.warn("No extractable text from PDF: {}", pdfUrl);
            return null;
        }
        log.info("Gemini text parse: url={}, textLength={}", pdfUrl, text.length());
        sleepForRateLimit();
        try {
            return geminiClient.parseText(text);
        } catch (GeminiRateLimitException e) {
            log.warn("Gemini text 429 rate limit exhausted: {}", pdfUrl);
            return null;
        }
    }

    private void sleepForRateLimit() {
        try {
            Thread.sleep(GEMINI_CALL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
