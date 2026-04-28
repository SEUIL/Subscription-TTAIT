package com.ttait.subscription.external.pdf;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);
    private static final int MIN_TEXT_LENGTH = 200;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String extract(String pdfUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pdfUrl))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("PDF download failed: status={}, url={}", response.statusCode(), pdfUrl);
                return null;
            }

            try (InputStream inputStream = response.body();
                 PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);

                if (text == null || text.length() < MIN_TEXT_LENGTH) {
                    log.warn("PDF appears to be scanned (text length={}): {}", text == null ? 0 : text.length(), pdfUrl);
                    return null;
                }

                return text.trim();
            }

        } catch (Exception e) {
            log.error("PDF extraction failed: url={}", pdfUrl, e);
            return null;
        }
    }
}
