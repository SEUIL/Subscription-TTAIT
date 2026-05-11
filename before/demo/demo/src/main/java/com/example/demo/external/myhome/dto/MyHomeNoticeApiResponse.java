package com.example.demo.external.myhome.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MyHomeNoticeApiResponse(
        Response response
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Header header,
            Body body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            String totalCount,
            String numOfRows,
            String pageNo,
            List<Item> item
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String pblancId,
            Integer houseSn,
            String sttusNm,
            String pblancNm,
            String suplyInsttNm,
            String houseTyNm,
            String suplyTyNm,
            String rcritPblancDe,
            String przwnerPresnatnDe,
            String url,
            String pcUrl,
            String mobileUrl,
            String hsmpNm,
            String brtcNm,
            String signguNm,
            String fullAdres,
            String pnu,
            Integer totHshldCo,
            Integer sumSuplyCo,
            Long rentGtn,
            Long mtRntchrg,
            String beginDe,
            String endDe
    ) {
    }
}
