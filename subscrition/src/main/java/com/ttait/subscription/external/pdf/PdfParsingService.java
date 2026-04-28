package com.ttait.subscription.external.pdf;

import com.ttait.subscription.external.ai.OpenAiClient;
import com.ttait.subscription.external.ai.dto.PdfParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfParsingService {

    private static final Logger log = LoggerFactory.getLogger(PdfParsingService.class);

    private final PdfTextExtractor textExtractor;
    private final OpenAiClient openAiClient;

    public PdfParsingService(PdfTextExtractor textExtractor, OpenAiClient openAiClient) {
        this.textExtractor = textExtractor;
        this.openAiClient = openAiClient;
    }

    /**
     * PDF URL에서 텍스트 추출 후 OpenAI로 파싱. 실패 시 null 반환.
     */
    public PdfParseResult parse(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isBlank()) {
            return null;
        }

        String text = textExtractor.extract(pdfUrl);
        if (text == null) {
            log.warn("No extractable text from PDF: {}", pdfUrl);
            return null;
        }

        log.info("Parsing PDF with OpenAI: url={}, textLength={}", pdfUrl, text.length());
        return openAiClient.parse(text);
    }
}
