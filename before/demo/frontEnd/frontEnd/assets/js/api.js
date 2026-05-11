import { listings, notificationSettings, notifications, userProfile } from "./mock-data.js";

const DEFAULT_API_BASE_URL = "";
const USE_MOCK_ONLY = false;

const STORAGE_KEYS = {
  token: "jg.accessToken",
  savedIds: "jg.savedIds",
  savedSnapshots: "jg.savedSnapshots",
  notificationSettings: "jg.notificationSettings",
  profileOverride: "jg.profileOverride"
};

const delay = (value) => new Promise((resolve) => window.setTimeout(() => resolve(value), 120));
const clone = (value) => JSON.parse(JSON.stringify(value));

function safeArray(value) {
  return Array.isArray(value) ? value : [];
}

function readJson(key, fallback) {
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function writeJson(key, value) {
  window.localStorage.setItem(key, JSON.stringify(value));
}

function getApiBaseUrl() {
  const configured = window.localStorage.getItem("jg.apiBaseUrl");
  return (configured || DEFAULT_API_BASE_URL).replace(/\/+$/, "");
}

function getAuthToken() {
  return window.localStorage.getItem(STORAGE_KEYS.token) ?? "";
}

function setAuthToken(token) {
  if (!token) return;
  window.localStorage.setItem(STORAGE_KEYS.token, token);
}

export function isAuthenticated() {
  return Boolean(getAuthToken());
}

export function logout() {
  window.localStorage.removeItem(STORAGE_KEYS.token);
}

function createHeaders(extraHeaders = {}, auth = false) {
  const headers = { ...extraHeaders };
  if (auth && getAuthToken()) {
    headers.Authorization = `Bearer ${getAuthToken()}`;
  }
  return headers;
}

async function request(path, options = {}) {
  const response = await fetch(`${getApiBaseUrl()}${path}`, options);
  if (!response.ok) {
    const error = new Error(`HTTP ${response.status}`);
    error.status = response.status;
    throw error;
  }
  return response;
}

async function requestJson(path, options = {}) {
  const response = await request(path, options);
  if (response.status === 204) return null;
  return response.json();
}

function formatMoney(value) {
  if (value == null || Number.isNaN(Number(value))) return "공고문 확인";
  return `${Math.round(Number(value) / 10000).toLocaleString("ko-KR")}만원`;
}

function formatRentShort(value) {
  if (value == null || Number.isNaN(Number(value))) return "공고문 확인";
  return `${(Number(value) / 10000).toFixed(1).replace(".0", "")}만원`;
}

function formatDateLabel(value) {
  if (!value) return "일정 확인 필요";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", { month: "long", day: "numeric" }).format(date);
}

function formatDateTimeLabel(value) {
  if (!value) return "확인 필요";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function daysUntil(dateString) {
  if (!dateString) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(dateString);
  target.setHours(0, 0, 0, 0);
  if (Number.isNaN(target.getTime())) return null;
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

function normalizeStatus(rawStatus, endDate) {
  if (rawStatus && rawStatus !== "OPEN") return "closed";
  const diff = daysUntil(endDate);
  if (diff != null && diff < 0) return "closed";
  if (diff != null && diff <= 3) return "closing";
  return "open";
}

function createDeadlineLabel(endDate, status) {
  if (!endDate) {
    return status === "closed" ? "모집 종료" : "일정 확인 필요";
  }
  const diff = daysUntil(endDate);
  const label = formatDateLabel(endDate);
  if (diff == null) return label;
  if (diff < 0 || status === "closed") return `모집 종료 · ${label}`;
  if (diff === 0) return `오늘 마감 · ${label}`;
  return `마감 D-${diff} · ${label}`;
}

function createBadges(item) {
  const first =
    item.status === "closing" ? "마감임박" : item.status === "closed" ? "모집 종료" : "모집중";
  return [first, item.supplyType, item.housingType].filter(Boolean);
}

function createTags(item) {
  const base = safeArray(item.recommendedTags).map((tag) => ({ label: String(tag), tone: "blue" }));
  if (!base.length && item.supplyType) {
    base.push({ label: item.supplyType, tone: "default" });
  }
  return base.slice(0, 3);
}

function readSavedIds() {
  return readJson(STORAGE_KEYS.savedIds, []);
}

function writeSavedIds(ids) {
  writeJson(STORAGE_KEYS.savedIds, ids);
}

function readSavedSnapshots() {
  return readJson(STORAGE_KEYS.savedSnapshots, {});
}

function writeSavedSnapshots(value) {
  writeJson(STORAGE_KEYS.savedSnapshots, value);
}

function readProfileOverride() {
  return readJson(STORAGE_KEYS.profileOverride, {});
}

function writeProfileOverride(value) {
  writeJson(STORAGE_KEYS.profileOverride, value);
}

function mapMockStatus(item) {
  if (item.status === "closing") return "closing";
  if (item.status === "closed") return "closed";
  return "open";
}

function toDisplayProfile(profile) {
  const categoryLabelMap = {
    YOUTH: "청년",
    NEWLYWED: "신혼부부",
    HOMELESS: "무주택자",
    ELDERLY: "고령자",
    LOW_INCOME: "저소득층",
    MULTI_CHILD: "다자녀"
  };

  return {
    id: profile.loginId ? `@${profile.loginId}` : profile.id ?? "@guest",
    name: profile.loginId || profile.name || "사용자",
    email: profile.email ?? "",
    phone: profile.phone ?? "",
    age: profile.age ?? "",
    maritalStatus:
      profile.maritalStatus === "SINGLE"
        ? "미혼"
        : profile.maritalStatus === "MARRIED"
          ? "기혼"
          : profile.maritalStatus === "OTHER"
            ? "기타"
            : profile.maritalStatus ?? "",
    householdCount: profile.childrenCount ?? profile.householdCount ?? 0,
    preferredRegion: profile.preferredRegionLevel1 ?? profile.preferredRegion ?? "",
    preferredDistrict: profile.preferredRegionLevel2 ?? profile.preferredDistrict ?? "",
    preferredHousingType: profile.preferredHouseType ?? profile.preferredHousingType ?? "",
    preferredSupplyType: profile.preferredSupplyType ?? "",
    maxDeposit: profile.maxDeposit ?? 0,
    maxRent: profile.maxMonthlyRent ?? profile.maxRent ?? 0,
    categories: safeArray(profile.categories).map((category) => categoryLabelMap[String(category)] ?? String(category))
  };
}

function toProfilePayload(profile) {
  const maritalStatusMap = {
    미혼: "SINGLE",
    기혼: "MARRIED",
    기타: "OTHER",
    SINGLE: "SINGLE",
    MARRIED: "MARRIED",
    OTHER: "OTHER"
  };

  const categoryMap = {
    청년: "YOUTH",
    신혼부부: "NEWLYWED",
    무주택자: "HOMELESS",
    고령자: "ELDERLY",
    저소득층: "LOW_INCOME",
    다자녀: "MULTI_CHILD"
  };

  return {
    age: Number(profile.age || 0),
    maritalStatus: maritalStatusMap[profile.maritalStatus] ?? "OTHER",
    childrenCount: Math.max(Number(profile.householdCount || 0) - 1, 0),
    isHomeless: safeArray(profile.categories).includes("무주택자"),
    isLowIncome: safeArray(profile.categories).includes("저소득층"),
    isElderly: safeArray(profile.categories).includes("고령자"),
    preferredRegionLevel1: profile.preferredRegion ?? "",
    preferredRegionLevel2: profile.preferredDistrict ?? "",
    preferredHouseType: profile.preferredHousingType ?? "",
    preferredSupplyType: profile.preferredSupplyType ?? "",
    maxDeposit: Number(profile.maxDeposit || 0),
    maxMonthlyRent: Number(profile.maxRent || 0),
    categories: safeArray(profile.categories).map((item) => categoryMap[item] ?? item)
  };
}

function mapAnnouncementSummary(item) {
  const savedIds = readSavedIds();
  const status = normalizeStatus(item.noticeStatus, item.applicationEndDate);
  const mapped = {
    id: String(item.announcementId),
    provider: item.providerName ?? "공급기관",
    providerDetail: [item.providerName, item.regionLevel1, item.regionLevel2].filter(Boolean).join(" · "),
    region: [item.regionLevel1, item.regionLevel2].filter(Boolean).join(" "),
    title: item.noticeName ?? "공고명 미확인",
    housingType: item.houseType ?? "주택유형 확인 필요",
    supplyType: item.supplyType ?? "공급유형 확인 필요",
    status,
    thumbnailEmoji: status === "closing" ? "⏳" : status === "closed" ? "📄" : "🏠",
    deposit: item.depositAmount ?? null,
    monthlyRent: item.monthlyRentAmount ?? null,
    salePrice: null,
    deadlineLabel: createDeadlineLabel(item.applicationEndDate, status),
    urgent: status === "closing",
    liked: Boolean(item.isSaved) || savedIds.includes(String(item.announcementId)),
    tags: createTags(item),
    badges: createBadges({
      status,
      supplyType: item.supplyType,
      housingType: item.houseType ?? "주택유형 확인 필요"
    }),
    recommendedReason:
      safeArray(item.recommendedTags).length > 0
        ? `${item.recommendedTags.join(", ")} 조건과 맞는 공고입니다.`
        : "추천 사유 데이터가 없어 MVP 기본 문구를 표시합니다."
  };
  return mapped;
}

function mapAttachment(item) {
  return {
    label: item.attachmentName ?? "첨부파일",
    type: item.attachmentType ?? "LINK",
    href: item.attachmentUrl ?? "#"
  };
}

function mapAnnouncementDetail(item) {
  const summary = mapAnnouncementSummary(item);
  const market = item.marketComparison
    ? {
        title: "주변 시세 비교",
        depositMarket: formatMoney(item.marketComparison.marketAverageDepositAmount),
        depositPublic: formatMoney(item.marketComparison.publicDepositAmount),
        depositRate:
          item.marketComparison.marketAverageDepositAmount > 0
            ? Math.min(
                100,
                Math.round(
                  (Number(item.marketComparison.publicDepositAmount || 0) /
                    Number(item.marketComparison.marketAverageDepositAmount || 1)) *
                    100
                )
              )
            : 0,
        rentMarket: formatRentShort(item.marketComparison.marketAverageMonthlyRentAmount),
        rentPublic: formatRentShort(item.marketComparison.publicMonthlyRentAmount),
        rentRate:
          item.marketComparison.marketAverageMonthlyRentAmount > 0
            ? Math.min(
                100,
                Math.round(
                  (Number(item.marketComparison.publicMonthlyRentAmount || 0) /
                    Number(item.marketComparison.marketAverageMonthlyRentAmount || 1)) *
                    100
                )
              )
            : 0,
        summary: item.marketComparison.comparisonSummary ?? "시세 비교 요약이 없습니다."
      }
    : {
        title: "주변 시세 비교",
        depositMarket: "비교 데이터 없음",
        depositPublic: summary.deposit ? formatMoney(summary.deposit) : "공고문 확인",
        depositRate: 0,
        rentMarket: "비교 데이터 없음",
        rentPublic: summary.monthlyRent ? formatRentShort(summary.monthlyRent) : "공고문 확인",
        rentRate: 0,
        summary: "주택 유형 또는 시세 데이터가 없어 비교를 생략했습니다."
      };

  const info = [
    ["공급기관", item.providerName ?? "확인 필요"],
    ["공급유형", item.supplyType ?? "확인 필요"],
    ["주택유형", item.houseType ?? "확인 필요"],
    ["지역", [item.regionLevel1, item.regionLevel2].filter(Boolean).join(" ") || "확인 필요"],
    ["단지명", item.complexName ?? "확인 필요"],
    ["주소", item.fullAddress ?? "확인 필요"],
    ["세대수", item.householdCount ? `${item.householdCount.toLocaleString("ko-KR")}세대` : "확인 필요"],
    ["공급세대", item.supplyHouseholdCount ? `${item.supplyHouseholdCount.toLocaleString("ko-KR")}세대` : "확인 필요"],
    ["전용면적", item.exclusiveAreaText ?? "확인 필요"],
    ["난방방식", item.heatingType ?? "확인 필요"],
    ["입주예정", item.moveInExpectedYm ?? "확인 필요"],
    ["문의처", item.contactPhone ?? "확인 필요"]
  ];

  const schedule = [
    { date: formatDateLabel(item.announcementDate), title: "공고 게시", current: false },
    {
      date: formatDateLabel(item.applicationStartDate),
      title: "접수 시작",
      desc: item.applicationDatetimeText ?? undefined,
      current: false
    },
    {
      date: formatDateLabel(item.applicationEndDate),
      title: "접수 마감",
      badge: createDeadlineLabel(item.applicationEndDate, summary.status).replace(` · ${formatDateLabel(item.applicationEndDate)}`, ""),
      current: summary.status !== "closed"
    },
    { date: formatDateLabel(item.winnerAnnouncementDate), title: "당첨 발표", current: false }
  ].filter((step) => step.date && step.date !== "일정 확인 필요");

  return {
    ...summary,
    info,
    schedule,
    market,
    attachments: safeArray(item.attachments).map(mapAttachment),
    notes: item.guideText ?? "공고문과 원문 링크를 함께 확인해 주세요.",
    applyUrl: item.sourceUrl ?? "https://apply.lh.or.kr",
    badges: createBadges(summary),
    region: [item.regionLevel1, item.regionLevel2].filter(Boolean).join(" "),
    tags: [
      ...createTags(item),
      ...safeArray(item.recommendReasons).slice(0, 2).map((reason) => ({ label: String(reason), tone: "pink" }))
    ]
  };
}

function withDetailDefaults(item) {
  return {
    ...item,
    liked: Boolean(item.liked),
    info:
      item.info ??
      [
        ["공급기관", item.provider],
        ["공급유형", item.supplyType],
        ["주택유형", item.housingType],
        ["지역", item.region]
      ],
    schedule:
      item.schedule ??
      [
        { date: item.deadlineLabel, title: "접수 일정", current: true, badge: "확인" }
      ],
    market:
      item.market ?? {
        title: "주변 시세 비교",
        depositMarket: item.deposit ? formatMoney(item.deposit * 1.6) : "공고문 확인",
        depositPublic: item.deposit ? formatMoney(item.deposit) : "공고문 확인",
        depositRate: 62,
        rentMarket: item.monthlyRent ? formatRentShort(item.monthlyRent * 1.4) : "공고문 확인",
        rentPublic: item.monthlyRent ? formatRentShort(item.monthlyRent) : "공고문 확인",
        rentRate: 68,
        summary: "실제 시세 비교 API 응답이 없어 목업 요약을 사용합니다."
      },
    attachments: item.attachments ?? [{ label: "공고문", type: "PDF", href: "#" }],
    notes: item.notes ?? "상세 자격요건은 공고문 원문을 확인해 주세요.",
    applyUrl: item.applyUrl ?? "https://apply.lh.or.kr"
  };
}

function mergeWithSavedState(item) {
  return { ...item, liked: readSavedIds().includes(String(item.id)) || Boolean(item.liked) };
}

function mockFetchListings(params = {}) {
  const query = (params.q ?? "").trim().toLowerCase();
  const status = params.status ?? "all";
  const supplyType = params.type ?? "all";
  const region = params.region ?? "all";
  const sort = params.sort ?? "recommend";

  let filtered = listings
    .map((item) => mergeWithSavedState({ ...item, status: mapMockStatus(item) }))
    .filter((item) => {
      const matchesQuery =
        !query ||
        item.title.toLowerCase().includes(query) ||
        item.region.toLowerCase().includes(query) ||
        item.supplyType.toLowerCase().includes(query);
      const matchesStatus = status === "all" || item.status === status;
      const matchesType = supplyType === "all" || item.supplyType === supplyType;
      const matchesRegion = region === "all" || item.region.includes(region);
      return matchesQuery && matchesStatus && matchesType && matchesRegion;
    });

  filtered = filtered.sort((a, b) => {
    if (sort === "deadline") return (b.urgent ? 1 : 0) - (a.urgent ? 1 : 0);
    if (sort === "latest") return a.title.localeCompare(b.title, "ko");
    return (b.liked ? 1 : 0) - (a.liked ? 1 : 0);
  });

  filtered.forEach(rememberListingSnapshot);
  return delay({ items: clone(filtered), total: filtered.length, page: Number(params.page ?? 1), pageSize: 8 });
}

export function rememberListingSnapshot(item) {
  if (!item?.id) return;
  const snapshots = readSavedSnapshots();
  snapshots[String(item.id)] = clone(item);
  writeSavedSnapshots(snapshots);
}

export async function fetchListings(params = {}) {
  if (USE_MOCK_ONLY) return mockFetchListings(params);

  try {
    const search = new URLSearchParams();
    search.set("page", String(Math.max(Number(params.page ?? 1) - 1, 0)));
    search.set("size", "20");
    if (params.q) search.set("keyword", params.q);
    if (params.region && params.region !== "all") search.set("regionLevel1", params.region);
    if (params.type && params.type !== "all") search.set("supplyType", params.type);
    if (params.sort && params.sort !== "recommend") {
      search.set("sort", params.sort === "latest" ? "latest" : "deadline");
    } else {
      search.set("sort", "recommended");
    }
    if (params.status === "open") search.set("status", "OPEN");

    const data = await requestJson(`/api/announcements?${search.toString()}`, {
      headers: createHeaders({}, false)
    });
    let items = safeArray(data?.content).map((item) => mergeWithSavedState(mapAnnouncementSummary(item)));
    if (params.status === "closing") {
      items = items.filter((item) => item.status === "closing");
    }
    items.forEach(rememberListingSnapshot);
    return {
      items,
      total: data?.totalElements ?? items.length,
      page: (data?.number ?? 0) + 1,
      pageSize: data?.size ?? items.length,
      totalPages: data?.totalPages ?? 1
    };
  } catch {
    return mockFetchListings(params);
  }
}

export async function fetchListingDetail(id) {
  if (USE_MOCK_ONLY) {
    return delay(clone(withDetailDefaults(mergeWithSavedState(listings.find((item) => String(item.id) === String(id)) ?? listings[0]))));
  }

  try {
    const data = await requestJson(`/api/announcements/${id}`, {
      headers: createHeaders({}, false)
    });
    const mapped = mergeWithSavedState(mapAnnouncementDetail(data));
    rememberListingSnapshot(mapped);
    return mapped;
  } catch {
    const fallback = listings.find((item) => String(item.id) === String(id)) ?? listings[0];
    return delay(clone(withDetailDefaults(mergeWithSavedState(fallback))));
  }
}

export async function fetchRecommendedListings() {
  const profile = await fetchMyProfile();
  const fallbackItems = listings
    .filter((item) => item.status !== "closed")
    .slice(0, 4)
    .map((item, index) => ({
      ...mergeWithSavedState(item),
      score: 96 - index * 7,
      recommendedReason: item.recommendedReason ?? "추천 사유 데이터가 없어 목업 문구를 사용합니다."
    }));

  if (USE_MOCK_ONLY || !isAuthenticated()) {
    fallbackItems.forEach(rememberListingSnapshot);
    return delay({ profile, items: clone(fallbackItems) });
  }

  try {
    const data = await requestJson("/api/recommendations?page=0&size=20", {
      headers: createHeaders({}, true)
    });
    const items = safeArray(data?.content)
      .map((item, index) => ({
        ...mergeWithSavedState(mapAnnouncementSummary(item)),
        score: 97 - index * 4,
        recommendedReason:
          safeArray(item.recommendedTags).length > 0
            ? `${item.recommendedTags.join(", ")} 조건과 잘 맞습니다.`
            : "백엔드 추천 사유 필드가 없어 MVP 기본 문구를 사용합니다."
      }))
      .slice(0, 8);
    items.forEach(rememberListingSnapshot);
    return { profile, items };
  } catch {
    fallbackItems.forEach(rememberListingSnapshot);
    return delay({ profile, items: clone(fallbackItems) });
  }
}

export async function login(payload) {
  if (USE_MOCK_ONLY) {
    setAuthToken("mock-token");
    return delay({ success: true, user: clone(userProfile), token: "mock-token" });
  }

  try {
    const data = await requestJson("/api/auth/login", {
      method: "POST",
      headers: createHeaders({ "Content-Type": "application/json" }, false),
      body: JSON.stringify({
        loginId: payload.id ?? payload.loginId ?? "",
        password: payload.password ?? ""
      })
    });
    setAuthToken(data?.accessToken ?? "");
    const profile = await fetchMyProfile();
    return { success: true, user: profile, token: data?.accessToken ?? "" };
  } catch {
    setAuthToken("mock-token");
    return delay({ success: true, user: clone(userProfile), token: "mock-token" });
  }
}

export async function signup(payload) {
  if (USE_MOCK_ONLY) {
    setAuthToken("mock-token");
    return delay({
      success: true,
      user: { ...clone(userProfile), email: payload.email ?? userProfile.email },
      token: "mock-token"
    });
  }

  try {
    const data = await requestJson("/api/auth/signup", {
      method: "POST",
      headers: createHeaders({ "Content-Type": "application/json" }, false),
      body: JSON.stringify({
        loginId: payload.loginId ?? payload.id ?? "",
        password: payload.password ?? "",
        phone: payload.phone ?? "",
        email: payload.email ?? ""
      })
    });
    setAuthToken(data?.accessToken ?? "");
    return { success: true, user: toDisplayProfile(data), token: data?.accessToken ?? "" };
  } catch {
    setAuthToken("mock-token");
    return delay({
      success: true,
      user: { ...clone(userProfile), email: payload.email ?? userProfile.email },
      token: "mock-token"
    });
  }
}

export async function fetchMyProfile() {
  const override = readProfileOverride();

  if (USE_MOCK_ONLY || !isAuthenticated()) {
    return delay({ ...clone(userProfile), ...override });
  }

  try {
    const data = await requestJson("/api/me", {
      headers: createHeaders({}, true)
    });
    const profile = { ...toDisplayProfile(data), ...override };
    writeProfileOverride(profile);
    return profile;
  } catch {
    return delay({ ...clone(userProfile), ...override });
  }
}

export async function saveProfile(profile) {
  writeProfileOverride(profile);

  if (USE_MOCK_ONLY || !isAuthenticated()) {
    return delay(clone(profile));
  }

  try {
    const data = await requestJson("/api/me/profile", {
      method: "PUT",
      headers: createHeaders({ "Content-Type": "application/json" }, true),
      body: JSON.stringify(toProfilePayload(profile))
    });
    const mapped = toDisplayProfile(data);
    writeProfileOverride(mapped);
    return mapped;
  } catch {
    return delay(clone(profile));
  }
}

export async function fetchSavedListings() {
  const savedIds = readSavedIds();
  const snapshots = readSavedSnapshots();
  const items = savedIds.map((id) => snapshots[id]).filter(Boolean).map(mergeWithSavedState);

  if (items.length > 0) return delay(clone(items));
  return delay(clone(listings.filter((item) => item.liked).map(mergeWithSavedState)));
}

export async function toggleSavedListing(itemOrId, forceValue) {
  const item = typeof itemOrId === "object" ? itemOrId : null;
  const id = String(item?.id ?? itemOrId);
  const savedIds = readSavedIds();
  const currentlySaved = savedIds.includes(id);
  const nextValue = typeof forceValue === "boolean" ? forceValue : !currentlySaved;

  if (item) rememberListingSnapshot(item);

  if (!USE_MOCK_ONLY && isAuthenticated()) {
    try {
      await request(`/api/announcements/${id}/save`, {
        method: nextValue ? "POST" : "DELETE",
        headers: createHeaders({}, true)
      });
    } catch {
      // Keep local fallback even when the API call fails in MVP mode.
    }
  }

  const nextIds = nextValue ? [...new Set([...savedIds, id])] : savedIds.filter((savedId) => savedId !== id);
  writeSavedIds(nextIds);

  const snapshots = readSavedSnapshots();
  if (item) snapshots[id] = { ...clone(item), liked: nextValue };
  if (!nextValue) delete snapshots[id];
  writeSavedSnapshots(snapshots);

  return nextValue;
}

function getNotificationSettingsStore() {
  return readJson(STORAGE_KEYS.notificationSettings, clone(notificationSettings));
}

export async function fetchNotifications() {
  if (USE_MOCK_ONLY || !isAuthenticated()) {
    return delay(clone(notifications));
  }

  try {
    const data = await requestJson("/api/notifications", {
      headers: createHeaders({}, true)
    });
    return safeArray(data).map((item) => ({
      id: String(item.notificationId),
      title: item.title ?? "알림",
      message: item.message ?? "",
      read: Boolean(item.isRead),
      createdAt: formatDateTimeLabel(item.createdAt),
      announcementId: item.announcementId ? String(item.announcementId) : null
    }));
  } catch {
    return delay(clone(notifications));
  }
}

export async function markNotificationRead(notificationId) {
  if (!USE_MOCK_ONLY && isAuthenticated()) {
    try {
      await request(`/api/notifications/${notificationId}/read`, {
        method: "PATCH",
        headers: createHeaders({}, true)
      });
      return true;
    } catch {
      return false;
    }
  }
  return true;
}

export async function fetchNotificationSettings() {
  return delay(clone(getNotificationSettingsStore()));
}

export async function saveNotificationSettings(nextSettings) {
  writeJson(STORAGE_KEYS.notificationSettings, nextSettings);
  return delay(clone(nextSettings));
}
