import { fetchListingDetail, toggleSavedListing } from "./api.js";
import { renderDetailHeader } from "./components.js";
import { formatCurrency, formatRent, queryParam } from "./utils.js";

const header = document.getElementById("site-header");
const root = document.getElementById("page-root");

async function init() {
  const item = await fetchListingDetail(queryParam("id", "1"));
  document.title = `집구해줘 - ${item.title}`;
  renderDetailHeader(header, item.title);

  root.innerHTML = `
    <div class="hero">
      ${item.thumbnailEmoji ?? "🏠"}
      <div class="hero__overlay"></div>
      <div class="hero__badges">
        ${(item.badges ?? []).map((badge, index) => `<span class="hero__badge ${index === 0 && item.status !== "closed" ? "is-open" : ""}">${badge}</span>`).join("")}
      </div>
    </div>
    <section class="container page-shell detail-page__body">
      <div class="layout-grid detail-layout">
        <article>
          <p class="meta-line">${item.providerDetail}</p>
          <h1 class="detail-title">${item.title}</h1>
          <div class="tag-list">
            <span class="tag-chip is-pink">${item.status === "closing" ? "마감임박" : item.status === "closed" ? "모집 종료" : "모집중"}</span>
            <span class="tag-chip">${item.supplyType}</span>
            <span class="tag-chip">${item.housingType}</span>
            ${(item.tags ?? []).map((tag) => `<span class="tag-chip">${tag.label}</span>`).join("")}
          </div>
          <section class="detail-section">
            <h2 class="detail-section__title">공고 정보</h2>
            <div class="info-grid">
              ${(item.info ?? []).map(([label, value]) => `<div class="info-item"><span class="info-label">${label}</span><span class="info-value">${value}</span></div>`).join("")}
            </div>
          </section>
          <section class="detail-section">
            <h2 class="detail-section__title">접수 일정</h2>
            <div class="timeline">
              ${(item.schedule ?? [])
                .map(
                  (step) => `
                    <div class="timeline-item ${step.current ? "is-current" : ""}">
                      <p class="timeline-date ${step.current ? "is-current" : ""}">
                        ${step.date}${step.badge ? `<span class="deadline-pill">${step.badge}</span>` : ""}
                      </p>
                      <p class="timeline-title">${step.title}</p>
                      ${step.desc ? `<p class="timeline-desc">${step.desc}</p>` : ""}
                    </div>
                  `
                )
                .join("")}
            </div>
          </section>
          <section class="detail-section">
            <h2 class="detail-section__title">시세 비교</h2>
            <div class="market-card">
              <p class="market-title">${item.market.title}</p>
              <div class="market-row"><span class="market-label">보증금</span><div class="market-compare"><span class="market-value-market">${item.market.depositMarket}</span><span>vs</span><span class="market-value-public">${item.market.depositPublic}</span></div></div>
              <div class="market-bar"><div class="market-bar-track"><div class="market-bar-fill" style="width:${item.market.depositRate}%"></div></div><div class="market-helper"><span>공공 ${item.market.depositPublic}</span><span>시세 ${item.market.depositMarket}</span></div></div>
              <div class="market-row"><span class="market-label">월세</span><div class="market-compare"><span class="market-value-market">${item.market.rentMarket}</span><span>vs</span><span class="market-value-public">${item.market.rentPublic}</span></div></div>
              <div class="market-bar"><div class="market-bar-track"><div class="market-bar-fill" style="width:${item.market.rentRate}%"></div></div><div class="market-helper"><span>공공 ${item.market.rentPublic}</span><span>시세 ${item.market.rentMarket}</span></div></div>
              <p class="market-summary">${item.market.summary}</p>
            </div>
          </section>
          <section class="detail-section">
            <h2 class="detail-section__title">첨부 서류</h2>
            <div class="attachment-list">
              ${(item.attachments ?? [])
                .map(
                  (attachment) => `
                    <a class="attachment-item" href="${attachment.href}" target="_blank" rel="noreferrer">
                      <span class="attachment-icon"><svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM13 3.5 18.5 9H13z"/></svg></span>
                      <span class="attachment-name">${attachment.label}</span>
                      <span class="attachment-type">${attachment.type}</span>
                      <span>열기</span>
                    </a>
                  `
                )
                .join("")}
            </div>
          </section>
          <section class="detail-section">
            <h2 class="detail-section__title">안내사항</h2>
            <div class="info-panel">
              <p>${item.notes}</p>
              <a class="pill-btn" href="${item.applyUrl}" target="_blank" rel="noreferrer" style="margin-top:12px;padding:0;">원문 링크 바로가기</a>
            </div>
          </section>
        </article>
        <aside>
          <div class="sticky-card">
            <p class="sticky-label">${item.salePrice ? "분양가" : "보증금"}</p>
            <p class="price-big">${item.salePrice ? formatCurrency(item.salePrice) : formatCurrency(item.deposit)}</p>
            ${item.salePrice ? "" : `<p class="sticky-label" style="margin-top:12px;">월 임대료</p><p class="price-big">${formatRent(item.monthlyRent)}</p>`}
            <div class="deadline-info"><span>마감</span><span>${item.deadlineLabel}</span></div>
            <div class="stack-actions">
              <a class="submit-btn" href="${item.applyUrl}" target="_blank" rel="noreferrer">신청하러 가기</a>
              <button class="save-btn" type="button" data-save-detail>${item.liked ? "저장 취소" : "공고 저장하기"}</button>
            </div>
            <div class="quick-info">
              <div class="quick-info-row"><span class="quick-label">공급유형</span><span>${item.supplyType}</span></div>
              <div class="quick-info-row"><span class="quick-label">주택유형</span><span>${item.housingType}</span></div>
              <div class="quick-info-row"><span class="quick-label">지역</span><span>${item.region}</span></div>
            </div>
          </div>
        </aside>
      </div>
    </section>
  `;

  const saveButton = root.querySelector("[data-save-detail]");
  saveButton?.addEventListener("click", async () => {
    saveButton.disabled = true;
    item.liked = await toggleSavedListing(item, !item.liked);
    saveButton.textContent = item.liked ? "저장 취소" : "공고 저장하기";
    saveButton.disabled = false;
  });
}

init();
