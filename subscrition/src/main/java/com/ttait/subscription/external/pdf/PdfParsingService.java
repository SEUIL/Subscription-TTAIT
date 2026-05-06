package com.ttait.subscription.external.pdf;

import com.ttait.subscription.external.ai.GeminiClient;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfParsingService {

    private static final Logger log = LoggerFactory.getLogger(PdfParsingService.class);

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
            PdfParseResult result = geminiClient.parsePdf(pdfBytes);
            if (result != null) return result;
            log.warn("Gemini PDF parse failed, falling back to text: {}", pdfUrl);
        }

        // 2차: 텍스트 추출 → Gemini 텍스트 파싱
        String text = textExtractor.extract(pdfUrl);
        if (text == null) {
            log.warn("No extractable text from PDF: {}", pdfUrl);
            return null;
        }
        log.info("Gemini text parse: url={}, textLength={}", pdfUrl, text.length());
        return geminiClient.parseText(text);
    }
}
