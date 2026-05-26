package com.ttait.subscription.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttait.subscription.common.exception.ApiException;
import com.ttait.subscription.external.rtms.RtmsApiResult;
import com.ttait.subscription.external.rtms.RtmsClient;
import com.ttait.subscription.external.rtms.RtmsSourceType;
import com.ttait.subscription.external.rtms.RtmsTransactionItem;
import com.ttait.subscription.external.support.CanonicalJsonHasher;
import com.ttait.subscription.market.domain.MarketSourceType;
import com.ttait.subscription.market.domain.MarketTransactionRaw;
import com.ttait.subscription.market.repository.MarketTransactionRawRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MarketRtmsCollectionServiceTest {

    @Mock
    private RtmsClient rtmsClient;
    @Mock
    private MarketTransactionRawRepository rawRepository;

    private CanonicalJsonHasher hasher;
    private MarketRtmsCollectionService service;

    @BeforeEach
    void setUp() {
        hasher = new CanonicalJsonHasher(new ObjectMapper());
        service = new MarketRtmsCollectionService(rtmsClient, rawRepository, hasher);
    }

    @Test
    void collectsAndSavesOnlyNonDuplicateTransactions() {
        RtmsTransactionItem first = item("마산동 테스트아파트 59.84 70000 30");
        RtmsTransactionItem duplicate = item("마산동 테스트아파트 84.99 80000 40");
        String firstHash = hash("APT_RENT", "41570", "202405", first.rawPayload());
        String duplicateHash = hash("APT_RENT", "41570", "202405", duplicate.rawPayload());
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100))
                .willReturn(RtmsApiResult.success(List.of(first, duplicate)));
        given(rawRepository.existsByRawPayloadHash(firstHash)).willReturn(false);
        given(rawRepository.existsByRawPayloadHash(duplicateHash)).willReturn(true);
        given(rawRepository.save(any(MarketTransactionRaw.class))).willAnswer(invocation -> invocation.getArgument(0));

        MarketRtmsCollectionService.CollectionResult result = service.collect(
                RtmsSourceType.APT_RENT, "41570", "202405", 1, 100);

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.SUCCESS);
        assertThat(result.fetchedCount()).isEqualTo(2);
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.duplicateCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();

        ArgumentCaptor<MarketTransactionRaw> captor = ArgumentCaptor.forClass(MarketTransactionRaw.class);
        then(rawRepository).should().save(captor.capture());
        MarketTransactionRaw saved = captor.getValue();
        assertThat(saved.getSourceType()).isEqualTo(MarketSourceType.APT_RENT);
        assertThat(saved.getLawdCd()).isEqualTo("41570");
        assertThat(saved.getDealYm()).isEqualTo("202405");
        assertThat(saved.getLegalDongName()).isEqualTo("마산동");
        assertThat(saved.getBuildingName()).isEqualTo("테스트아파트");
        assertThat(saved.getExclusiveArea()).isEqualByComparingTo(new BigDecimal("59.84"));
        assertThat(saved.getDepositAmount()).isEqualTo(70000L);
        assertThat(saved.getMonthlyRentAmount()).isEqualTo(30L);
        assertThat(saved.getRawPayloadHash()).isEqualTo(firstHash);
        assertThat(saved.getRawPayload()).isEqualTo(first.rawPayload());
        assertThat(saved.getCollectedAt()).isNotNull();
    }

    @Test
    void noResultDoesNotSaveTransactions() {
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100))
                .willReturn(RtmsApiResult.noResult("RTMS response has no items"));

        MarketRtmsCollectionService.CollectionResult result = service.collect(
                RtmsSourceType.APT_RENT, "41570", "202405", 1, 100);

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.NO_RESULT);
        assertThat(result.fetchedCount()).isZero();
        assertThat(result.savedCount()).isZero();
        assertThat(result.duplicateCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(result.message()).isEqualTo("RTMS response has no items");
        then(rawRepository).should(never()).save(any());
    }

    @Test
    void failedResultDoesNotSaveAndCountsRequestFailure() {
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100))
                .willReturn(RtmsApiResult.failed("RTMS response parse failed"));

        MarketRtmsCollectionService.CollectionResult result = service.collect(
                RtmsSourceType.APT_RENT, "41570", "202405", 1, 100);

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.FAILED);
        assertThat(result.fetchedCount()).isZero();
        assertThat(result.savedCount()).isZero();
        assertThat(result.duplicateCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.message()).isEqualTo("RTMS response parse failed");
        then(rawRepository).should(never()).save(any());
    }

    @Test
    void propagatesRtmsClientApiException() {
        ApiException exception = new ApiException(HttpStatus.BAD_REQUEST, "RTMS service key is missing");
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100)).willThrow(exception);

        assertThatThrownBy(() -> service.collect(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100))
                .isSameAs(exception);
    }

    @Test
    void rawPayloadHashIncludesRequestScope() {
        RtmsTransactionItem item = item("same raw payload");
        String mayHash = hash("APT_RENT", "41570", "202405", item.rawPayload());
        String juneHash = hash("APT_RENT", "41570", "202406", item.rawPayload());
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100))
                .willReturn(RtmsApiResult.success(List.of(item)));
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202406", 1, 100))
                .willReturn(RtmsApiResult.success(List.of(item)));

        service.collect(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100);
        service.collect(RtmsSourceType.APT_RENT, "41570", "202406", 1, 100);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        then(rawRepository).should(times(2)).existsByRawPayloadHash(hashCaptor.capture());
        assertThat(hashCaptor.getAllValues()).containsExactly(mayHash, juneHash);
        assertThat(mayHash).isNotEqualTo(juneHash);
    }

    @Test
    void collectAllFetchesPagesUntilTotalCountIsCovered() {
        RtmsTransactionItem first = item("page1-row1");
        RtmsTransactionItem second = item("page1-row2");
        RtmsTransactionItem third = item("page2-row1");
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 2))
                .willReturn(RtmsApiResult.success(List.of(first, second), 3, 1, 2));
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 2, 2))
                .willReturn(RtmsApiResult.success(List.of(third), 3, 2, 2));
        given(rawRepository.existsByRawPayloadHash(any())).willReturn(false);
        given(rawRepository.save(any(MarketTransactionRaw.class))).willAnswer(invocation -> invocation.getArgument(0));

        MarketRtmsCollectionService.CollectionAllResult result = service.collectAll(
                RtmsSourceType.APT_RENT, "41570", "202405", 2, 10);

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.SUCCESS);
        assertThat(result.fetchedCount()).isEqualTo(3);
        assertThat(result.savedCount()).isEqualTo(3);
        assertThat(result.duplicateCount()).isZero();
        assertThat(result.collectedPageCount()).isEqualTo(2);
        assertThat(result.totalCount()).isEqualTo(3);
        then(rtmsClient).should().fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 2);
        then(rtmsClient).should().fetch(RtmsSourceType.APT_RENT, "41570", "202405", 2, 2);
        then(rtmsClient).should(never()).fetch(RtmsSourceType.APT_RENT, "41570", "202405", 3, 2);
    }

    @Test
    void collectAllReturnsNoResultWhenFirstPageHasNoData() {
        given(rtmsClient.fetch(RtmsSourceType.APT_RENT, "41570", "202405", 1, 100))
                .willReturn(RtmsApiResult.noResult("NO_DATA", 0, 1, 100));

        MarketRtmsCollectionService.CollectionAllResult result = service.collectAll(
                RtmsSourceType.APT_RENT, "41570", "202405", 100, null);

        assertThat(result.status()).isEqualTo(RtmsApiResult.Status.NO_RESULT);
        assertThat(result.fetchedCount()).isZero();
        assertThat(result.collectedPageCount()).isZero();
        assertThat(result.totalCount()).isZero();
        then(rawRepository).should(never()).save(any());
    }

    private RtmsTransactionItem item(String rawPayload) {
        return new RtmsTransactionItem(
                RtmsSourceType.APT_RENT,
                "41570",
                "202405",
                "마산동",
                "테스트아파트",
                "123-1",
                "테스트로",
                2020,
                new BigDecimal("59.84"),
                10,
                70000L,
                30L,
                null,
                rawPayload
        );
    }

    private String hash(String sourceType, String lawdCd, String dealYm, String rawPayload) {
        return hasher.sha256Text(sourceType + "|" + lawdCd + "|" + dealYm + "|" + rawPayload);
    }
}
