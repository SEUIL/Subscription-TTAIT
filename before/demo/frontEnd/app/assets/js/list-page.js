import { fetchListings } from "./api.js";
import { attachCardInteractions, renderFooter, renderListingCard, renderMainHeader } from "./components.js";
import { queryParam, setQueryParams } from "./utils.js";

const header = document.getElementById("site-header");
const root = document.getElementById("page-root");
const footer = document.getElementById("site-footer");

renderMainHeader(header, "list");
renderFooter(footer);

const typeOptions = [
  { value: "all", label: "전체", icon: '<path d="M12 3L2 12h3v9h6v-6h2v6h6v-9h3L12 3z"/>' },
  { value: "국민임대", label: "국민임대", icon: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/>' },
  { value: "행복주택", label: "행복주택", icon: '<path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-4h6v4"/>' },
  { value: "영구임대", label: "영구임대", icon: '<circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/>' },
  { value: "매입임대", label: "매입임대", icon: '<path d="M20 9v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9"/><path d="M9 22V12h6v10M2 10.6L12 2l10 8.6"/>' },
  { value: "공공분양", label: "공공분양", icon: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>' }
];

const currentState = {
  q: queryParam("q", ""),
  status: queryParam("status", "all"),
  type: queryParam("type", "all"),
  sort: queryParam("sort", "recommend"),
  region: queryParam("region", "all"),
  page: queryParam("page", "1")
};

async function init() {
  const result = await fetchListings(currentState);

  root.innerHTML = `
    <div class="filter-bar">
      <div class="container filter-bar__inner">
        ${typeOptions
          .map(
            (option) => `
            <button class="filter-pill ${currentState.type === option.value ? "is-active" : ""}" type="button" data-type="${option.value}">
              <span class="filter-pill__icon"><svg viewBox="0 0 24 24">${option.icon}</svg></span>
              <span class="filter-pill__label">${option.label}</span>
            </button>
          `
          )
          .join("")}
      </div>
    </div>
    <section class="container page-shell list-page__body">
      <div class="list-page__filters">
        <div class="chip-row">
          <button class="chip ${currentState.region === "경기도" ? "is-active" : ""}" type="button" data-region="경기도">경기도 · 오산시</button>
          <button class="chip ${currentState.status === "open" ? "is-active" : ""}" type="button" data-status="open">모집중</button>
          <button class="chip ${currentState.status === "closing" ? "is-active" : ""}" type="button" data-status="closing">마감임박</button>
          <button class="chip ${currentState.sort === "recommend" ? "is-active" : ""}" type="button" data-sort="recommend">추천순</button>
          <button class="chip ${currentState.sort === "deadline" ? "is-active" : ""}" type="button" data-sort="deadline">마감순</button>
          <button class="chip ${currentState.sort === "latest" ? "is-active" : ""}" type="button" data-sort="latest">최신순</button>
          <span class="result-count">총 <strong>${result.total}</strong>개의 공고</span>
        </div>
      </div>
      <div class="listing-grid">${result.items.map(renderListingCard).join("")}</div>
      <div class="pagination">
        <button class="page-btn" type="button">‹</button>
        <button class="page-btn is-active" type="button">1</button>
        <button class="page-btn" type="button">2</button>
        <button class="page-btn" type="button">3</button>
        <button class="page-btn" type="button">›</button>
      </div>
    </section>
  `;

  attachCardInteractions(root);
  bindFilters();
}

function bindFilters() {
  root.querySelectorAll("[data-type]").forEach((button) => {
    button.addEventListener("click", () => setQueryParams({ type: button.dataset.type }));
  });

  root.querySelectorAll("[data-status]").forEach((button) => {
    button.addEventListener("click", () => {
      const next = currentState.status === button.dataset.status ? "all" : button.dataset.status;
      setQueryParams({ status: next });
    });
  });

  root.querySelectorAll("[data-sort]").forEach((button) => {
    button.addEventListener("click", () => setQueryParams({ sort: button.dataset.sort }));
  });

  root.querySelectorAll("[data-region]").forEach((button) => {
    button.addEventListener("click", () => {
      const next = currentState.region === button.dataset.region ? "all" : button.dataset.region;
      setQueryParams({ region: next });
    });
  });
}

init();
