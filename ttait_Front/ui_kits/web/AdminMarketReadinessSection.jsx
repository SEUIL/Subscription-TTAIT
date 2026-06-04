// AdminMarketReadinessSection.jsx — 주변시세 readiness / prepare 관리자 목업
// 정적 프로토타입용 파일입니다. import React / export default가 없고 Object.assign(window, ...) 방식으로 동작합니다.
// 실제 앱에 이식할 때는 모듈 컴포넌트로 분리하고 useApi/useToast를 연결한 뒤 prepare 완료 후 readiness를 재조회하세요.

const MARKET_READINESS_SAMPLE = {
  announcementId: 1042,
  sourceType: 'APT_RENT',
  dealYmFrom: '202512',
  dealYmTo: '202605',
  rtmsServiceKeyConfigured: true,
  readyUnitCount: 2,
  blockedUnitCount: 3,
  units: [
    {
      unitId: 8841,
      unitOrder: 1,
      complexName: '강동천호 행복주택 26A',
      fullAddress: '서울특별시 강동구 천호동 458-3 일원',
      legalDongCode: '1174010900',
      lawdCd: '11740',
      addressStatus: 'SUCCESS',
      addressMessage: '천호동 기준으로 법정동 코드 매칭 완료',
      addressNormalizedAt: '2026-06-02T11:20:00',
      exclusiveAreaValue: 26.43,
      recommendedSourceType: 'APT_RENT',
      rawTransactionCount: 42,
      snapshotFound: true,
      snapshotStatus: 'OK',
      marketReady: true,
      blocker: 'READY',
    },
    {
      unitId: 8842,
      unitOrder: 2,
      complexName: '강동천호 행복주택 36B',
      fullAddress: '서울특별시 강동구 천호동 458-3 일원',
      legalDongCode: '1174010900',
      lawdCd: '11740',
      addressStatus: 'SUCCESS',
      addressMessage: '법정동 코드 매칭 완료',
      addressNormalizedAt: '2026-06-02T11:20:00',
      exclusiveAreaValue: 36.82,
      recommendedSourceType: 'APT_RENT',
      rawTransactionCount: 0,
      snapshotFound: false,
      snapshotStatus: null,
      marketReady: false,
      blocker: 'SNAPSHOT_NOT_FOUND',
    },
    {
      unitId: 8843,
      unitOrder: 3,
      complexName: '강동천호 행복주택 44A',
      fullAddress: '서울특별시 강동구 천호동 458-3 일원',
      legalDongCode: '1174010900',
      lawdCd: '11740',
      addressStatus: 'SUCCESS',
      addressMessage: '법정동 코드 매칭 완료',
      addressNormalizedAt: '2026-06-02T11:20:00',
      exclusiveAreaValue: 44.12,
      recommendedSourceType: 'APT_RENT',
      rawTransactionCount: 2,
      snapshotFound: true,
      snapshotStatus: 'INSUFFICIENT_DATA',
      marketReady: false,
      blocker: 'INSUFFICIENT_DATA',
    },
    {
      unitId: 8844,
      unitOrder: 4,
      complexName: '미정 단지',
      fullAddress: '서울 강동구 일대',
      legalDongCode: null,
      lawdCd: null,
      addressStatus: 'NO_LAWD_CODE',
      addressMessage: '상세 주소 부족으로 법정동 코드 매칭 실패',
      addressNormalizedAt: null,
      exclusiveAreaValue: 29.91,
      recommendedSourceType: 'APT_RENT',
      rawTransactionCount: 0,
      snapshotFound: false,
      snapshotStatus: null,
      marketReady: false,
      blocker: 'UNIT_LAWD_CD_MISSING',
    },
    {
      unitId: 8845,
      unitOrder: 5,
      complexName: '강동천호 행복주택 미상형',
      fullAddress: '서울특별시 강동구 천호동 458-3 일원',
      legalDongCode: '1174010900',
      lawdCd: '11740',
      addressStatus: 'SUCCESS',
      addressMessage: '법정동 코드 매칭 완료',
      addressNormalizedAt: '2026-06-02T11:20:00',
      exclusiveAreaValue: null,
      recommendedSourceType: 'APT_RENT',
      rawTransactionCount: 0,
      snapshotFound: false,
      snapshotStatus: null,
      marketReady: false,
      blocker: 'UNIT_AREA_MISSING',
    },
  ],
};

const PREPARE_SAMPLE_BODY = {
  sourceType: 'APT_RENT',
  dealYm: '202605',
  dealYmFrom: '202512',
  dealYmTo: '202605',
  numOfRows: 100,
  maxPages: 10,
  minimumSampleCount: 3,
  retryNoLawdCode: true,
};

const BLOCKER_META = {
  READY: {
    tone: 'ready',
    badge: '준비 완료',
    title: 'admin readiness 준비 완료',
    helper: 'admin readiness의 blocker=READY 상태입니다. public 비교 응답에서는 status=COMPARABLE로 매핑합니다.',
    cta: 'public COMPARABLE 확인',
  },
  SNAPSHOT_NOT_FOUND: {
    tone: 'empty',
    badge: '데이터 없음',
    title: '시세 데이터 준비 필요',
    helper: '최근 6개월 거래 데이터를 수집해 비교 기준을 생성합니다.',
    cta: '시세 데이터 준비',
  },
  INSUFFICIENT_DATA: {
    tone: 'warning',
    badge: '표본 부족',
    title: '비교 가능한 거래 표본 부족',
    helper: '주변 거래 표본이 부족해 신뢰도 있는 비교가 어렵습니다.',
    cta: '시세비교 비활성',
  },
  UNIT_LAWD_CD_MISSING: {
    tone: 'blocked',
    badge: '정보 부족',
    title: '주소 정보 보완 필요',
    helper: '주소 정규화 또는 법정동 코드(lawdCd) 매칭이 필요합니다.',
    cta: '주소 보완 필요',
  },
  UNIT_AREA_MISSING: {
    tone: 'blocked',
    badge: '정보 부족',
    title: '면적 정보 없음',
    helper: '전용면적(exclusiveAreaValue)이 없어 주변 거래 면적 범위를 만들 수 없습니다.',
    cta: '면적 보완 필요',
  },
  API_KEY_MISSING: {
    tone: 'danger',
    badge: '설정 필요',
    title: 'RTMS 서비스키 미설정',
    helper: '서비스키가 설정되지 않으면 prepare 실행을 막습니다.',
    cta: 'prepare 비활성',
  },
  RUNNING: {
    tone: 'loading',
    badge: '준비 중',
    title: '데이터 수집 및 집계 중',
    helper: '동기 API 응답이 끝날 때까지 버튼을 disabled 처리합니다.',
    cta: '수집 중…',
  },
};

const PUBLIC_COMPARISON_META = {
  COMPARABLE: {
    tone: 'ready',
    badge: '준비 완료',
    title: 'public status=COMPARABLE',
    helper: '일반 상세 화면에서 시세비교 그래프와 CTA를 활성화합니다.',
    cta: '시세비교 보기 →',
  },
  INSUFFICIENT_DATA: BLOCKER_META.INSUFFICIENT_DATA,
  SNAPSHOT_NOT_FOUND: {
    ...BLOCKER_META.SNAPSHOT_NOT_FOUND,
    cta: '관리자 준비 필요',
  },
  UNIT_LAWD_CD_MISSING: BLOCKER_META.UNIT_LAWD_CD_MISSING,
  UNIT_AREA_MISSING: BLOCKER_META.UNIT_AREA_MISSING,
};

const SOURCE_LABELS = {
  APT_RENT: '아파트 전월세',
  OFFICETEL_RENT: '오피스텔 전월세',
  ROW_HOUSE_RENT: '연립/다세대 전월세',
};

const MARKET_READINESS_STYLE = `
.market-page{min-height:100vh;background:#f2f2f2;color:#222;font-family:'Noto Sans KR',-apple-system,system-ui,sans-serif}
.market-shell{max-width:1180px;margin:0 auto;padding:32px 24px 80px}.market-stack{display:grid;gap:20px}
.market-panel{background:#fff;border-radius:22px;box-shadow:rgba(0,0,0,.02) 0 0 0 1px,rgba(0,0,0,.04) 0 2px 6px,rgba(0,0,0,.1) 0 4px 8px;overflow:hidden}
.market-head{padding:24px;border-bottom:1px solid rgba(0,0,0,.06);display:flex;justify-content:space-between;gap:18px;align-items:flex-start}.market-pad{padding:24px}
.market-eyebrow{color:#ff385c;font-size:12px;font-weight:900;letter-spacing:.2px;margin-bottom:6px}.market-title{margin:0 0 8px;font-size:24px;line-height:1.35;letter-spacing:-.4px}.market-desc{margin:0;color:#6a6a6a;font-size:14px;line-height:1.65}
.market-actions{display:flex;gap:8px;justify-content:flex-end;flex-wrap:wrap}.market-btn{height:40px;border:none;border-radius:10px;padding:0 15px;background:#222;color:#fff;font-size:13px;font-weight:900;cursor:pointer;transition:.15s ease;display:inline-flex;gap:8px;align-items:center;justify-content:center}.market-btn:hover{transform:translateY(-1px);box-shadow:rgba(0,0,0,.08) 0 4px 12px}.market-btn.brand{background:#ff385c}.market-btn.secondary{background:#f2f2f2;color:#222}.market-btn:disabled{opacity:.5;cursor:not-allowed;transform:none;box-shadow:none}.market-btn.loading::before{content:'';width:13px;height:13px;border-radius:50%;border:2px solid rgba(255,255,255,.45);border-top-color:#fff;animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
.market-badge{display:inline-flex;align-items:center;min-height:24px;border-radius:999px;padding:4px 10px;border:1px solid transparent;font-size:11px;font-weight:900;line-height:1.2;white-space:nowrap}.market-badge.ready{color:#166534;background:#f0fdf4;border-color:rgba(22,101,52,.16)}.market-badge.empty{color:#6a6a6a;background:#f2f2f2;border-color:rgba(0,0,0,.06)}.market-badge.warning{color:#c2410c;background:#fff7ed;border-color:rgba(194,65,12,.18)}.market-badge.blocked{color:#7e22ce;background:#fdf4ff;border-color:rgba(126,34,206,.16)}.market-badge.danger{color:#e00b41;background:#fff0f3;border-color:rgba(224,11,65,.18)}.market-badge.loading{color:#1d4ed8;background:#eff6ff;border-color:rgba(29,78,216,.16)}
.market-query{display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;padding:16px 24px;border-bottom:1px solid rgba(0,0,0,.06);background:#fafafa}.market-chip{display:inline-flex;align-items:center;gap:6px;height:34px;padding:0 12px;border-radius:999px;background:#fff;border:1px solid rgba(0,0,0,.08);font-size:12px;font-weight:800}.market-chip strong{color:#ff385c}.market-alert{margin:18px 24px 0;border-radius:16px;padding:14px 16px;background:#fff0f3;color:#e00b41;border:1px solid rgba(224,11,65,.18);font-size:13px;font-weight:800;line-height:1.6}.market-alert.green{background:#f0fdf4;color:#166534;border-color:rgba(22,101,52,.16)}
.market-stats{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.market-stat{border:1px solid rgba(0,0,0,.07);border-radius:18px;padding:16px;background:#fff}.market-stat.primary{color:#fff;background:linear-gradient(135deg,#ff385c,#e00b41);border-color:transparent}.market-stat-label{font-size:11px;color:#6a6a6a;font-weight:900}.market-stat.primary .market-stat-label,.market-stat.primary .market-stat-note{color:rgba(255,255,255,.76)}.market-stat-value{margin-top:8px;font-size:28px;line-height:1;font-weight:900}.market-stat-note{margin-top:8px;color:#6a6a6a;font-size:12px;line-height:1.45}
.market-table-wrap{overflow-x:auto}.market-table{width:100%;min-width:1100px;border-collapse:collapse}.market-table th{padding:12px 14px;text-align:left;background:#fafafa;border-bottom:1px solid rgba(0,0,0,.08);color:#6a6a6a;font-size:12px;font-weight:900}.market-table td{padding:15px 14px;border-bottom:1px solid rgba(0,0,0,.06);vertical-align:top;font-size:13px;line-height:1.45}.market-table tr:last-child td{border-bottom:0}.market-name{font-weight:900;margin-bottom:4px}.market-muted{color:#6a6a6a;font-size:12px;line-height:1.45}.market-code{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;font-size:12px;font-weight:800}.market-strong{font-weight:900}
.market-row-action{height:34px;border:none;border-radius:9px;padding:0 12px;background:#222;color:#fff;font-size:12px;font-weight:900;cursor:pointer;white-space:nowrap}.market-row-action.prepare{background:#ff385c}.market-row-action.disabled{background:#f2f2f2;color:#6a6a6a;cursor:not-allowed}.market-row-action.secondary{background:#fff;color:#ff385c;border:1px solid rgba(255,56,92,.28)}
.market-card-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.market-state-card{position:relative;border:1px solid rgba(0,0,0,.08);border-radius:20px;background:#fff;padding:18px;min-height:188px;overflow:hidden}.market-state-card::after{content:'';position:absolute;right:-26px;top:-26px;width:86px;height:86px;border-radius:50%;opacity:.7}.market-state-card.ready::after{background:#f0fdf4}.market-state-card.empty::after{background:#f2f2f2}.market-state-card.warning::after{background:#fff7ed}.market-state-card.blocked::after{background:#fdf4ff}.market-state-card.danger::after{background:#fff0f3}.market-state-card.loading::after{background:#eff6ff}.market-state-title{position:relative;margin:12px 0 8px;font-size:17px;font-weight:900}.market-state-copy{position:relative;margin:0;color:#6a6a6a;font-size:13px;line-height:1.6}.market-state-cta{position:relative;margin-top:14px;display:inline-flex;height:34px;align-items:center;padding:0 12px;border-radius:9px;background:#222;color:#fff;font-size:12px;font-weight:900}.market-state-cta.ready{background:#ff385c}.market-state-cta.disabled{background:#f2f2f2;color:#6a6a6a}.market-state-cta.loading{background:#1d4ed8;color:#fff}
.market-two-col{display:grid;grid-template-columns:minmax(0,1fr) 360px;gap:20px}.market-json{margin:0;border-radius:16px;background:#222;color:#fff;padding:16px;overflow:auto;font-size:12px;line-height:1.7}.market-result{border-radius:16px;border:1px solid rgba(0,0,0,.08);padding:16px;background:#fafafa}.market-result-row{display:flex;justify-content:space-between;gap:12px;padding:10px 0;border-bottom:1px solid rgba(0,0,0,.06);font-size:13px}.market-result-row:last-child{border-bottom:0}
.market-mobile-list{display:none}.market-mobile-card{border:1px solid rgba(0,0,0,.08);border-radius:18px;background:#fff;padding:16px}.market-mobile-top{display:flex;justify-content:space-between;gap:12px;align-items:flex-start;margin-bottom:10px}
@media(max-width:980px){.market-two-col{grid-template-columns:1fr}.market-card-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.market-stats{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:720px){.market-shell{padding:20px 16px 56px}.market-head{flex-direction:column;padding:20px}.market-actions{justify-content:flex-start}.market-query{padding:14px 20px}.market-stats,.market-card-grid{grid-template-columns:1fr}.market-table-wrap{display:none}.market-mobile-list{display:grid;gap:12px;padding:0 20px 20px}.market-title{font-size:21px}}
`;

const MarketBadge = ({ tone, children }) => <span className={`market-badge ${tone}`}>{children}</span>;
const getBlockerKey = (unit) => unit?.marketReady ? 'READY' : (unit?.blocker || (unit?.snapshotStatus === 'INSUFFICIENT_DATA' ? 'INSUFFICIENT_DATA' : 'SNAPSHOT_NOT_FOUND'));
const getUnitMeta = (unit) => BLOCKER_META[getBlockerKey(unit)] || BLOCKER_META.SNAPSHOT_NOT_FOUND;
const formatArea = (value) => value != null ? `${value}㎡` : '-';
const formatDateTime = (value) => value ? String(value).slice(0, 16).replace('T', ' ') : '-';

const isPrepareEligibleUnit = (unit, allowInsufficientDataReprepare = false) => {
  const key = getBlockerKey(unit);
  const hasRequiredBaseData = Boolean(unit?.lawdCd) && unit?.exclusiveAreaValue != null;
  return hasRequiredBaseData && !unit?.marketReady && (
    key === 'SNAPSHOT_NOT_FOUND' ||
    (allowInsufficientDataReprepare && key === 'INSUFFICIENT_DATA')
  );
};

const getRowAction = (unit, allowInsufficientDataReprepare = false) => {
  const key = getBlockerKey(unit);
  if (key === 'READY') return { label: '시세비교 보기', className: 'secondary', disabled: false };
  if (isPrepareEligibleUnit(unit, allowInsufficientDataReprepare)) return { label: key === 'INSUFFICIENT_DATA' ? '표본 재수집' : '시세 데이터 준비', className: 'prepare', disabled: false };
  if (key === 'INSUFFICIENT_DATA') return { label: '재수집 정책 확인', className: 'disabled', disabled: true };
  if (key === 'UNIT_LAWD_CD_MISSING') return { label: '주소 보완 필요', className: 'disabled', disabled: true };
  if (key === 'UNIT_AREA_MISSING') return { label: '면적 보완 필요', className: 'disabled', disabled: true };
  return { label: '준비 불가', className: 'disabled', disabled: true };
};

const ReadinessStats = ({ readiness }) => (
  <div className="market-stats">
    <div className="market-stat primary"><div className="market-stat-label">준비 완료 unit</div><div className="market-stat-value">{readiness.readyUnitCount}</div><div className="market-stat-note">marketReady=true</div></div>
    <div className="market-stat"><div className="market-stat-label">차단 unit</div><div className="market-stat-value">{readiness.blockedUnitCount}</div><div className="market-stat-note">blocker 사유 표시</div></div>
    <div className="market-stat"><div className="market-stat-label">조회 대상</div><div className="market-stat-value">{readiness.units?.length || 0}</div><div className="market-stat-note">공고 units[] 기준</div></div>
    <div className="market-stat"><div className="market-stat-label">RTMS key</div><div className="market-stat-value">{readiness.rtmsServiceKeyConfigured ? 'ON' : 'OFF'}</div><div className="market-stat-note">false면 prepare disabled</div></div>
  </div>
);

const ReadinessUnitTable = ({ readiness, running, allowInsufficientDataReprepare }) => {
  const units = readiness.units || [];

  return (
    <>
      <div className="market-table-wrap">
        <table className="market-table">
          <thead>
            <tr>
              <th>Unit</th><th>상태</th><th>단지/주소</th><th>주소 정규화</th><th>면적</th><th>Raw / Snapshot</th><th>UI 분기</th><th>행 액션</th>
            </tr>
          </thead>
          <tbody>
            {units.map((unit) => {
              const meta = running && unit.blocker === 'SNAPSHOT_NOT_FOUND' ? BLOCKER_META.RUNNING : getUnitMeta(unit);
              const action = getRowAction(unit, allowInsufficientDataReprepare);
              return (
                <tr key={unit.unitId}>
                  <td><div className="market-strong">#{unit.unitOrder}</div><div className="market-muted">unitId {unit.unitId}</div></td>
                  <td><MarketBadge tone={meta.tone}>{meta.badge}</MarketBadge><div className="market-muted" style={{ marginTop: 6 }}>{getBlockerKey(unit)}</div></td>
                  <td><div className="market-name">{unit.complexName}</div><div className="market-muted">{unit.fullAddress}</div></td>
                  <td>
                    <div className="market-code">lawdCd {unit.lawdCd || '-'}</div>
                    <div className="market-muted">legalDongCode {unit.legalDongCode || '-'}</div>
                    <div className="market-muted">{unit.addressStatus || '-'} · {formatDateTime(unit.addressNormalizedAt)}</div>
                    <div className="market-muted">recommended {unit.recommendedSourceType || '-'}</div>
                  </td>
                  <td><span className="market-strong">{formatArea(unit.exclusiveAreaValue)}</span></td>
                  <td><div>raw <b>{unit.rawTransactionCount}</b>건</div><div className="market-muted">snapshot {unit.snapshotFound ? 'found' : 'not found'} · {unit.snapshotStatus || '-'}</div></td>
                  <td><div className="market-strong">{meta.title}</div><div className="market-muted">{meta.helper}</div></td>
                  <td><button className={`market-row-action ${action.className}`} type="button" disabled={action.disabled || running}>{action.label}</button></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div className="market-mobile-list">
        {units.map((unit) => {
          const meta = running && unit.blocker === 'SNAPSHOT_NOT_FOUND' ? BLOCKER_META.RUNNING : getUnitMeta(unit);
          const action = getRowAction(unit, allowInsufficientDataReprepare);
          return (
            <article className="market-mobile-card" key={unit.unitId}>
              <div className="market-mobile-top">
                <div><div className="market-name">#{unit.unitOrder} {unit.complexName}</div><div className="market-muted">{unit.fullAddress}</div></div>
                <MarketBadge tone={meta.tone}>{meta.badge}</MarketBadge>
              </div>
              <div className="market-muted">lawdCd {unit.lawdCd || '-'} · 전용 {formatArea(unit.exclusiveAreaValue)} · raw {unit.rawTransactionCount}건</div>
              <div className="market-strong" style={{ marginTop: 10 }}>{meta.title}</div>
              <div className="market-muted">{meta.helper}</div>
              <button className={`market-row-action ${action.className}`} type="button" disabled={action.disabled || running} style={{ marginTop: 12 }}>{action.label}</button>
            </article>
          );
        })}
      </div>
    </>
  );
};

const AdminMarketReadinessSection = ({
  readiness = MARKET_READINESS_SAMPLE,
  onPrepare,
  onRefresh,
  allowInsufficientDataReprepare = false,
}) => {
  const [running, setRunning] = React.useState(false);
  const [refreshing, setRefreshing] = React.useState(false);
  const [resultStatus, setResultStatus] = React.useState('');
  const prepareTargets = (readiness.units || []).filter((unit) => isPrepareEligibleUnit(unit, allowInsufficientDataReprepare));
  const hasPrepareTarget = prepareTargets.length > 0;
  const prepareDisabled = running || !readiness.rtmsServiceKeyConfigured || !hasPrepareTarget;

  const handleRefresh = async () => {
    if (!onRefresh || refreshing || running) return;
    setRefreshing(true);
    try {
      await Promise.resolve(onRefresh());
    } finally {
      setRefreshing(false);
    }
  };

  const handlePrepare = async () => {
    if (prepareDisabled) return;
    setRunning(true);
    setResultStatus('');
    try {
      let response;
      if (onPrepare) {
        response = await Promise.resolve(onPrepare(PREPARE_SAMPLE_BODY, prepareTargets));
      } else {
        await new Promise((resolve) => window.setTimeout(resolve, 1300));
        response = { status: 'PARTIAL_SUCCESS' };
      }
      setResultStatus(response?.status || '요청 완료');
    } catch {
      setResultStatus('실행 실패');
    } finally {
      setRunning(false);
    }
  };

  return (
    <section className="market-panel">
      <div className="market-head">
        <div>
          <div className="market-eyebrow">ADMIN MARKET READINESS</div>
          <h2 className="market-title">주변시세 준비 상태</h2>
          <p className="market-desc">프론트는 readiness 상태를 보고 UI를 분기하고, 관리자에서 필요한 경우 prepare를 호출합니다.</p>
        </div>
        <div className="market-actions">
          <button className="market-btn secondary" type="button" onClick={handleRefresh} disabled={!onRefresh || refreshing || running}>
            {refreshing ? '새로고침 중' : 'readiness 새로고침'}
          </button>
          <button className={`market-btn brand ${running ? 'loading' : ''}`} type="button" onClick={handlePrepare} disabled={prepareDisabled}>
            {running ? '데이터 수집 및 집계 중' : `시세 데이터 준비${prepareTargets.length ? ` ${prepareTargets.length}건` : ''}`}
          </button>
        </div>
      </div>

      <div className="market-query">
        <span className="market-chip">sourceType <strong>{readiness.sourceType}</strong> · {SOURCE_LABELS[readiness.sourceType] || '아파트 전월세'}</span>
        <span className="market-chip">조회기간 <strong>{readiness.dealYmFrom}~{readiness.dealYmTo}</strong></span>
        <span className="market-chip">기본안 <strong>최근 완료 6개월</strong></span>
        <span className="market-chip">INSUFFICIENT_DATA 재실행 <strong>{allowInsufficientDataReprepare ? '허용' : '정책 확인 필요'}</strong></span>
      </div>

      {!readiness.rtmsServiceKeyConfigured && <div className="market-alert">RTMS 서비스키 미설정 상태입니다. 관리자/debug 영역에서 alert를 노출하고 prepare 버튼은 비활성화합니다.</div>}
      {resultStatus && <div className="market-alert green">prepare 결과: {resultStatus}. 완료 후 readiness를 다시 조회해 최신 상태로 갱신하세요.</div>}

      <div className="market-pad"><ReadinessStats readiness={readiness} /></div>
      <ReadinessUnitTable readiness={readiness} running={running} allowInsufficientDataReprepare={allowInsufficientDataReprepare} />
    </section>
  );
};

const MarketStateGallery = () => {
  const adminStates = ['READY', 'SNAPSHOT_NOT_FOUND', 'RUNNING', 'INSUFFICIENT_DATA', 'UNIT_LAWD_CD_MISSING', 'UNIT_AREA_MISSING', 'API_KEY_MISSING'];
  const publicStates = ['COMPARABLE', 'INSUFFICIENT_DATA', 'SNAPSHOT_NOT_FOUND', 'UNIT_LAWD_CD_MISSING', 'UNIT_AREA_MISSING'];
  return (
    <section className="market-panel">
      <div className="market-head">
        <div>
          <div className="market-eyebrow">ADMIN READINESS vs PUBLIC COMPARISON</div>
          <h2 className="market-title">admin READY와 public COMPARABLE을 분리한 상태 카드</h2>
          <p className="market-desc">관리자 readiness는 blocker=READY, 일반 상세 시세비교 응답은 status=COMPARABLE을 사용합니다. 일반 사용자 카드에는 prepare CTA를 노출하지 않습니다.</p>
        </div>
      </div>
      <div className="market-pad">
        <div className="market-eyebrow">ADMIN READINESS</div>
        <div className="market-card-grid">
          {adminStates.map((key) => {
            const meta = BLOCKER_META[key];
            const disabled = ['INSUFFICIENT_DATA', 'UNIT_LAWD_CD_MISSING', 'UNIT_AREA_MISSING', 'API_KEY_MISSING'].includes(key);
            return (
              <article className={`market-state-card ${meta.tone}`} key={`admin-${key}`}>
                <MarketBadge tone={meta.tone}>{meta.badge}</MarketBadge>
                <h3 className="market-state-title">{meta.title}</h3>
                <p className="market-state-copy">{meta.helper}</p>
                <span className={`market-state-cta ${key === 'SNAPSHOT_NOT_FOUND' ? 'ready' : ''} ${disabled ? 'disabled' : ''} ${key === 'RUNNING' ? 'loading' : ''}`}>{meta.cta}</span>
              </article>
            );
          })}
        </div>

        <div className="market-eyebrow" style={{ marginTop: 24 }}>PUBLIC DETAIL</div>
        <div className="market-card-grid">
          {publicStates.map((key) => {
            const meta = PUBLIC_COMPARISON_META[key];
            const disabled = key !== 'COMPARABLE';
            return (
              <article className={`market-state-card ${meta.tone}`} key={`public-${key}`}>
                <MarketBadge tone={meta.tone}>{meta.badge}</MarketBadge>
                <h3 className="market-state-title">{meta.title}</h3>
                <p className="market-state-copy">{meta.helper}</p>
                <span className={`market-state-cta ${key === 'COMPARABLE' ? 'ready' : 'disabled'}`}>{disabled ? '시세비교 비활성' : meta.cta}</span>
              </article>
            );
          })}
        </div>
      </div>
    </section>
  );
};

const PreparePanel = ({ resultStatus = '' }) => (
  <section className="market-panel">
    <div className="market-head">
      <div>
        <div className="market-eyebrow">PREPARE REQUEST</div>
        <h2 className="market-title">시세 데이터 준비 실행 패널</h2>
        <p className="market-desc">동기 API이므로 실행 중 문구와 disabled 상태를 같은 컴포넌트에서 관리합니다.</p>
      </div>
      <MarketBadge tone={resultStatus ? 'ready' : 'empty'}>{resultStatus || '대기'}</MarketBadge>
    </div>
    <div className="market-pad market-two-col">
      <pre className="market-json">{JSON.stringify(PREPARE_SAMPLE_BODY, null, 2)}</pre>
      <aside className="market-result">
        <div className="market-result-row"><span>성공 상태</span><b>SUCCESS / PARTIAL_SUCCESS / NO_ELIGIBLE_UNITS</b></div>
        <div className="market-result-row"><span>unit 상태</span><b>QUEUED / SKIPPED</b></div>
        <div className="market-result-row"><span>blocker</span><b>UNIT_LAWD_CD_MISSING / UNIT_AREA_MISSING</b></div>
        <div className="market-result-row"><span>활성 조건</span><b>lawdCd + exclusiveAreaValue 보유 unit</b></div>
        <div className="market-result-row"><span>표본 부족 재실행</span><b>정책 확정 필요</b></div>
        <div className="market-result-row"><span>완료 후</span><b>readiness 재조회</b></div>
      </aside>
    </div>
  </section>
);

const HandoffPanel = () => (
  <section className="market-panel">
    <div className="market-head">
      <div>
        <div className="market-eyebrow">FRONT HANDOFF</div>
        <h2 className="market-title">프론트팀 추가 전달사항</h2>
        <p className="market-desc">기본 기간, 버튼 노출 범위, disabled 정책은 구현 전 결정이 필요합니다.</p>
      </div>
    </div>
    <div className="market-pad">
      <div className="market-card-grid">
        {[
          ['기본 조회 기간', '권장안은 최근 완료 6개월입니다. 2026-06-02 기준 예시는 202512~202605입니다.'],
          ['기본 sourceType', '현재 디자인은 APT_RENT 기준입니다.'],
          ['prepare 노출 범위', '관리자 전용으로 가정했습니다. 일반 사용자는 준비/부족 상태만 안내합니다.'],
          ['marketReady=false unit', '숨기지 않고 비활성 카드로 보여주는 안을 권장합니다.'],
          ['INSUFFICIENT_DATA', '기존 청약가/임대료는 보여주고 시세비교만 disabled 처리합니다.'],
          ['RTMS key 미설정', 'rtmsServiceKeyConfigured=false이면 alert와 prepare disabled가 필요합니다.'],
          ['static prototype 주의', '이 파일은 window 등록 방식입니다. 실제 앱 import용으로는 모듈화가 필요합니다.'],
          ['READY/COMPARABLE 분리', 'admin readiness는 READY, public comparison은 COMPARABLE로 표기합니다.'],
        ].map(([title, copy]) => (
          <article className="market-state-card empty" key={title}>
            <MarketBadge tone="empty">결정 필요</MarketBadge>
            <h3 className="market-state-title">{title}</h3>
            <p className="market-state-copy">{copy}</p>
          </article>
        ))}
      </div>
    </div>
  </section>
);

const AdminMarketReadinessPage = () => (
  <div className="market-page">
    <style>{MARKET_READINESS_STYLE}</style>
    <main className="market-shell market-stack">
      <div style={{ marginBottom: 4 }}>
        <div className="market-eyebrow">HANDOFF MOCKUP</div>
        <h1 className="market-title" style={{ fontSize: 30 }}>주변시세 readiness / prepare 디자인</h1>
        <p className="market-desc">관리자 준비 상태, public 시세비교 상태, prepare 실행 패턴을 프론트 이식용으로 정리한 목업입니다.</p>
      </div>
      <AdminMarketReadinessSection />
      <MarketStateGallery />
      <PreparePanel />
      <HandoffPanel />
    </main>
  </div>
);

Object.assign(window, {
  AdminMarketReadinessSection,
  AdminMarketReadinessPage,
  MarketStateGallery,
});
