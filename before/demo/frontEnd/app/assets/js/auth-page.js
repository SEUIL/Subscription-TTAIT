import { login, signup } from "./api.js";
import { renderAuthHeader } from "./components.js";

const root = document.getElementById("page-root");
const state = { tab: "login", signupStep: 1 };

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
      <h1 class="form-title">다시 만나서 반가워요</h1>
      <p class="form-subtitle">공공임대주택 공고를 저장하고<br>맞춤 추천도 받아보세요.</p>
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
        <p class="terms-text" style="margin-top:20px;"><a href="#">비밀번호를 잊으셨나요?</a></p>
      </form>
    </section>
  `;
}

function renderSignupPanel() {
  if (state.signupStep === 3) {
    return `
      <section class="auth-section" style="text-align:center;">
        <div style="font-size:48px;">🎉</div>
        <h2 class="form-title" style="font-size:28px;margin-top:16px;">가입이 완료되었어요</h2>
        <p class="form-subtitle">입력한 정보를 기반으로 맞춤 공고를 추천해드릴게요.</p>
        <a class="submit-btn" href="./list.html" style="width:100%;margin-top:32px;">공고 둘러보기</a>
      </section>
    `;
  }

  const isStep1 = state.signupStep === 1;
  return `
    <section class="auth-section">
      <div class="step-indicator">
        <div class="step ${isStep1 ? "is-current" : "is-done"}">${isStep1 ? "1" : "✓"}</div>
        <div class="step-line ${!isStep1 ? "is-done" : ""}"></div>
        <div class="step ${isStep1 ? "is-pending" : "is-current"}">2</div>
      </div>
      ${isStep1 ? renderSignupStep1() : renderSignupStep2()}
    </section>
  `;
}

function renderSignupStep1() {
  return `
    <form id="signup-step1-form">
      <h1 class="form-title">계정 만들기</h1>
      <p class="form-subtitle">기본 정보를 입력해주세요.</p>
      <div class="form-group" style="margin-top:24px;">
        <label class="form-label" for="signup-id">아이디</label>
        <input class="form-input" id="signup-id" type="text" placeholder="영문, 숫자 4~20자">
        <p class="form-hint">사용 가능한 아이디입니다.</p>
      </div>
      <div class="form-group" style="margin-top:16px;">
        <label class="form-label" for="signup-password">비밀번호</label>
        <div class="input-wrapper">
          <input class="form-input" id="signup-password" type="password" placeholder="8자 이상, 영문+숫자 조합">
          <button class="input-toggle" type="button" data-toggle="#signup-password" aria-label="비밀번호 보기">
            <svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          </button>
        </div>
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-email">이메일</label>
          <input class="form-input" id="signup-email" type="email" placeholder="이메일 주소">
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-phone">휴대폰 번호</label>
          <input class="form-input" id="signup-phone" type="tel" placeholder="010-0000-0000">
        </div>
      </div>
      <button class="submit-btn" type="submit" style="width:100%;margin-top:24px;">다음 단계</button>
      <p class="terms-text" style="margin-top:16px;">가입하면 <a href="#">이용약관</a>과 <a href="#">개인정보처리방침</a>에 동의한 것으로 간주됩니다.</p>
    </form>
  `;
}

function renderSignupStep2() {
  return `
    <form id="signup-step2-form">
      <h1 class="form-title">맞춤 정보 입력</h1>
      <p class="form-subtitle">추천 정확도를 높이기 위한 선택 입력입니다.</p>
      <div class="form-group" style="margin-top:24px;">
        <label class="form-label" for="signup-age">나이</label>
        <input class="form-input" id="signup-age" type="number" placeholder="나이를 입력하세요">
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-status">혼인 상태</label>
          <select class="form-select" id="signup-status">
            <option value="">선택</option>
            <option>미혼</option>
            <option>기혼</option>
            <option>기타</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-household">가구원 수</label>
          <input class="form-input" id="signup-household" type="number" min="0" placeholder="0">
        </div>
      </div>
      <div class="form-row" style="margin-top:16px;">
        <div class="form-group">
          <label class="form-label" for="signup-region">선호 지역 (시/도)</label>
          <select class="form-select" id="signup-region">
            <option value="">선택</option>
            <option>경기도</option>
            <option>서울특별시</option>
            <option>인천광역시</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label" for="signup-district">선호 지역 (구/시)</label>
          <select class="form-select" id="signup-district">
            <option value="">선택</option>
            <option>오산시</option>
            <option>부평구</option>
            <option>강서구</option>
          </select>
        </div>
      </div>
      <div class="form-group" style="margin-top:16px;">
        <label class="form-label">해당 유형 선택</label>
        <div class="category-grid">
          ${["청년", "신혼부부", "무주택자", "고령자", "저소득층", "다자녀"].map((label) => `<button class="category-chip ${label === "청년" ? "is-selected" : ""}" type="button" data-chip>${label}</button>`).join("")}
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
    state.signupStep = 2;
    render();
  });

  root.querySelector("#signup-step2-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    await signup({});
    state.signupStep = 3;
    render();
  });
}

render();
