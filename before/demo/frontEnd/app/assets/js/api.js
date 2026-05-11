import { listings, notificationSettings, notifications, userProfile } from "./mock-data.js";

const API_BASE_URL = "";
const USE_MOCK = true;
const delay = (value) => new Promise((resolve) => window.setTimeout(() => resolve(value), 120));
const clone = (value) => JSON.parse(JSON.stringify(value));

function withDetailDefaults(item) {
  return {
    ...item,
    info: item.info ?? [
      ["공급기관", item.provider],
      ["공급유형", item.supplyType],
      ["주택유형", item.housingType],
      ["공급지역", item.region],
      ["세대수", "공고문 확인"],
      ["신청방식", "인터넷 접수"],
      ["문의처", `${item.provider} 고객센터`],
      ["입주예정", "공고문 확인"]
    ],
    schedule: item.schedule ?? [
      { date: "공고문 게시일", title: "공고 게시", current: false },
      { date: item.deadlineLabel, title: "접수 마감 일정", desc: "세부 시간은 공고문을 확인하세요.", current: true, badge: "확인" },
      { date: "추후 안내", title: "후속 일정", current: false }
    ],
    market: item.market ?? {
      title: "주변 시세 대비 임대 조건 분석",
      depositMarket: item.deposit ? formatMoney(item.deposit * 1.6) : "공고문 확인",
      depositPublic: item.deposit ? formatMoney(item.deposit) : "공고문 확인",
      depositRate: 62,
      rentMarket: item.monthlyRent ? formatWonShort(item.monthlyRent * 1.4) : "공고문 확인",
      rentPublic: item.monthlyRent ? formatWonShort(item.monthlyRent) : "공고문 확인",
      rentRate: 68,
      summary: "세부 비교 수치는 실제 시세 데이터 연동 후 보정됩니다."
    },
    attachments: item.attachments ?? [{ label: "공고문", type: "PDF", href: "#" }],
    notes: item.notes ?? "상세 자격 요건과 접수 절차는 반드시 공고문 원문을 확인하세요.",
    applyUrl: item.applyUrl ?? "https://apply.lh.or.kr"
  };
}

function formatMoney(value) {
  return `${Math.round(value / 10000).toLocaleString("ko-KR")}만원`;
}

function formatWonShort(value) {
  return `${(value / 10000).toFixed(1).replace(".0", "")}만원`;
}

export async function fetchListings(params = {}) {
  if (!USE_MOCK) {
    const search = new URLSearchParams(params);
    const response = await fetch(`${API_BASE_URL}/listings?${search.toString()}`);
    return response.json();
  }

  const query = (params.q ?? "").trim().toLowerCase();
  const status = params.status ?? "all";
  const supplyType = params.type ?? "all";
  const region = params.region ?? "all";
  const sort = params.sort ?? "recommend";

  let filtered = listings.filter((item) => {
    const matchesQuery = !query || item.title.toLowerCase().includes(query) || item.region.toLowerCase().includes(query) || item.supplyType.toLowerCase().includes(query);
    const matchesStatus = status === "all" || item.status === status;
    const matchesType = supplyType === "all" || item.supplyType === supplyType;
    const matchesRegion = region === "all" || item.region.includes(region);
    return matchesQuery && matchesStatus && matchesType && matchesRegion;
  });

  filtered = filtered.sort((a, b) => {
    if (sort === "deadline") return (b.urgent ? 1 : 0) - (a.urgent ? 1 : 0);
    if (sort === "latest") return a.title.localeCompare(b.title, "ko");
    if (sort === "price-low") return (a.deposit ?? a.salePrice ?? Number.MAX_SAFE_INTEGER) - (b.deposit ?? b.salePrice ?? Number.MAX_SAFE_INTEGER);
    return (b.liked ? 1 : 0) - (a.liked ? 1 : 0);
  });

  return delay({ items: clone(filtered), total: filtered.length, page: Number(params.page ?? 1), pageSize: 8 });
}

export async function fetchListingDetail(id) {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/listings/${id}`);
    return response.json();
  }
  return delay(clone(withDetailDefaults(listings.find((item) => item.id === id) ?? listings[0])));
}

export async function fetchRecommendedListings() {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/recommendations`);
    return response.json();
  }
  const items = listings.filter((item) => item.status !== "closed").slice(0, 4).map((item, index) => ({ ...item, score: 96 - index * 7 }));
  return delay({ profile: clone(userProfile), items: clone(items) });
}

export async function login(payload) {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    return response.json();
  }
  return delay({ success: true, user: clone(userProfile), token: "mock-token" });
}

export async function signup(payload) {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    return response.json();
  }
  return delay({ success: true, user: { ...clone(userProfile), email: payload.email ?? userProfile.email } });
}

export async function fetchMyProfile() {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/me`);
    return response.json();
  }
  return delay(clone(userProfile));
}

export async function fetchSavedListings() {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/me/saved-listings`);
    return response.json();
  }
  return delay(clone(listings.filter((item) => item.liked)));
}

export async function fetchNotifications() {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/me/notifications`);
    return response.json();
  }
  return delay(clone(notifications));
}

export async function fetchNotificationSettings() {
  if (!USE_MOCK) {
    const response = await fetch(`${API_BASE_URL}/me/notification-settings`);
    return response.json();
  }
  return delay(clone(notificationSettings));
}
