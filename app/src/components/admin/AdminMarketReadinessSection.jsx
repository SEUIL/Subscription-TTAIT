const COLORS = {
  brand: '#ff385c',
  brandDark: '#e00b41',
  nearBlack: '#222',
  secondary: '#6a6a6a',
  border: '#c1c1c1',
  surface: '#f2f2f2',
  soft: '#fafafa',
  white: '#fff',
  redTint: '#fff0f3',
  blueTint: '#eff6ff',
  greenTint: '#f0fdf4',
  orangeTint: '#fff7ed',
  purpleTint: '#fdf4ff',
  blue: '#1d4ed8',
  green: '#166534',
  orange: '#c2410c',
  purple: '#7e22ce',
};

const SOURCE_TYPE_LABELS = {
  APT_RENT: '아파트 전월세',
  APT_TRADE: '아파트 매매',
  ROW_HOUSE_RENT: '연립/다세대 전월세',
  ROW_HOUSE_TRADE: '연립/다세대 매매',
  OFFICETEL_RENT: '오피스텔 전월세',
  OFFICETEL_TRADE: '오피스텔 매매',
};

const STATUS_META = {
  READY: {
    label: 'READY',
    badge: '준비 완료',
    tone: 'success',
    title: 'admin READY',
    helper: 'marketReady=true인 관리자 준비 완료 상태입니다.',
  },
  SNAPSHOT_NOT_FOUND: {
    label: 'SNAPSHOT_NOT_FOUND',
    badge: '준비 필요',
    tone: 'neutral',
    title: '시세 snapshot 없음',
    helper: 'lawdCd와 전용면적 기준으로 RTMS 수집 및 snapshot 집계가 필요합니다.',
  },
  INSUFFICIENT_DATA: {
    label: 'INSUFFICIENT_DATA',
    badge: '표본 부족',
    tone: 'warning',
    title: '거래 표본 부족',
    helper: 'snapshot은 있으나 최소 표본 수를 채우지 못했습니다.',
  },
  UNIT_LAWD_CD_MISSING: {
    label: 'UNIT_LAWD_CD_MISSING',
    badge: '주소 보완',
    tone: 'blocked',
    title: 'lawdCd 없음',
    helper: '주소 정규화 또는 법정동 코드 매칭이 먼저 필요합니다.',
  },
  UNIT_AREA_MISSING: {
    label: 'UNIT_AREA_MISSING',
    badge: '면적 보완',
    tone: 'blocked',
    title: '전용면적 없음',
    helper: 'exclusiveAreaValue가 없어 주변 거래 면적 범위를 만들 수 없습니다.',
  },
  API_KEY_MISSING: {
    label: 'API_KEY_MISSING',
    badge: '설정 필요',
    tone: 'danger',
    title: 'RTMS 서비스키 미설정',
    helper: '서비스키가 설정되기 전까지 prepare 실행을 막습니다.',
  },
  UNKNOWN: {
    label: 'UNKNOWN',
    badge: '상태 확인',
    tone: 'neutral',
    title: 'blocker 확인 필요',
    helper: 'marketReady=false이지만 blocker 값이 비어 있습니다.',
  },
};

const TONE_STYLES = {
  success: { bg: COLORS.greenTint, color: COLORS.green, border: 'rgba(22,101,52,0.16)' },
  neutral: { bg: COLORS.surface, color: COLORS.secondary, border: 'rgba(0,0,0,0.06)' },
  warning: { bg: COLORS.orangeTint, color: COLORS.orange, border: 'rgba(194,65,12,0.18)' },
  blocked: { bg: COLORS.purpleTint, color: COLORS.purple, border: 'rgba(126,34,206,0.16)' },
  danger: { bg: COLORS.redTint, color: COLORS.brandDark, border: 'rgba(224,11,65,0.18)' },
  info: { bg: COLORS.blueTint, color: COLORS.blue, border: 'rgba(29,78,216,0.16)' },
};

const S = {
  panel: {
    background: COLORS.white,
    borderRadius: 20,
    marginBottom: 24,
    boxShadow: 'var(--shadow-card)',
    overflow: 'hidden',
    color: COLORS.nearBlack,
    fontFamily: 'var(--font)',
  },
  head: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 20,
    padding: 24,
    borderBottom: '1px solid rgba(0,0,0,0.08)',
    flexWrap: 'wrap',
  },
  eyebrow: {
    color: COLORS.brand,
    fontSize: 12,
    fontWeight: 800,
    letterSpacing: 0.2,
    marginBottom: 6,
  },
  title: {
    fontSize: 20,
    lineHeight: 1.4,
    fontWeight: 700,
    color: COLORS.nearBlack,
    margin: '0 0 8px',
    letterSpacing: '-0.3px',
  },
  desc: {
    fontSize: 14,
    lineHeight: 1.7,
    color: COLORS.secondary,
    margin: 0,
    maxWidth: 680,
  },
  actions: {
    display: 'flex',
    alignItems: 'flex-start',
    justifyContent: 'flex-end',
    gap: 10,
    flexWrap: 'wrap',
  },
  actionGroup: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-end',
    gap: 6,
  },
  button: {
    minHeight: 40,
    border: 'none',
    borderRadius: 10,
    padding: '0 16px',
    fontSize: 13,
    fontWeight: 800,
    cursor: 'pointer',
    transition: 'background 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease',
    whiteSpace: 'nowrap',
  },
  reason: {
    color: COLORS.secondary,
    fontSize: 12,
    lineHeight: 1.5,
    maxWidth: 260,
    textAlign: 'right',
  },
  queryBar: {
    display: 'flex',
    gap: 8,
    flexWrap: 'wrap',
    padding: '16px 24px',
    background: COLORS.soft,
    borderBottom: '1px solid rgba(0,0,0,0.06)',
  },
  chip: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 6,
    minHeight: 32,
    borderRadius: 999,
    padding: '6px 12px',
    background: COLORS.white,
    border: '1px solid rgba(0,0,0,0.08)',
    color: COLORS.secondary,
    fontSize: 12,
    fontWeight: 700,
  },
  chipStrong: {
    color: COLORS.brand,
    fontWeight: 800,
  },
  content: {
    padding: 24,
  },
  alert: {
    margin: '18px 24px 0',
    borderRadius: 16,
    padding: '14px 16px',
    border: '1px solid rgba(0,0,0,0.08)',
    fontSize: 13,
    fontWeight: 700,
    lineHeight: 1.6,
  },
  kpiGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
    gap: 12,
  },
  kpiCard: {
    borderRadius: 16,
    padding: 16,
    border: '1px solid rgba(0,0,0,0.06)',
    background: COLORS.white,
  },
  kpiPrimary: {
    background: COLORS.brand,
    borderColor: COLORS.brand,
    color: COLORS.white,
  },
  kpiLabel: {
    fontSize: 12,
    fontWeight: 800,
    color: COLORS.secondary,
    marginBottom: 8,
  },
  kpiValue: {
    fontSize: 28,
    fontWeight: 800,
    lineHeight: 1,
    color: COLORS.nearBlack,
  },
  kpiNote: {
    fontSize: 12,
    lineHeight: 1.5,
    color: COLORS.secondary,
    marginTop: 8,
  },
  tableWrap: {
    overflowX: 'auto',
    borderTop: '1px solid rgba(0,0,0,0.06)',
  },
  table: {
    width: '100%',
    minWidth: 1120,
    borderCollapse: 'collapse',
  },
  th: {
    textAlign: 'left',
    padding: '12px 14px',
    background: COLORS.soft,
    borderBottom: '1px solid rgba(0,0,0,0.08)',
    color: COLORS.secondary,
    fontSize: 12,
    fontWeight: 800,
    whiteSpace: 'nowrap',
  },
  td: {
    padding: '15px 14px',
    borderBottom: '1px solid rgba(0,0,0,0.06)',
    verticalAlign: 'top',
    fontSize: 13,
    lineHeight: 1.5,
    color: COLORS.nearBlack,
  },
  disabledRow: {
    background: COLORS.soft,
  },
  muted: {
    color: COLORS.secondary,
    fontSize: 12,
    lineHeight: 1.5,
  },
  strong: {
    color: COLORS.nearBlack,
    fontSize: 13,
    fontWeight: 800,
    lineHeight: 1.5,
  },
  code: {
    color: COLORS.nearBlack,
    fontSize: 12,
    fontWeight: 800,
    lineHeight: 1.5,
    fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
  },
  badge: {
    display: 'inline-flex',
    alignItems: 'center',
    minHeight: 24,
    borderRadius: 999,
    padding: '4px 10px',
    border: '1px solid transparent',
    fontSize: 11,
    fontWeight: 800,
    lineHeight: 1.2,
    whiteSpace: 'nowrap',
  },
  stateBox: {
    borderRadius: 16,
    border: '1px solid rgba(0,0,0,0.06)',
    background: COLORS.white,
    padding: 14,
  },
  rowActionWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-start',
    gap: 6,
  },
  rowButton: {
    minHeight: 34,
    borderRadius: 9,
    border: 'none',
    padding: '0 12px',
    fontSize: 12,
    fontWeight: 800,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  statePanel: {
    padding: '48px 24px',
    textAlign: 'center',
  },
  spinner: {
    width: 38,
    height: 38,
    borderRadius: '50%',
    border: `3px solid ${COLORS.surface}`,
    borderTop: `3px solid ${COLORS.brand}`,
    margin: '0 auto 16px',
    animation: 'spin 0.8s linear infinite',
  },
};

function getSourceTypeLabel(sourceType) {
  return SOURCE_TYPE_LABELS[sourceType] || sourceType || '-';
}

function fmt(value) {
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value === 'number') return value.toLocaleString('ko-KR');
  return String(value);
}

function formatArea(value) {
  if (value === null || value === undefined || value === '') return '-';
  return `${fmt(value)}㎡`;
}

function formatDateTime(value) {
  if (!value) return '-';
  return String(value).slice(0, 16).replace('T', ' ');
}

function hasAreaValue(value) {
  return value !== null && value !== undefined && value !== '';
}

function getAdminStatusKey(unit) {
  if (unit?.marketReady) return 'READY';
  return unit?.blocker || 'UNKNOWN';
}

function getStatusMeta(unit) {
  return STATUS_META[getAdminStatusKey(unit)] || STATUS_META.UNKNOWN;
}

function isPrepareEligibleUnit(unit, allowInsufficientDataReprepare = false) {
  const blocker = getAdminStatusKey(unit);
  return Boolean(unit?.lawdCd)
    && hasAreaValue(unit?.exclusiveAreaValue)
    && !unit?.marketReady
    && (
      blocker === 'SNAPSHOT_NOT_FOUND'
      || (allowInsufficientDataReprepare && blocker === 'INSUFFICIENT_DATA')
    );
}

function getCounts(readiness, units) {
  return {
    ready: readiness?.readyUnitCount ?? units.filter(unit => unit?.marketReady).length,
    blocked: readiness?.blockedUnitCount ?? units.filter(unit => !unit?.marketReady).length,
    total: units.length,
  };
}

function getButtonStyle(tone, disabled) {
  const palette = {
    primary: [COLORS.brand, COLORS.white, 'none'],
    dark: [COLORS.nearBlack, COLORS.white, 'none'],
    secondary: [COLORS.surface, COLORS.nearBlack, 'none'],
    ghost: [COLORS.white, COLORS.nearBlack, '1px solid rgba(0,0,0,0.12)'],
  }[tone];

  return {
    ...S.button,
    background: palette[0],
    color: palette[1],
    border: palette[2],
    opacity: disabled ? 0.52 : 1,
    cursor: disabled ? 'not-allowed' : 'pointer',
  };
}

function getRowButtonStyle(tone, disabled) {
  const palette = {
    primary: [COLORS.brand, COLORS.white, 'none'],
    dark: [COLORS.nearBlack, COLORS.white, 'none'],
    secondary: [COLORS.surface, COLORS.secondary, 'none'],
    outline: [COLORS.white, COLORS.brand, '1px solid rgba(255,56,92,0.28)'],
  }[tone];

  return {
    ...S.rowButton,
    background: palette[0],
    color: palette[1],
    border: palette[2],
    opacity: disabled ? 0.72 : 1,
    cursor: disabled ? 'not-allowed' : 'pointer',
  };
}

function getPrepareDisabledReason({ loading, preparing, readiness, eligibleUnits, onPrepare }) {
  if (loading) return 'readiness 조회 중에는 prepare를 실행할 수 없습니다.';
  if (preparing) return 'prepare가 실행 중입니다.';
  if (!readiness) return 'readiness 데이터가 없습니다.';
  if (!readiness.rtmsServiceKeyConfigured) return 'RTMS 서비스키가 설정되지 않았습니다.';
  if (eligibleUnits.length === 0) return 'prepare 가능한 unit이 없습니다.';
  if (typeof onPrepare !== 'function') return 'onPrepare 핸들러가 연결되지 않았습니다.';
  return '';
}

function getRefreshDisabledReason({ loading, preparing, onRefresh }) {
  if (loading) return '조회 중';
  if (preparing) return 'prepare 실행 중';
  if (typeof onRefresh !== 'function') return 'onRefresh 미연결';
  return '';
}

function getRowAction(unit, allowInsufficientDataReprepare) {
  const key = getAdminStatusKey(unit);
  if (isPrepareEligibleUnit(unit, allowInsufficientDataReprepare)) {
    return {
      label: key === 'INSUFFICIENT_DATA' ? '표본 재수집' : '시세 데이터 준비',
      tone: 'primary',
    };
  }
  if (key === 'READY') return { label: 'READY', tone: 'outline' };
  return { label: '준비 불가', tone: 'secondary' };
}

function getRowDisabledReason({ unit, readiness, loading, preparing, allowInsufficientDataReprepare, onPrepare }) {
  const key = getAdminStatusKey(unit);
  if (loading) return 'readiness 조회 중입니다.';
  if (preparing) return 'prepare 실행 중입니다.';
  if (!readiness?.rtmsServiceKeyConfigured) return 'RTMS 서비스키 설정이 필요합니다.';
  if (typeof onPrepare !== 'function') return 'onPrepare 핸들러가 연결되지 않았습니다.';
  if (isPrepareEligibleUnit(unit, allowInsufficientDataReprepare)) return '';
  if (key === 'READY') return '이미 admin READY 상태입니다.';
  if (!unit?.lawdCd) return 'lawdCd 보완 후 prepare 가능합니다.';
  if (!hasAreaValue(unit?.exclusiveAreaValue)) return 'exclusiveAreaValue 보완 후 prepare 가능합니다.';
  if (key === 'INSUFFICIENT_DATA') return 'allowInsufficientDataReprepare=false라 재수집하지 않습니다.';
  if (key === 'UNKNOWN') return 'blocker 사유 확인이 필요합니다.';
  return `${key} 상태라 prepare 대상이 아닙니다.`;
}

function Badge({ tone, children }) {
  const palette = TONE_STYLES[tone] || TONE_STYLES.neutral;
  return (
    <span style={{ ...S.badge, background: palette.bg, color: palette.color, borderColor: palette.border }}>
      {children}
    </span>
  );
}

function QueryChip({ label, value, suffix }) {
  return (
    <span style={S.chip}>
      {label}
      <strong style={S.chipStrong}>{value}</strong>
      {suffix && <span>{suffix}</span>}
    </span>
  );
}

function SectionHeader({ loading, preparing, readiness, eligibleUnits, onRefresh, onPrepare, onPrepareAll }) {
  const prepareDisabledReason = getPrepareDisabledReason({ loading, preparing, readiness, eligibleUnits, onPrepare });
  const refreshDisabledReason = getRefreshDisabledReason({ loading, preparing, onRefresh });
  const prepareDisabled = Boolean(prepareDisabledReason);
  const refreshDisabled = Boolean(refreshDisabledReason);
  const prepareLabel = preparing
    ? '시세 데이터 준비 중'
    : `시세 데이터 준비${eligibleUnits.length > 0 ? ` ${eligibleUnits.length}건` : ''}`;

  return (
    <div style={S.head}>
      <div>
        <div style={S.eyebrow}>ADMIN MARKET READINESS</div>
        <h2 style={S.title}>주변시세 준비 상태</h2>
        <p style={S.desc}>
          관리자 상세 화면에서 readiness의 marketReady/blocker 값을 기준으로 주변시세 준비 여부와 prepare 대상을 확인합니다.
        </p>
      </div>
      <div style={S.actions}>
        <div style={S.actionGroup}>
          <button
            type="button"
            style={getButtonStyle('secondary', refreshDisabled)}
            disabled={refreshDisabled}
            aria-disabled={refreshDisabled}
            onClick={refreshDisabled ? undefined : onRefresh}
          >
            readiness 새로고침
          </button>
          {refreshDisabledReason && <span style={S.reason}>새로고침: {refreshDisabledReason}</span>}
        </div>
        <div style={S.actionGroup}>
          <button
            type="button"
            style={getButtonStyle('primary', prepareDisabled)}
            disabled={prepareDisabled}
            aria-disabled={prepareDisabled}
            onClick={prepareDisabled ? undefined : onPrepareAll}
          >
            {prepareLabel}
          </button>
          {prepareDisabledReason ? (
            <span style={S.reason}>prepare: {prepareDisabledReason}</span>
          ) : (
            <span style={{ ...S.reason, color: COLORS.green }}>prepare 가능 unit {eligibleUnits.length}건</span>
          )}
        </div>
      </div>
    </div>
  );
}

function LoadingState() {
  return (
    <div style={S.statePanel} role="status" aria-live="polite">
      <div style={S.spinner} />
      <h3 style={{ ...S.title, marginBottom: 8 }}>readiness를 불러오는 중입니다</h3>
      <p style={{ ...S.desc, margin: '0 auto' }}>관리자 주변시세 준비 상태를 조회하고 있습니다.</p>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

function ErrorState({ error, onRefresh }) {
  const message = typeof error === 'string' ? error : error?.message || 'readiness 조회 중 오류가 발생했습니다.';
  return (
    <div style={S.statePanel} role="alert" aria-live="assertive">
      <div style={{ ...S.alert, margin: '0 auto 16px', maxWidth: 560, background: COLORS.redTint, color: COLORS.brandDark, borderColor: 'rgba(224,11,65,0.18)' }}>
        {message}
      </div>
      {typeof onRefresh === 'function' && (
        <button type="button" style={getButtonStyle('dark', false)} onClick={onRefresh}>
          다시 조회
        </button>
      )}
    </div>
  );
}

function EmptyState({ onRefresh }) {
  return (
    <div style={S.statePanel} aria-live="polite">
      <h3 style={{ ...S.title, marginBottom: 8 }}>readiness 데이터가 없습니다</h3>
      <p style={{ ...S.desc, margin: '0 auto 18px' }}>
        AdminReviewDetailPage에서 조회한 readiness 객체를 전달하면 준비 상태와 prepare 대상을 표시합니다.
      </p>
      {typeof onRefresh === 'function' && (
        <button type="button" style={getButtonStyle('dark', false)} onClick={onRefresh}>
          readiness 조회
        </button>
      )}
    </div>
  );
}

function QueryBar({ readiness, eligibleCount, allowInsufficientDataReprepare }) {
  return (
    <div style={S.queryBar}>
      <QueryChip label="announcementId" value={fmt(readiness.announcementId)} />
      <QueryChip label="sourceType" value={fmt(readiness.sourceType)} suffix={getSourceTypeLabel(readiness.sourceType)} />
      <QueryChip label="조회기간" value={`${fmt(readiness.dealYmFrom)} ~ ${fmt(readiness.dealYmTo)}`} />
      <QueryChip label="prepare 대상" value={`${eligibleCount}건`} />
      <QueryChip label="INSUFFICIENT_DATA 재실행" value={allowInsufficientDataReprepare ? '허용' : '차단'} />
    </div>
  );
}

function RtmsKeyAlert({ configured }) {
  const palette = configured ? TONE_STYLES.success : TONE_STYLES.danger;
  const message = configured
    ? 'RTMS 서비스키가 설정되어 있습니다. prepare 가능 unit이 있으면 데이터 준비를 실행할 수 있습니다.'
    : 'RTMS 서비스키가 설정되지 않았습니다. 서비스키를 설정하기 전까지 prepare 버튼과 행 액션은 비활성화됩니다.';

  return (
    <div
      style={{ ...S.alert, background: palette.bg, color: palette.color, borderColor: palette.border }}
      role={configured ? 'status' : 'alert'}
      aria-live={configured ? 'polite' : 'assertive'}
    >
      {message}
    </div>
  );
}

function KpiGrid({ readiness, units, eligibleUnits }) {
  const counts = getCounts(readiness, units);
  const configuredLabel = readiness.rtmsServiceKeyConfigured ? 'ON' : 'OFF';
  const configuredTone = readiness.rtmsServiceKeyConfigured ? COLORS.green : COLORS.brandDark;

  const cards = [
    { label: '준비 완료 unit', value: counts.ready, note: 'marketReady=true', primary: true },
    { label: '차단 unit', value: counts.blocked, note: 'marketReady=false' },
    { label: '조회 대상', value: counts.total, note: 'readiness.units[] 기준' },
    { label: 'prepare 가능', value: eligibleUnits.length, note: 'lawdCd + 전용면적 + blocker 기준' },
    { label: 'RTMS key', value: configuredLabel, note: 'false면 prepare disabled', valueColor: configuredTone },
  ];

  return (
    <div style={S.kpiGrid}>
      {cards.map(card => (
        <div key={card.label} style={{ ...S.kpiCard, ...(card.primary ? S.kpiPrimary : null) }}>
          <div style={{ ...S.kpiLabel, color: card.primary ? 'rgba(255,255,255,0.78)' : COLORS.secondary }}>{card.label}</div>
          <div style={{ ...S.kpiValue, color: card.primary ? COLORS.white : card.valueColor || COLORS.nearBlack }}>{card.value}</div>
          <div style={{ ...S.kpiNote, color: card.primary ? 'rgba(255,255,255,0.78)' : COLORS.secondary }}>{card.note}</div>
        </div>
      ))}
    </div>
  );
}

function UnitState({ unit }) {
  const key = getAdminStatusKey(unit);
  const meta = getStatusMeta(unit);
  return (
    <div style={S.stateBox}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 8 }}>
        <Badge tone={meta.tone}>{meta.badge}</Badge>
        <span style={S.code}>{key}</span>
      </div>
      <div style={S.strong}>{meta.title}</div>
      <div style={{ ...S.muted, marginTop: 4 }}>{meta.helper}</div>
    </div>
  );
}

function UnitRow({ unit, readiness, loading, preparing, allowInsufficientDataReprepare, onPrepareUnit }) {
  const meta = getStatusMeta(unit);
  const action = getRowAction(unit, allowInsufficientDataReprepare);
  const disabledReason = getRowDisabledReason({
    unit,
    readiness,
    loading,
    preparing,
    allowInsufficientDataReprepare,
    onPrepare: onPrepareUnit,
  });
  const disabled = Boolean(disabledReason);
  const rowStyle = disabled ? S.disabledRow : null;

  return (
    <tr style={rowStyle}>
      <td style={S.td}>
        <div style={S.strong}>#{fmt(unit.unitOrder)}</div>
        <div style={{ ...S.muted, marginTop: 4 }}>unitId {fmt(unit.unitId)}</div>
      </td>
      <td style={S.td}>
        <Badge tone={meta.tone}>{meta.badge}</Badge>
        <div style={{ ...S.code, marginTop: 6 }}>{getAdminStatusKey(unit)}</div>
      </td>
      <td style={S.td}>
        <div style={S.strong}>{fmt(unit.complexName)}</div>
        <div style={{ ...S.muted, marginTop: 4, maxWidth: 260 }}>{fmt(unit.fullAddress)}</div>
      </td>
      <td style={S.td}>
        <div style={S.code}>lawdCd {fmt(unit.lawdCd)}</div>
        <div style={S.muted}>legalDongCode {fmt(unit.legalDongCode)}</div>
        <div style={S.muted}>{fmt(unit.addressStatus)} · {formatDateTime(unit.addressNormalizedAt)}</div>
        {unit.addressMessage && <div style={{ ...S.muted, marginTop: 4, maxWidth: 220 }}>{unit.addressMessage}</div>}
      </td>
      <td style={S.td}>
        <div style={S.strong}>전용 {formatArea(unit.exclusiveAreaValue)}</div>
        <div style={{ ...S.muted, marginTop: 4 }}>
          추천 {fmt(unit.recommendedSourceType)} · {getSourceTypeLabel(unit.recommendedSourceType)}
        </div>
      </td>
      <td style={S.td}>
        <div>raw <strong>{fmt(unit.rawTransactionCount)}</strong>건</div>
        <div style={S.muted}>snapshot {unit.snapshotFound ? 'found' : 'not found'}</div>
        <div style={S.muted}>status {fmt(unit.snapshotStatus)}</div>
      </td>
      <td style={S.td}>
        <UnitState unit={unit} />
      </td>
      <td style={S.td}>
        <div style={S.rowActionWrap}>
          <button
            type="button"
            style={getRowButtonStyle(action.tone, disabled)}
            disabled={disabled}
            aria-disabled={disabled}
            onClick={disabled ? undefined : () => onPrepareUnit([unit])}
          >
            {action.label}
          </button>
          {disabledReason && <span style={{ ...S.muted, maxWidth: 190 }}>{disabledReason}</span>}
        </div>
      </td>
    </tr>
  );
}

function UnitTable({ readiness, units, loading, preparing, allowInsufficientDataReprepare, onPrepareUnit }) {
  return (
    <div style={S.tableWrap}>
      <table style={S.table}>
        <caption style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}>
          관리자 주변시세 readiness unit 목록
        </caption>
        <thead>
          <tr>
            {['Unit', '상태', '단지/주소', '주소 정규화', '면적/sourceType', 'Raw/Snapshot', '관리자 분기', '행 액션'].map(label => (
              <th key={label} style={S.th}>{label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {units.map((unit, index) => (
            <UnitRow
              key={unit.unitId ?? `${unit.unitOrder ?? 'unit'}-${index}`}
              unit={unit}
              readiness={readiness}
              loading={loading}
              preparing={preparing}
              allowInsufficientDataReprepare={allowInsufficientDataReprepare}
              onPrepareUnit={onPrepareUnit}
            />
          ))}
          {units.length === 0 && (
            <tr>
              <td style={{ ...S.td, textAlign: 'center', color: COLORS.secondary, padding: 32 }} colSpan="8">
                readiness.units[]가 비어 있습니다.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export default function AdminMarketReadinessSection({
  readiness = null,
  loading = false,
  error = null,
  preparing = false,
  onRefresh,
  onPrepare,
  allowInsufficientDataReprepare = false,
}) {
  const units = Array.isArray(readiness?.units) ? readiness.units : [];
  const eligibleUnits = readiness
    ? units.filter(unit => isPrepareEligibleUnit(unit, allowInsufficientDataReprepare))
    : [];

  const handlePrepareAll = () => {
    if (typeof onPrepare === 'function') onPrepare(eligibleUnits, readiness);
  };

  const handlePrepareUnit = (targetUnits) => {
    if (typeof onPrepare === 'function') onPrepare(targetUnits, readiness);
  };

  return (
    <section style={S.panel} aria-busy={Boolean(loading || preparing)} aria-live="polite">
      <SectionHeader
        loading={loading}
        preparing={preparing}
        readiness={readiness}
        eligibleUnits={eligibleUnits}
        onRefresh={onRefresh}
        onPrepare={onPrepare}
        onPrepareAll={handlePrepareAll}
      />

      {loading ? (
        <LoadingState />
      ) : error ? (
        <ErrorState error={error} onRefresh={onRefresh} />
      ) : !readiness ? (
        <EmptyState onRefresh={onRefresh} />
      ) : (
        <>
          <QueryBar
            readiness={readiness}
            eligibleCount={eligibleUnits.length}
            allowInsufficientDataReprepare={allowInsufficientDataReprepare}
          />
          <RtmsKeyAlert configured={Boolean(readiness.rtmsServiceKeyConfigured)} />
          <div style={S.content}>
            <KpiGrid readiness={readiness} units={units} eligibleUnits={eligibleUnits} />
          </div>
          <UnitTable
            readiness={readiness}
            units={units}
            loading={loading}
            preparing={preparing}
            allowInsufficientDataReprepare={allowInsufficientDataReprepare}
            onPrepareUnit={handlePrepareUnit}
          />
        </>
      )}
    </section>
  );
}
