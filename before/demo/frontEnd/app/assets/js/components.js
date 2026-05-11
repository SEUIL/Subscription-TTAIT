import { formatCurrency, formatRent, heartIcon } from "./utils.js";

const logo = `
  <a class="logo" href="./list.html">
    <span class="logo__icon">
      <svg viewBox="0 0 24 24"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5S10.62 6.5 12 6.5s2.5 1.12 2.5 2.5S13.38 11.5 12 11.5z"/></svg>
    </span>
    <span class="logo__text">집구해</span>
  </a>
`;

export function renderMainHeader(root, currentPage) {
  root.className = "site-header";
  root.innerHTML = `
    <div class="container site-header__inner">
      ${logo}
      <form class="search-bar" action="./list.html" method="get">
        <input type="text" name="q" placeholder="지역, 공급유형, 기관명으로 검색" value="${new URLSearchParams(window.location.search).get("q") ?? ""}">
        <button class="search-btn" type="submit" aria-label="검색">
          <svg viewBox="0 0 24 24"><path d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19z"/></svg>
        </button>
      </form>
      <nav class="header-nav" aria-label="주요 메뉴">
        <a class="nav-item ${currentPage === "list" ? "is-active" : ""}" href="./list.html">공고 목록</a>
        <a class="nav-item ${currentPage === "recommend" ? "is-active" : ""}" href="./recommend.html">추천 공고</a>
        <a class="nav-item ${currentPage === "mypage" ? "is-active" : ""}" href="./mypage.html">마이페이지</a>
      </nav>
    </div>
  `;
}

export function renderDetailHeader(root, title) {
  root.className = "site-header";
  root.innerHTML = `
    <div class="container site-header__inner">
      <button class="back-btn" type="button" aria-label="뒤로" data-action="back">
        <svg viewBox="0 0 24 24"><path d="M19 12H5M12 5l-7 7 7 7"/></svg>
      </button>
      <p class="detail-page__header-title">${title}</p>
      <div class="header-actions">
        <button class="icon-btn" type="button" aria-label="공유">
          <svg viewBox="0 0 24 24"><path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><line x1="12" y1="2" x2="12" y2="15"/></svg>
        </button>
        <button class="icon-btn" type="button" aria-label="저장">${heartIcon()}</button>
      </div>
    </div>
  `;

  root.querySelector('[data-action="back"]')?.addEventListener("click", () => {
    if (window.history.length > 1) return window.history.back();
    window.location.href = "./list.html";
  });
}

export function renderAuthHeader(root) {
  root.innerHTML = `
    <header class="auth-page__header">
      ${logo}
      <button class="close-btn" type="button" aria-label="닫기" data-action="close">
        <svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </header>
  `;

  root.querySelector('[data-action="close"]')?.addEventListener("click", () => {
    window.location.href = "./list.html";
  });
}

export function renderFooter(root) {
  root.className = "site-footer";
  root.innerHTML = `
    <div class="container site-footer__grid">
      <div class="site-footer__group"><h3>서비스</h3><p>공고 목록</p><p>추천 공고</p><p>알림 설정</p></div>
      <div class="site-footer__group"><h3>지원</h3><p>공급유형 안내</p><p>자격 확인</p><p>자주 묻는 질문</p></div>
      <p class="site-footer__legal">© 2026 집구해 공공임대주택 정보 플랫폼</p>
    </div>
  `;
}

const tagToneClass = (tone) => {
  if (tone === "pink") return "is-pink";
  if (tone === "blue") return "is-blue";
  if (tone === "green") return "is-green";
  if (tone === "orange") return "is-orange";
  return "";
};

const statusClass = (status) => {
  if (status === "open") return "is-open";
  if (status === "closing") return "is-closing";
  return "is-closed";
};

const statusLabel = (status) => {
  if (status === "open") return "모집중";
  if (status === "closing") return "마감임박";
  return "모집 종료";
};

export function renderListingCard(item) {
  const priceLine = item.salePrice
    ? `분양가 <strong>${formatCurrency(item.salePrice)}</strong>`
    : `보증금 <strong>${formatCurrency(item.deposit)}</strong> / 월세 <strong>${formatRent(item.monthlyRent)}</strong>`;

  return `
    <a class="listing-card" href="./detail.html?id=${item.id}">
      <div class="listing-card__media">
        <div class="listing-card__placeholder">${item.thumbnailEmoji}</div>
        <span class="status-badge ${statusClass(item.status)}">${statusLabel(item.status)}</span>
        <button class="heart-btn ${item.liked ? "is-saved" : ""}" type="button" aria-label="저장" data-heart="${item.id}">
          ${heartIcon()}
        </button>
      </div>
      <div class="listing-card__body">
        <p class="listing-card__provider">${item.provider} · ${item.region}</p>
        <h3 class="listing-card__title">${item.title}</h3>
        <p class="listing-card__price">${priceLine}</p>
        <p class="listing-card__deadline ${item.urgent ? "is-urgent" : ""}">${item.deadlineLabel}</p>
        <div class="tag-list">
          ${item.tags.map((tag) => `<span class="tag-chip ${tagToneClass(tag.tone)}">${tag.label}</span>`).join("")}
        </div>
      </div>
    </a>
  `;
}

export function attachCardInteractions(root) {
  root.querySelectorAll("[data-heart]").forEach((button) => {
    button.addEventListener("click", (event) => {
      event.preventDefault();
      button.classList.toggle("is-saved");
    });
  });
}
