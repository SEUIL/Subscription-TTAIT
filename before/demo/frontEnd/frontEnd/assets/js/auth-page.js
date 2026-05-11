import { renderAuthHeader } from "./components.js";
import { login, saveProfile, signup } from "./api.js";

const root = document.getElementById("page-root");
const state = {
  tab: "login",
  signupStep: 1,
  signupDraft: {
    loginId: "",
    password: "",
    email: "",
    phone: "",
    age: "",
    maritalStatus: "",
    householdCount: "",
    preferredRegion: "",
    preferredDistrict: "",
    preferredHousingType: "",
    preferredSupplyType: "",
    maxDeposit: "",
    maxRent: "",
    categories: ["청년"]
  }
};

function render() {
  root.innerHTML = `
    <div class="auth-shell">
      <div style="width:100%;">
        <div id="auth-header-slot"></div>
        <div class="auth-card">
          <div class="tab-bar">
            <button class="tab-btn ${state.tab === "login" ? "is-active" : ""}" type="button" data-tab="login">로그인</button>
            <button class="tab-btn ${state.tab === "signup" ? "is-active" : ""}" type="button" data-tab="signup">회원가입</button>
          </div>
          ${state.tab === "login" ? renderLoginPanel() : renderSignupPanel()}
        </div>
      </div>
    </div>
  `;

  renderAuthHeader(document.getElementById("auth-header-slot"));
  bindEvents();
}

function renderLoginPanel() {
  return `
    <section class="auth-section">
      <h1 class="form-title">다시 만나서 반갑습니다</h1>
      <p class="form-subtitle">공공임대주택 공고를 저장하고<br>맞춤 추천도 받아보세요</p>
      <form id="login-form" style="margin-top:24px;">
        <div class="form-group">
          <label class="form-label" for="login-id">아이디</label>
          <input class="form-input" id="login-id" name="id" type="text" placeholder="아이디를 입력하세요">
        </div>
        <div class="form-group" style="margin-top:16px;">
          <label class="form-label" for="login-password">비밀번호</label>
          <div class="input-wrapper">
            <input class="form-input" id="login-password" name="password" type="password" placeholder="비밀번호를 입력하세요">
            <button class="input-toggle" type="button" data-toggle="#login-password" aria-label="비밀번호 보기">
              <svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            </button>
          </div>
        </div>
        <button class="submit-btn" type="submit" style="width:100%;margin-top:24px;">로그인</button>
      </form>
    </section>
  `;
}

function renderSignupPanel() {
  if (state.signupStep === 3) {
    return `
      <section class="auth-section" style="text-align:center;">
        <div style="font-size:48px;">🎉</div>
        <h2 class="form-title" style="font-size:28px;margin-top:16px;">가입이 완료됐습니다</h2>
        <p class="form-subtitle">입력한 정보를 기준으로 맞춤 공고를 추천해드릴게요.</p>
        <a class="submit-btn" href="./list.html" style="width:100%;margin-top:32px;">공고 보러가기</a>
      </section>
    `;
  }

  return `
    <section class="auth-section">
      <div class="step-indicator">
        <div class="step ${state.signupStep === 1 ? "is-current" : "is-done"}">${state.signupStep === 1 ? "1" : "✓"}</div>
        <div class="step-line ${state.signupStep > 1 ? "is-done" : ""}"></div>
        <div class="step ${state.signupStep === 1 ? "is-pending" : "is-current"}">2</div>
      </div>
      ${state.signupStep === 1 ? renderSignupStep1() : renderSignupStep2()}
    </section>
  `;
}

function renderSignupStep1() {
  return `
    <form id="signup-step1-form">
      <h1 class="form-title">계정 만들기</h1>
      <p class="form-subtitle">기본 정보를 입력해 주세요.</p>
      <div class="form-group" style="margin-top:24px;">
        <label class="form-label" for="signup-id">아이디</label>
        <input class="form-input" id="signup-id" type="text" value="${state.signupDraft.loginId}" placeholder="영문, 숫자 4~20자">
      </div>
      <div class="form-group" style="margin-top:16px;">
        <label class="form-label" for="signup-password">비밀번호</label>
        <div class="input-wrapper">
          <input class="form-input" id="signup-password" type="password" value="${state.signupDraft.password}" placeholder="8자 이상, 영문+숫자 조합">
          <button class="input-toggle" type="button" data-toggle="#signup-password" aria-label="비밀번호 보기">
            <svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          </button>
        </div>
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-email">이메일</label>
          <input class="form-input" id="signup-email" type="email" value="${state.signupDraft.email}" placeholder="이메일 주소">
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-phone">전화번호</label>
          <input class="form-input" id="signup-phone" type="tel" value="${state.signupDraft.phone}" placeholder="010-0000-0000">
        </div>
      </div>
      <button class="submit-btn" type="submit" style="width:100%;margin-top:24px;">다음 단계</button>
    </form>
  `;
}

function renderSignupStep2() {
  return `
    <form id="signup-step2-form">
      <h1 class="form-title">맞춤 정보 입력</h1>
      <p class="form-subtitle">MVP 단계에서는 선택 정보도 함께 저장합니다.</p>
      <div class="form-group" style="margin-top:24px;">
        <label class="form-label" for="signup-age">나이</label>
        <input class="form-input" id="signup-age" type="number" value="${state.signupDraft.age}" placeholder="나이를 입력하세요">
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-status">혼인 상태</label>
          <select class="form-select" id="signup-status">
            <option value="">선택</option>
            <option value="미혼" ${state.signupDraft.maritalStatus === "미혼" ? "selected" : ""}>미혼</option>
            <option value="기혼" ${state.signupDraft.maritalStatus === "기혼" ? "selected" : ""}>기혼</option>
            <option value="기타" ${state.signupDraft.maritalStatus === "기타" ? "selected" : ""}>기타</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-household">가구원 수</label>
          <input class="form-input" id="signup-household" type="number" min="0" value="${state.signupDraft.householdCount}" placeholder="0">
        </div>
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-region">선호 지역(시/도)</label>
          <input class="form-input" id="signup-region" value="${state.signupDraft.preferredRegion}" placeholder="예: 경기">
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-district">선호 지역(시/군/구)</label>
          <input class="form-input" id="signup-district" value="${state.signupDraft.preferredDistrict}" placeholder="예: 오산시">
        </div>
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-house-type">선호 주택 유형</label>
          <input class="form-input" id="signup-house-type" value="${state.signupDraft.preferredHousingType}" placeholder="예: 아파트">
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-supply-type">선호 공급 유형</label>
          <input class="form-input" id="signup-supply-type" value="${state.signupDraft.preferredSupplyType}" placeholder="예: 공공임대">
        </div>
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-deposit">최대 보증금</label>
          <input class="form-input" id="signup-deposit" type="number" value="${state.signupDraft.maxDeposit}" placeholder="예: 20000000">
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-rent">최대 월세</label>
          <input class="form-input" id="signup-rent" type="number" value="${state.signupDraft.maxRent}" placeholder="예: 300000">
        </div>
      </div>
      <div class="form-group" style="margin-top:16px;">
        <label class="form-label">해당 유형 선택</label>
        <div class="category-grid">
          ${["청년", "신혼부부", "무주택자", "고령자", "저소득층", "다자녀"]
            .map(
              (label) => `
                <button class="category-chip ${state.signupDraft.categories.includes(label) ? "is-selected" : ""}" type="button" data-chip>
                  ${label}
                </button>
              `
            )
            .join("")}
        </div>
      </div>
      <div style="display:flex;gap:12px;margin-top:24px;">
        <button class="ghost-btn" type="button" data-step="back" style="width:140px;">이전</button>
        <button class="submit-btn" type="submit" style="flex:1;">가입 완료</button>
      </div>
    </form>
  `;
}

function bindEvents() {
  root.querySelectorAll("[data-tab]").forEach((button) => {
    button.addEventListener("click", () => {
      state.tab = button.dataset.tab;
      state.signupStep = 1;
      render();
    });
  });

  root.querySelectorAll("[data-toggle]").forEach((button) => {
    button.addEventListener("click", () => {
      const input = root.querySelector(button.dataset.toggle);
      if (input) input.type = input.type === "password" ? "text" : "password";
    });
  });

  root.querySelectorAll("[data-chip]").forEach((button) => {
    button.addEventListener("click", () => button.classList.toggle("is-selected"));
  });

  root.querySelector("[data-step='back']")?.addEventListener("click", () => {
    saveSignupStep2Draft();
    state.signupStep = 1;
    render();
  });

  root.querySelector("#login-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    await login(Object.fromEntries(formData.entries()));
    window.location.href = "./mypage.html";
  });

  root.querySelector("#signup-step1-form")?.addEventListener("submit", (event) => {
    event.preventDefault();
    state.signupDraft.loginId = root.querySelector("#signup-id")?.value.trim() ?? "";
    state.signupDraft.password = root.querySelector("#signup-password")?.value ?? "";
    state.signupDraft.email = root.querySelector("#signup-email")?.value.trim() ?? "";
    state.signupDraft.phone = root.querySelector("#signup-phone")?.value.trim() ?? "";
    state.signupStep = 2;
    render();
  });

  root.querySelector("#signup-step2-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    saveSignupStep2Draft();
    await signup(state.signupDraft);
    await saveProfile({
      id: `@${state.signupDraft.loginId}`,
      name: state.signupDraft.loginId,
      email: state.signupDraft.email,
      phone: state.signupDraft.phone,
      age: Number(state.signupDraft.age || 0),
      maritalStatus: state.signupDraft.maritalStatus,
      householdCount: Number(state.signupDraft.householdCount || 0),
      preferredRegion: state.signupDraft.preferredRegion,
      preferredDistrict: state.signupDraft.preferredDistrict,
      preferredHousingType: state.signupDraft.preferredHousingType,
      preferredSupplyType: state.signupDraft.preferredSupplyType,
      maxDeposit: Number(state.signupDraft.maxDeposit || 0),
      maxRent: Number(state.signupDraft.maxRent || 0),
      categories: [...state.signupDraft.categories]
    });
    state.signupStep = 3;
    render();
  });
}

function saveSignupStep2Draft() {
  state.signupDraft.age = root.querySelector("#signup-age")?.value ?? state.signupDraft.age;
  state.signupDraft.maritalStatus = root.querySelector("#signup-status")?.value ?? state.signupDraft.maritalStatus;
  state.signupDraft.householdCount = root.querySelector("#signup-household")?.value ?? state.signupDraft.householdCount;
  state.signupDraft.preferredRegion = root.querySelector("#signup-region")?.value ?? state.signupDraft.preferredRegion;
  state.signupDraft.preferredDistrict = root.querySelector("#signup-district")?.value ?? state.signupDraft.preferredDistrict;
  state.signupDraft.preferredHousingType = root.querySelector("#signup-house-type")?.value ?? state.signupDraft.preferredHousingType;
  state.signupDraft.preferredSupplyType = root.querySelector("#signup-supply-type")?.value ?? state.signupDraft.preferredSupplyType;
  state.signupDraft.maxDeposit = root.querySelector("#signup-deposit")?.value ?? state.signupDraft.maxDeposit;
  state.signupDraft.maxRent = root.querySelector("#signup-rent")?.value ?? state.signupDraft.maxRent;
  state.signupDraft.categories = Array.from(root.querySelectorAll("[data-chip].is-selected")).map((button) => button.textContent.trim());
}

render();
