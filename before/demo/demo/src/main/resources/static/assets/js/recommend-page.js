import { fetchRecommendedListings } from "./api.js";
import { attachCardInteractions, renderFooter, renderListingCard, renderMainHeader } from "./components.js";
import { formatCurrency } from "./utils.js";

const header = document.getElementById("site-header");
const root = document.getElementById("page-root");
const footer = document.getElementById("site-footer");

renderMainHeader(header, "recommend");
renderFooter(footer);

async function init() {
  const result = await fetchRecommendedListings();

  root.innerHTML = `
    <section class="container page-shell recommend-page__body">
      <div class="recommend-hero">
        <h1>${result.profile.name}님에게 맞는<br>추천 공고를 골라봤습니다</h1>
        <p>현재 입력된 선호 조건인 ${result.profile.preferredRegion} ${result.profile.preferredDistrict}, ${result.profile.preferredSupplyType}, 최대 보증금 ${formatCurrency(result.profile.maxDeposit)} 기준으로 우선순위를 계산했습니다.</p>
        <div class="recommend-summary">
          <span class="summary-pill">선호 지역 · ${result.profile.preferredRegion} ${result.profile.preferredDistrict}</span>
          <span class="summary-pill">관심 유형 · ${(result.profile.categories ?? []).join(", ") || "미설정"}</span>
          <span class="summary-pill">예산 기준 · 보증금 ${formatCurrency(result.profile.maxDeposit)}</span>
        </div>
      </div>
      <section class="recommend-page__section">
        <div class="chip-row" style="margin-bottom:20px;">
          <span class="chip is-active">추천 정확도 높은 순</span>
          <span class="result-count">총 <strong>${result.items.length}</strong>개의 추천 공고</span>
        </div>
        <div class="listing-grid">
          ${result.items
            .map(
              (item) => `
                <div>
                  ${renderListingCard(item)}
                  <div style="padding:8px 12px 0;">
                    <p class="meta-line" style="color:var(--rausch);font-weight:700;">추천 점수 ${item.score}점</p>
                    <p class="meta-line" style="margin-top:4px;line-height:1.6;">${item.recommendedReason}</p>
                  </div>
                </div>
              `
            )
            .join("")}
        </div>
      </section>
    </section>
  `;

  attachCardInteractions(root);
}

init();
