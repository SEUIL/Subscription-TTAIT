import { fetchMyProfile, fetchNotificationSettings, fetchNotifications, fetchSavedListings } from "./api.js";
import { renderMainHeader } from "./components.js";
import { formatCurrency, formatRent } from "./utils.js";

const header = document.getElementById("site-header");
const root = document.getElementById("page-root");
const state = { section: "profile" };

async function init() {
  renderMainHeader(header, "mypage");
  const [profile, savedListings, notifications, settings] = await Promise.all([
    fetchMyProfile(),
    fetchSavedListings(),
    fetchNotifications(),
    fetchNotificationSettings()
  ]);
  render(profile, savedListings, notifications, settings);
}

function render(profile, savedListings, notifications, settings) {
  root.innerHTML = `
    <section class="container page-shell mypage-page__body">
      <div class="page-layout">
        <aside class="sidebar section-card">
          <div class="profile-top">
            <div class="avatar">${profile.name.slice(0, 1)}</div>
            <p class="profile-name">${profile.name}</p>
            <p class="profile-id">${profile.id}</p>
          </div>
          <nav class="sidebar-nav">
            <button class="sidebar-item ${state.section === "profile" ? "is-active" : ""}" type="button" data-section="profile">프로필</button>
            <button class="sidebar-item ${state.section === "saved" ? "is-active" : ""}" type="button" data-section="saved">저장한 공고</button>
            <button class="sidebar-item ${state.section === "notifications" ? "is-active" : ""}" type="button" data-section="notifications">알림<span style="margin-left:auto;background:var(--rausch);color:white;font-size:10px;font-weight:700;padding:2px 6px;border-radius:10px;">${notifications.filter((item) => !item.read).length}</span></button>
            <div class="sidebar-divider"></div>
            <a class="logout-btn" href="./auth.html">로그아웃</a>
          </nav>
        </aside>
        <div>
          ${state.section === "profile" ? renderProfile(profile) : ""}
          ${state.section === "saved" ? renderSaved(savedListings) : ""}
          ${state.section === "notifications" ? renderNotifications(notifications, settings) : ""}
        </div>
      </div>
    </section>
  `;
  bindEvents(profile, savedListings, notifications, settings);
}

function renderProfile(profile) {
  return `
    <section class="section-card">
      <div class="section-card__head"><h2 class="section-card__title">기본 정보</h2></div>
      <div class="section-card__body">
        <div class="form-row">
          <div class="form-group"><label class="form-label">아이디</label><input class="form-input readonly-input" value="${profile.id.replace("@", "")}" readonly></div>
          <div class="form-group"><label class="form-label">이메일</label><input class="form-input" value="${profile.email}"></div>
        </div>
        <div class="form-row" style="margin-top:16px;"><div class="form-group"><label class="form-label">휴대폰 번호</label><input class="form-input" value="${profile.phone}"></div><div class="form-group"><label class="form-label">나이</label><input class="form-input" value="${profile.age}"></div></div>
        <div class="form-row" style="margin-top:16px;"><div class="form-group"><label class="form-label">혼인 상태</label><input class="form-input" value="${profile.maritalStatus}"></div><div class="form-group"><label class="form-label">가구원 수</label><input class="form-input" value="${profile.householdCount}"></div></div>
      </div>
    </section>
    <section class="section-card">
      <div class="section-card__head"><h2 class="section-card__title">주거 선호 설정</h2></div>
      <div class="section-card__body">
        <div class="form-row"><div class="form-group"><label class="form-label">선호 지역 (시/도)</label><input class="form-input" value="${profile.preferredRegion}"></div><div class="form-group"><label class="form-label">선호 지역 (구/시)</label><input class="form-input" value="${profile.preferredDistrict}"></div></div>
        <div class="form-row" style="margin-top:16px;"><div class="form-group"><label class="form-label">선호 주택 유형</label><input class="form-input" value="${profile.preferredHousingType}"></div><div class="form-group"><label class="form-label">선호 공급 유형</label><input class="form-input" value="${profile.preferredSupplyType}"></div></div>
        <div style="margin-top:20px;"><div class="budget-display"><span class="budget-label">최대 보증금</span><strong>${formatCurrency(profile.maxDeposit)}</strong></div><input class="form-input" value="${formatCurrency(profile.maxDeposit)}" readonly><div class="budget-display" style="margin-top:20px;"><span class="budget-label">최대 월세</span><strong>${formatCurrency(profile.maxRent)}</strong></div><input class="form-input" value="${formatCurrency(profile.maxRent)}" readonly></div>
      </div>
    </section>
    <section class="section-card">
      <div class="section-card__head"><h2 class="section-card__title">해당 유형 선택</h2><span class="section-card__badge">추천 정확도에 영향</span></div>
      <div class="section-card__body">
        <div class="category-grid">${["청년", "신혼부부", "무주택자", "고령자", "저소득층", "다자녀"].map((label) => `<button class="category-chip ${profile.categories.includes(label) ? "is-selected" : ""}" type="button" data-chip>${label}</button>`).join("")}</div>
        <button class="save-btn" type="button" style="width:100%;margin-top:20px;">변경 사항 저장</button>
      </div>
    </section>
  `;
}

function renderSaved(savedListings) {
  return `
    <section class="section-card">
      <div class="section-card__head"><h2 class="section-card__title">저장한 공고</h2><span class="section-card__badge">${savedListings.length}개</span></div>
      <div class="section-card__body">
        <div class="saved-list">
          ${savedListings.map((item) => `<a class="saved-item" href="./detail.html?id=${item.id}"><div class="saved-thumb">${item.thumbnailEmoji}</div><div class="saved-info"><p class="saved-title">${item.title}</p><p class="meta-line">${item.provider} · ${item.region} · 보증금 ${formatCurrency(item.deposit)} / 월세 ${formatRent(item.monthlyRent)}</p><p class="meta-line" style="margin-top:2px;color:var(--rausch);font-weight:700;">${item.deadlineLabel}</p></div><span class="saved-status ${item.status === "closing" ? "is-closing" : "is-open"}">${item.status === "closing" ? "마감임박" : "모집중"}</span></a>`).join("")}
        </div>
      </div>
    </section>
  `;
}

function renderNotifications(notifications, settings) {
  return `
    <section class="section-card">
      <div class="section-card__head"><h2 class="section-card__title">알림</h2><span class="section-card__badge">읽지 않은 알림 ${notifications.filter((item) => !item.read).length}개</span></div>
      <div class="section-card__body" style="padding-top:8px;padding-bottom:8px;"><div class="notif-list">${notifications.map((item) => `<article class="notif-item" data-notification="${item.id}"><span class="notif-dot ${item.read ? "is-read" : ""}"></span><div><p class="notif-title" ${item.read ? 'style="color:var(--gray-secondary);"' : ""}>${item.title}</p><p class="notif-msg">${item.message}</p><p class="notif-time">${item.createdAt}</p></div></article>`).join("")}</div></div>
    </section>
    <section class="section-card">
      <div class="section-card__head"><h2 class="section-card__title">알림 설정</h2></div>
      <div class="section-card__body"><div class="toggle-list">${settings.map((item) => `<div class="toggle-item"><div><p class="toggle-label">${item.title}</p><p class="toggle-desc">${item.desc}</p></div><button class="toggle-switch ${item.enabled ? "is-on" : ""}" type="button" data-toggle-setting="${item.id}" aria-label="${item.title}"></button></div>`).join("")}</div></div>
    </section>
  `;
}

function bindEvents(profile, savedListings, notifications, settings) {
  root.querySelectorAll("[data-section]").forEach((button) => button.addEventListener("click", () => {
    state.section = button.dataset.section;
    render(profile, savedListings, notifications, settings);
  }));
  root.querySelectorAll("[data-chip]").forEach((button) => button.addEventListener("click", () => button.classList.toggle("is-selected")));
  root.querySelectorAll("[data-notification]").forEach((item) => item.addEventListener("click", () => item.querySelector(".notif-dot")?.classList.add("is-read")));
  root.querySelectorAll("[data-toggle-setting]").forEach((button) => button.addEventListener("click", () => button.classList.toggle("is-on")));
}

init();
