package com.ttait.subscription.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ttait.subscription.admin.dto.RtmsCollectionRequest;
import com.ttait.subscription.admin.dto.RtmsCollectionResponse;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsApiResult;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.market.service.MarketRtmsCollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMarketCollectionServiceTest {

    @Mock
    private MarketRtmsCollectionService marketRtmsCollectionService;

    private AdminMarketCollectionService service;

    @BeforeEach
    void setUp() {
        service = new AdminMarketCollectionService(marketRtmsCollectionService);
    }

    @Test
    void collectRtmsAppliesRequestDefaultsAndMapsResponse() {
        given(marketRtmsCollectionService.collect(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100))
                .willReturn(new MarketRtmsCollectionService.CollectionResult(
                        RtmsSourceType.APT_RENT, "41570", "202405", RtmsApiResult.Status.SUCCESS, 2, 1, 1, 0, null));

        RtmsCollectionResponse response = service.collectRtms(new RtmsCollectionRequest(
                RtmsSourceType.APT_RENT, "41570", "202405", null, null));

        assertThat(response.sourceType()).isEqualTo("APT_RENT");
        assertThat(response.lawdCd()).isEqualTo("41570");
        assertThat(response.dealYm()).isEqualTo("202405");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.fetchedCount()).isEqualTo(2);
        assertThat(response.savedCount()).isEqualTo(1);
        assertThat(response.duplicateCount()).isEqualTo(1);
        assertThat(response.failedCount()).isZero();
        then(marketRtmsCollectionService).should().collect(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100);
    }

    @Test
    void collectRtmsRejectsMissingSourceType() {
        RtmsCollectionRequest request = new RtmsCollectionRequest(null, "41570", "202405", null, null);

        assertThatThrownBy(() -> service.collectRtms(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("sourceType is required");
    }
}
