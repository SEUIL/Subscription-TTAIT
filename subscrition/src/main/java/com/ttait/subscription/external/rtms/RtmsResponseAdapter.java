package com.ttait.subscription.external.rtms;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class RtmsResponseAdapter {

    public RtmsApiResult adapt(String xml, RtmsSourceType sourceType, String lawdCd, String dealYm) {
        if (!StringUtils.hasText(xml)) {
            return RtmsApiResult.noResult("RTMS response is empty");
        }

        try {
            Document document = documentBuilderFactory()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            String resultCode = text(document.getDocumentElement(), "resultCode");
            String resultMessage = text(document.getDocumentElement(), "resultMsg");
            if (StringUtils.hasText(resultCode) && !isSuccessResultCode(resultCode)) {
                return RtmsApiResult.failed(resultMessage == null ? "RTMS API error" : resultMessage);
            }

            NodeList itemNodes = document.getElementsByTagName("item");
            if (itemNodes.getLength() == 0) {
                return RtmsApiResult.noResult("RTMS response has no items");
            }

            List<RtmsTransactionItem> items = new ArrayList<>();
            for (int index = 0; index < itemNodes.getLength(); index++) {
                Element item = (Element) itemNodes.item(index);
                items.add(toItem(item, sourceType, lawdCd, dealYm));
            }
            return RtmsApiResult.success(items);
        } catch (Exception exception) {
            return RtmsApiResult.failed("RTMS response parse failed");
        }
    }

    private boolean isSuccessResultCode(String resultCode) {
        return "00".equals(resultCode) || "000".equals(resultCode);
    }

    private DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private RtmsTransactionItem toItem(Element item, RtmsSourceType sourceType, String lawdCd, String dealYm) {
        return new RtmsTransactionItem(
                sourceType,
                lawdCd,
                dealYm,
                firstText(item, "법정동", "umdNm"),
                firstText(item, "아파트", "연립다세대", "오피스텔", "aptNm", "mhouseNm", "offiNm"),
                firstText(item, "지번", "jibun"),
                firstText(item, "도로명", "roadNm"),
                integer(firstText(item, "건축년도", "buildYear")),
                decimal(firstText(item, "전용면적", "excluUseAr")),
                integer(firstText(item, "층", "floor")),
                sourceType.rent() ? amount(firstText(item, "보증금액", "deposit")) : null,
                sourceType.rent() ? amount(firstText(item, "월세금액", "monthlyRent")) : null,
                sourceType.rent() ? null : amount(firstText(item, "거래금액", "dealAmount")),
                itemToText(item)
        );
    }

    private String firstText(Element element, String... names) {
        for (String name : names) {
            String value = text(element, name);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Long amount(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace(",", "").replace(" ", "").trim();
        return normalized.isBlank() ? null : Long.valueOf(normalized);
    }

    private Integer integer(String value) {
        return StringUtils.hasText(value) ? Integer.valueOf(value.trim()) : null;
    }

    private BigDecimal decimal(String value) {
        return StringUtils.hasText(value) ? new BigDecimal(value.trim()) : null;
    }

    private String itemToText(Element item) {
        return item.getTextContent() == null ? null : item.getTextContent().trim();
    }
}
