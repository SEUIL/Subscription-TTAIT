export const listings = [
  {
    id: "osan-segyo-21",
    provider: "LH",
    providerDetail: "LH · 경기도 오산시 초평중앙로 15",
    region: "경기도 오산시",
    title: "오산세교2 21단지 국민임대주택 예비입주자 모집공고",
    housingType: "아파트",
    supplyType: "국민임대",
    status: "open",
    thumbnailEmoji: "🏢",
    deposit: 11190000,
    monthlyRent: 149570,
    salePrice: null,
    deadlineLabel: "마감 D-6 · 4월 14일",
    urgent: true,
    liked: false,
    tags: [
      { label: "청년", tone: "blue" },
      { label: "국민임대", tone: "default" }
    ],
    badges: ["모집중", "국민임대", "아파트"],
    info: [
      ["공급기관", "LH (한국토지주택공사)"],
      ["공급유형", "국민임대"],
      ["주택유형", "아파트"],
      ["단지명", "오산세교2 21단지 (A-15블록)"],
      ["전용면적", "46.01㎡"],
      ["세대수", "총 694세대 (공급 155세대)"],
      ["신청방식", "인터넷 접수"],
      ["입주예정", "2026년 10월"]
    ],
    schedule: [
      { date: "2026년 4월 4일", title: "공고 게시", current: false },
      { date: "2026년 4월 6일 ~ 4월 14일", title: "신청 접수 기간", desc: "2026.04.13 10:00 ~ 2026.04.15 17:00", current: true, badge: "D-6" },
      { date: "2026년 5월 7일", title: "당첨자 발표", current: false }
    ],
    market: {
      title: "주변 시세 대비 임대 조건 분석",
      depositMarket: "2,800만원",
      depositPublic: "1,119만원",
      depositRate: 40,
      rentMarket: "32만원",
      rentPublic: "14.9만원",
      rentRate: 47,
      summary: "인근 시세 대비 월세 부담이 약 53% 낮음"
    },
    attachments: [{ label: "공고문", type: "PDF", href: "#" }],
    notes: "예비입주자 모집 관련 자세한 사항은 공고문을 반드시 확인하시기 바랍니다.",
    applyUrl: "https://apply.lh.or.kr",
    recommendedReason: "저장한 조건과 예산, 경기 남부 선호 지역이 모두 일치합니다."
  },
  {
    id: "incheon-newlywed-26-2",
    provider: "LH",
    providerDetail: "LH · 인천광역시",
    region: "인천광역시",
    title: "26년 2차 신혼·신생아Ⅰ 예비입주자 모집공고 (인천,부천)",
    housingType: "행복주택",
    supplyType: "행복주택",
    status: "closing",
    thumbnailEmoji: "🏗️",
    deposit: 85000000,
    monthlyRent: 320000,
    salePrice: null,
    deadlineLabel: "마감 D-1 · 4월 8일",
    urgent: true,
    liked: true,
    tags: [
      { label: "신혼부부", tone: "default" },
      { label: "행복주택", tone: "default" }
    ],
    badges: ["마감임박", "행복주택", "신혼부부"],
    info: [
      ["공급기관", "LH (인천지역본부)"],
      ["공급유형", "행복주택"],
      ["주택유형", "아파트"],
      ["공급지역", "인천광역시, 부천시"],
      ["전용면적", "36㎡ ~ 59㎡"],
      ["세대수", "공급 214세대"],
      ["신청방식", "인터넷 접수"],
      ["입주예정", "2026년 하반기"]
    ],
    schedule: [
      { date: "2026년 3월 30일", title: "공고 게시", current: false },
      { date: "2026년 4월 7일 ~ 4월 8일", title: "신청 접수 기간", desc: "2026.04.08 17:00 마감", current: true, badge: "D-1" },
      { date: "2026년 4월 29일", title: "당첨자 발표", current: false }
    ],
    market: {
      title: "주변 시세 대비 임대 조건 분석",
      depositMarket: "1억 1,000만원",
      depositPublic: "8,500만원",
      depositRate: 77,
      rentMarket: "45만원",
      rentPublic: "32만원",
      rentRate: 71,
      summary: "인근 시세 대비 월세 부담이 약 29% 낮음"
    },
    attachments: [{ label: "정정 공고문", type: "PDF", href: "#" }],
    notes: "마감일이 임박한 공고로 신청 시간과 자격을 다시 확인하세요.",
    applyUrl: "https://apply.lh.or.kr",
    recommendedReason: "신혼부부 유형과 인천 선호 지역이 일치해 우선 추천됩니다."
  },
  {
    id: "magok-permanent",
    provider: "LH",
    providerDetail: "LH · 서울특별시 강서구",
    region: "서울특별시",
    title: "서울 강서구 마곡지구 영구임대 입주자 모집공고",
    housingType: "아파트",
    supplyType: "영구임대",
    status: "open",
    thumbnailEmoji: "🏠",
    deposit: 23400000,
    monthlyRent: 98000,
    salePrice: null,
    deadlineLabel: "마감 D-15 · 4월 23일",
    urgent: false,
    liked: false,
    tags: [
      { label: "영구임대", tone: "default" },
      { label: "저소득", tone: "green" }
    ],
    badges: ["모집중", "영구임대", "아파트"],
    recommendedReason: "월세 부담이 낮고 서울권 대기 목록을 탐색할 때 적합합니다."
  },
  {
    id: "gwanggyo-sale",
    provider: "LH",
    providerDetail: "LH · 경기도 수원시",
    region: "경기도 수원시",
    title: "수원 광교신도시 공공분양 (일반공급) 입주자 모집",
    housingType: "아파트",
    supplyType: "공공분양",
    status: "closing",
    thumbnailEmoji: "🏘️",
    deposit: null,
    monthlyRent: null,
    salePrice: 320000000,
    deadlineLabel: "마감 D-2 · 4월 10일",
    urgent: true,
    liked: false,
    tags: [{ label: "공공분양", tone: "default" }],
    badges: ["마감임박", "공공분양", "아파트"],
    recommendedReason: "분양 예산 범위가 맞을 때 볼 만한 옵션입니다."
  },
  {
    id: "bupyeong-happy",
    provider: "LH",
    providerDetail: "LH · 인천광역시 부평구",
    region: "인천광역시 부평구",
    title: "부평구 청천동 행복주택 입주자 모집공고",
    housingType: "아파트",
    supplyType: "행복주택",
    status: "open",
    thumbnailEmoji: "🏙️",
    deposit: 52000000,
    monthlyRent: 230000,
    salePrice: null,
    deadlineLabel: "마감 D-20 · 4월 28일",
    urgent: false,
    liked: false,
    tags: [
      { label: "청년", tone: "blue" },
      { label: "행복주택", tone: "default" }
    ],
    badges: ["모집중", "행복주택", "청년"],
    recommendedReason: "직장 접근성과 보증금 범위가 맞는 후보입니다."
  },
  {
    id: "nowon-closed",
    provider: "LH",
    providerDetail: "LH · 서울특별시 노원구",
    region: "서울특별시 노원구",
    title: "노원구 상계동 국민임대주택 입주자 모집공고",
    housingType: "아파트",
    supplyType: "국민임대",
    status: "closed",
    thumbnailEmoji: "🏛️",
    deposit: 38000000,
    monthlyRent: 175000,
    salePrice: null,
    deadlineLabel: "모집 종료",
    urgent: false,
    liked: false,
    tags: [{ label: "국민임대", tone: "default" }],
    badges: ["모집 종료", "국민임대", "아파트"],
    recommendedReason: "마감된 공고이지만 유사 조건의 대체 공고 비교에 활용할 수 있습니다."
  },
  {
    id: "dongtan-purchase",
    provider: "LH",
    providerDetail: "LH · 경기도 화성시",
    region: "경기도 화성시",
    title: "화성 동탄2 매입임대주택 입주자 모집공고",
    housingType: "다세대주택",
    supplyType: "매입임대",
    status: "open",
    thumbnailEmoji: "🏢",
    deposit: 18000000,
    monthlyRent: 210000,
    salePrice: null,
    deadlineLabel: "마감 D-30 · 5월 8일",
    urgent: false,
    liked: false,
    tags: [
      { label: "매입임대", tone: "default" },
      { label: "고령자", tone: "orange" }
    ],
    badges: ["모집중", "매입임대", "다세대"],
    recommendedReason: "장기 거주와 보증금 부담 완화를 함께 보는 경우 적합합니다."
  },
  {
    id: "pyeongtaek-godeok",
    provider: "LH",
    providerDetail: "LH · 경기도 평택시",
    region: "경기도 평택시",
    title: "평택 고덕신도시 국민임대주택 예비입주자 모집공고",
    housingType: "아파트",
    supplyType: "국민임대",
    status: "open",
    thumbnailEmoji: "🏗️",
    deposit: 92400000,
    monthlyRent: 188000,
    salePrice: null,
    deadlineLabel: "마감 D-12 · 4월 20일",
    urgent: false,
    liked: false,
    tags: [
      { label: "신혼부부", tone: "default" },
      { label: "청년", tone: "blue" }
    ],
    badges: ["모집중", "국민임대", "아파트"],
    recommendedReason: "수도권 남부 근무지와 통근 조건에 잘 맞습니다."
  }
];

export const userProfile = {
  id: "@admin",
  name: "관리자",
  email: "admin@example.com",
  phone: "010-0000-0000",
  age: 29,
  maritalStatus: "미혼",
  householdCount: 0,
  preferredRegion: "경기도",
  preferredDistrict: "오산시",
  preferredHousingType: "아파트",
  preferredSupplyType: "국민임대",
  maxDeposit: 20000000,
  maxRent: 300000,
  categories: ["청년", "무주택자"]
};

export const notifications = [
  {
    id: "n1",
    title: "[공고 알림] 마감 하루 전",
    message: "[정정공고][인천지역본부] 26년 2차 신혼·신생아Ⅰ 예비입주자 모집공고(인천,부천)가 2026-04-08에 마감됩니다.",
    read: false,
    createdAt: "2026년 4월 7일 오후 5:03"
  },
  {
    id: "n2",
    title: "[공고 알림] 새 추천 공고",
    message: "회원님의 조건에 맞는 새로운 공고가 등록되었습니다. 지금 확인해보세요.",
    read: true,
    createdAt: "2026년 4월 6일 오전 9:00"
  },
  {
    id: "n3",
    title: "[공고 알림] 마감 3일 전",
    message: "저장하신 오산세교2 21단지 공고가 2026-04-14에 마감됩니다.",
    read: true,
    createdAt: "2026년 4월 5일 오전 9:00"
  }
];

export const notificationSettings = [
  { id: "deadline3", title: "마감 D-3 알림", desc: "저장한 공고 마감 3일 전 알림", enabled: true },
  { id: "deadline1", title: "마감 D-1 알림", desc: "저장한 공고 마감 하루 전 알림", enabled: true },
  { id: "recommend", title: "새 추천 공고 알림", desc: "내 조건에 맞는 새 공고 등록 시 알림", enabled: false }
];
