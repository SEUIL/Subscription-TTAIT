import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import StatusBadge from '../components/common/StatusBadge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../components/common/Toast';
import { formatPrice, calcDday, getDdayColor } from '../components/common/AnnouncementCard';

const S = {
  backBtn: {
    display: 'inline-flex', alignItems: 'center', gap: 8, padding: '8px 16px', borderRadius: 8,
    border: 'none', background: 'transparent', cursor: 'pointer', fontSize: 14, fontWeight: 500,
    color: '#6a6a6a', marginBottom: 16,
  },
  layout: { maxWidth: 1200, margin: '0 auto', padding: '32px 24px 80px', display: 'grid', gridTemplateColumns: '1fr 360px', gap: 64, alignItems: 'start' },
  title: { fontSize: 26, fontWeight: 700, color: '#222', lineHeight: 1.35, letterSpacing: '-0.4px', marginBottom: 16 },
  sectionTitle: {
    fontSize: 20, fontWeight: 700, color: '#222', letterSpacing: '-0.3px', marginBottom: 20,
    paddingBottom: 12, borderBottom: '1px solid rgba(0,0,0,0.08)',
  },
  infoGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0 },
  infoItem: { padding: '13px 0', borderBottom: '1px solid rgba(0,0,0,0.06)', display: 'flex', alignItems: 'flex-start', gap: 12 },
  infoLabel: { fontSize: 13, color: '#6a6a6a', fontWeight: 400, minWidth: 80, flexShrink: 0, paddingTop: 2 },
  infoValue: { fontSize: 14, color: '#222', fontWeight: 500, lineHeight: 1.5 },
  unitGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16 },
  unitCard: { background: '#fff', border: '1px solid rgba(0,0,0,0.08)', borderRadius: 16, padding: 20 },
  unitBadge: { display: 'inline-flex', alignItems: 'center', borderRadius: 999, padding: '5px 10px', background: '#fff0f3', color: '#ff385c', fontSize: 11, fontWeight: 700, whiteSpace: 'nowrap' },
  unitMeta: { display: 'flex', justifyContent: 'space-between', gap: 12, padding: '8px 0', borderBottom: '1px solid rgba(0,0,0,0.04)' },
  stickyCard: {
    position: 'sticky', top: 90, background: '#fff', borderRadius: 20, padding: 28,
    boxShadow: 'var(--shadow-card)',
  },
  applyBtn: {
    width: '100%', height: 56, borderRadius: 12, background: '#ff385c', border: 'none', cursor: 'pointer',
    fontSize: 16, fontWeight: 600, color: '#fff', marginBottom: 12, transition: 'background 0.2s',
  },
  saveBtn: {
    width: '100%', height: 48, borderRadius: 12, background: 'transparent', border: '1px solid #c1c1c1',
    cursor: 'pointer', fontSize: 15, fontWeight: 500, color: '#222',
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, marginBottom: 24,
    transition: 'border-color 0.15s',
  },
  quickRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0' },
};

const normalizeLocationUnit = (unit = {}) => ({
  ...unit,
  complexName: unit.complexName || '공급 단위',
  fullAddress: unit.fullAddress || '',
});

const getLocationUnitKey = (unit, idx) => unit.unitId ?? `${unit.unitOrder ?? idx}-${unit.complexName ?? ''}-${unit.fullAddress ?? ''}`;

const MARKET_STATUS_MESSAGES = {
  INSUFFICIENT_DATA: '비교 가능한 주변 거래가 아직 충분하지 않습니다.',
  SNAPSHOT_NOT_FOUND: '이 지역의 주변 시세 데이터를 준비 중입니다.',
  UNIT_LAWD_CD_MISSING: '주소 기준 시세 비교를 준비 중입니다.',
  UNIT_AREA_MISSING: '전용면적 정보가 없어 시세 비교가 어렵습니다.',
  NOT_REQUESTED: '시세 비교를 준비 중입니다.',
  FAILED: '시세 정보를 불러오지 못했습니다.',
};

const EMBEDDED_MARKET_STATUS_MAP = {
  AVAILABLE: 'COMPARABLE',
  NOT_REQUESTED: 'NOT_REQUESTED',
  NO_DATA: 'SNAPSHOT_NOT_FOUND',
  INSUFFICIENT_DATA: 'INSUFFICIENT_DATA',
  FAILED: 'FAILED',
};

const MARKET_SOURCE_LABELS = {
  APT_RENT: '아파트 전월세',
  APT_TRADE: '아파트 매매',
  ROW_HOUSE_RENT: '연립/다세대 전월세',
  ROW_HOUSE_TRADE: '연립/다세대 매매',
  OFFICETEL_RENT: '오피스텔 전월세',
  OFFICETEL_TRADE: '오피스텔 매매',
};

const MARKET_METRIC_DEFINITIONS = [
  { key: 'monthlyRentComparison', label: '월세', marketLabel: '주변 월세', unitLabel: '공고 월세', accent: '#ff385c' },
  { key: 'depositComparison', label: '보증금', marketLabel: '주변 보증금', unitLabel: '공고 보증금', accent: '#1f8a70' },
  { key: 'tradeComparison', label: '매매가', marketLabel: '주변 매매가', unitLabel: '공고 금액', accent: '#2f5f98' },
];

const resolveMarketSourceType = (unit = {}, announcement = {}) => {
  const text = [
    unit.houseType,
    unit.supplyType,
    unit.complexName,
    unit.buildingType,
    announcement.houseType,
    announcement.supplyType,
    announcement.complexName,
  ].filter(Boolean).join(' ');

  if (text.includes('오피스텔')) return 'OFFICETEL_RENT';
  if (text.includes('연립') || text.includes('다세대') || text.includes('다가구')) return 'ROW_HOUSE_RENT';
  return 'APT_RENT';
};

const formatDealYmLabel = (dealYm) => {
  const value = String(dealYm || '');
  if (!/^\d{6}$/.test(value)) return '';
  return `${value.slice(0, 4)}년 ${Number(value.slice(4, 6))}월`;
};

const getMarketDealYmRange = (referenceDate = new Date()) => {
  const dealYmToDate = new Date(referenceDate.getFullYear(), referenceDate.getMonth() - 1, 1);
  const dealYmFromDate = new Date(dealYmToDate.getFullYear(), dealYmToDate.getMonth() - 5, 1);
  const formatDealYm = (date) => `${date.getFullYear()}${String(date.getMonth() + 1).padStart(2, '0')}`;
  const dealYmFrom = formatDealYm(dealYmFromDate);
  const dealYmTo = formatDealYm(dealYmToDate);

  return {
    dealYmFrom,
    dealYmTo,
    label: `${formatDealYmLabel(dealYmFrom)}~${formatDealYmLabel(dealYmTo)} 거래`,
  };
};

const toFiniteNumber = (value) => {
  if (value != null) {
    const number = Number(value);
    return Number.isFinite(number) ? number : null;
  }
  return null;
};

const formatMarketAmount = (value) => {
  const number = toFiniteNumber(value);
  return number != null ? `${formatPrice(number)} 만원` : '-';
};

const formatMarketRate = (value) => {
  const number = toFiniteNumber(value);
  if (number != null) {
    const absolute = Math.abs(number);
    const rounded = Math.abs(absolute - Math.round(absolute)) < 0.05 ? String(Math.round(absolute)) : absolute.toFixed(1);
    return `${rounded}%`;
  }
  return '';
};

const clampMarketRatio = (value) => Math.max(0, Math.min(100, value));

const normalizeMarketRatioPercent = (value) => {
  const number = toFiniteNumber(value);
  if (number != null) return number > 0 && number <= 2 ? number * 100 : number;
  return null;
};

const getPreferredMarketRatio = (metric) => {
  const preferredRatio = normalizeMarketRatioPercent(metric?.ratioPercent);
  if (preferredRatio != null) return preferredRatio;

  const unit = toFiniteNumber(metric?.unitAmount);
  const market = toFiniteNumber(metric?.marketAmount);
  if (unit != null && market != null) {
    if (market <= 0) return unit <= 0 ? 0 : 100;
    return (unit / market) * 100;
  }
  return null;
};

const getMarketRatio = (metric) => {
  const ratio = getPreferredMarketRatio(metric);
  return ratio != null ? clampMarketRatio(ratio) : null;
};

const normalizeMarketMetric = (metric) => {
  if (!metric || typeof metric !== 'object') return null;
  return {
    unitAmount: metric.unitAmount,
    marketAmount: metric.marketAmount,
    differenceAmount: metric.differenceAmount,
    differenceRatePercent: metric.differenceRatePercent,
    ratioPercent: metric.ratioPercent,
    discountRatePercent: metric.discountRatePercent,
  };
};

const normalizeEmbeddedMarketStatus = (status) => EMBEDDED_MARKET_STATUS_MAP[status] || status;

const hasEmbeddedMarketShape = (comparison) => (
  comparison.marketDeposit != null ||
  comparison.publicDeposit != null ||
  comparison.marketMonthlyRent != null ||
  comparison.publicMonthlyRent != null ||
  comparison.depositRatio != null ||
  comparison.monthlyRentRatio != null ||
  comparison.depositDiscountRate != null ||
  comparison.monthlyRentDiscountRate != null ||
  comparison.sampleCount != null ||
  comparison.sourceLabel != null ||
  comparison.updatedAt != null ||
  comparison.aggregatedAt != null ||
  comparison.regionName != null ||
  comparison.comparisonType != null ||
  comparison.status === 'AVAILABLE' ||
  comparison.status === 'NOT_REQUESTED' ||
  comparison.status === 'NO_DATA' ||
  comparison.status === 'FAILED'
);

const normalizeEmbeddedMarketMetric = ({ unitAmount, marketAmount, ratioPercent, discountRatePercent }) => {
  const unit = toFiniteNumber(unitAmount);
  const market = toFiniteNumber(marketAmount);

  return normalizeMarketMetric({
    unitAmount,
    marketAmount,
    differenceAmount: unit != null && market != null ? unit - market : null,
    ratioPercent,
    discountRatePercent,
  });
};

const normalizeEmbeddedMarketComparison = (comparison) => ({
  status: normalizeEmbeddedMarketStatus(comparison.status),
  unitPrice: comparison.unitPrice ?? null,
  sourceLabel: comparison.sourceLabel ?? comparison.snapshot?.sourceLabel ?? null,
  regionName: comparison.regionName ?? comparison.snapshot?.regionName ?? null,
  comparisonType: comparison.comparisonType ?? comparison.snapshot?.comparisonType ?? null,
  snapshot: {
    ...(comparison.snapshot || {}),
    sampleCount: comparison.sampleCount ?? comparison.snapshot?.sampleCount,
    updatedAt: comparison.updatedAt ?? comparison.snapshot?.updatedAt,
    aggregatedAt: comparison.aggregatedAt ?? comparison.snapshot?.aggregatedAt,
    sourceLabel: comparison.sourceLabel ?? comparison.snapshot?.sourceLabel,
    regionName: comparison.regionName ?? comparison.snapshot?.regionName,
    comparisonType: comparison.comparisonType ?? comparison.snapshot?.comparisonType,
  },
  depositComparison: normalizeEmbeddedMarketMetric({
    unitAmount: comparison.publicDeposit,
    marketAmount: comparison.marketDeposit,
    ratioPercent: comparison.depositRatio,
    discountRatePercent: comparison.depositDiscountRate,
  }),
  monthlyRentComparison: normalizeEmbeddedMarketMetric({
    unitAmount: comparison.publicMonthlyRent,
    marketAmount: comparison.marketMonthlyRent,
    ratioPercent: comparison.monthlyRentRatio,
    discountRatePercent: comparison.monthlyRentDiscountRate,
  }),
  tradeComparison: normalizeMarketMetric(comparison.tradeComparison),
});

const normalizeMarketComparison = (comparison) => {
  if (!comparison || typeof comparison !== 'object') return null;
  const hasCurrentMetricShape = comparison.depositComparison != null || comparison.monthlyRentComparison != null || comparison.tradeComparison != null;
  if (hasEmbeddedMarketShape(comparison) && !hasCurrentMetricShape) return normalizeEmbeddedMarketComparison(comparison);

  return {
    status: comparison.status,
    unitPrice: comparison.unitPrice ?? null,
    snapshot: comparison.snapshot ?? null,
    sourceLabel: comparison.sourceLabel ?? comparison.snapshot?.sourceLabel ?? null,
    regionName: comparison.regionName ?? comparison.snapshot?.regionName ?? null,
    comparisonType: comparison.comparisonType ?? comparison.snapshot?.comparisonType ?? null,
    depositComparison: normalizeMarketMetric(comparison.depositComparison),
    monthlyRentComparison: normalizeMarketMetric(comparison.monthlyRentComparison),
    tradeComparison: normalizeMarketMetric(comparison.tradeComparison),
  };
};

const isMarketMetricAvailable = (metric) => (
  metric &&
  metric.unitAmount != null &&
  metric.marketAmount != null &&
  toFiniteNumber(metric.unitAmount) != null &&
  toFiniteNumber(metric.marketAmount) != null
);

const getAvailableMarketMetrics = (comparison) => MARKET_METRIC_DEFINITIONS
  .map((definition) => ({ definition, metric: comparison?.[definition.key] }))
  .filter(({ metric }) => isMarketMetricAvailable(metric));

const getMarketDifferenceAmount = (metric) => {
  const difference = toFiniteNumber(metric?.differenceAmount);
  if (difference != null) return difference;

  const unit = toFiniteNumber(metric?.unitAmount);
  const market = toFiniteNumber(metric?.marketAmount);
  if (unit != null && market != null) return unit - market;
  return null;
};

const getMarketDifferenceRate = (metric) => {
  const rate = toFiniteNumber(metric?.differenceRatePercent);
  if (rate != null) return rate;

  const difference = getMarketDifferenceAmount(metric);
  const market = toFiniteNumber(metric?.marketAmount);
  if (difference != null && market != null && market !== 0) return (difference / market) * 100;
  return null;
};

const getMarketMetricInterpretation = (definition, metric) => {
  const unit = toFiniteNumber(metric?.unitAmount);
  const market = toFiniteNumber(metric?.marketAmount);
  const difference = getMarketDifferenceAmount(metric);
  const discountRate = toFiniteNumber(metric?.discountRatePercent);
  const rate = difference != null && difference < 0 && discountRate != null ? discountRate : getMarketDifferenceRate(metric);
  const rateText = rate != null ? ` (${formatMarketRate(rate)})` : '';

  if (unit != null && market != null && difference != null) {
    if (market === 0) {
      if (unit === 0) return `공고 ${definition.label}가 주변 기준과 같은 수준입니다.`;
      return `공고 ${definition.label}는 금액으로 직접 확인해 주세요.`;
    }
    if (difference < 0) return `공고 ${definition.label}가 주변 시세보다 ${formatMarketAmount(Math.abs(difference))}${rateText} 낮아요.`;
    if (difference > 0) return `공고 ${definition.label}가 주변 시세보다 ${formatMarketAmount(difference)}${rateText} 높아요.`;
    return `공고 ${definition.label}가 주변 시세와 같은 수준입니다.`;
  }
  return '금액 차이를 계산하기 어렵습니다.';
};

const getMarketStatusMessage = (status, hasMetrics) => {
  if (status === 'COMPARABLE') return hasMetrics ? '' : '비교할 가격 정보가 부족합니다.';
  return MARKET_STATUS_MESSAGES[status] || (hasMetrics ? '' : '비교할 가격 정보가 부족합니다.');
};

const getMarketSourceLabel = (sourceType) => MARKET_SOURCE_LABELS[sourceType] || MARKET_SOURCE_LABELS.APT_RENT;

const getUserSafeMarketText = (value) => {
  if (value != null) {
    const text = String(value).trim();
    if (!text) return '';
    if (/^[A-Z0-9_]+$/.test(text)) return '';
    if (/(api|backend|admin|developer|fallback|lawdCd|sourceType|unitId|snapshot|status|raw|개발|관리자)/i.test(text)) return '';
    return text;
  }
  return '';
};

const getMarketUnitAreaLabel = (unit = {}) => unit.exclusiveAreaText || (unit.exclusiveAreaValue != null ? `${unit.exclusiveAreaValue}㎡` : '');

const getMarketRegionLabel = (unit = {}, announcement = {}) => (
  [unit.regionLevel1, unit.regionLevel2, unit.regionLevel3].filter(Boolean).join(' ') ||
  [announcement.regionLevel1, announcement.regionLevel2].filter(Boolean).join(' ') ||
  unit.fullAddress ||
  announcement.fullAddress ||
  ''
);

const getMarketSampleCount = (snapshot) => {
  if (!snapshot || typeof snapshot !== 'object') return null;
  const fields = ['sampleCount', 'totalSampleCount', 'dealCount', 'transactionCount', 'tradeCount', 'rentCount', 'count', 'totalCount'];

  for (const field of fields) {
    const number = toFiniteNumber(snapshot[field]);
    if (number != null) return Math.max(0, Math.round(number));
  }
  return null;
};

const formatMarketDate = (value) => {
  if (value != null) {
    const text = String(value);
    if (/^\d{6}$/.test(text)) return `${text.slice(0, 4)}.${text.slice(4, 6)}`;
    if (/^\d{8}$/.test(text)) return `${text.slice(0, 4)}.${text.slice(4, 6)}.${text.slice(6, 8)}`;
    if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10).replace(/-/g, '.');

    const date = new Date(text);
    if (!Number.isNaN(date.getTime())) return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
  }
  return '';
};

const getMarketUpdatedLabel = (snapshot) => {
  if (!snapshot || typeof snapshot !== 'object') return '';
  return formatMarketDate(
    snapshot.aggregatedAt ??
    snapshot.updatedAt ??
    snapshot.updatedDate ??
    snapshot.baseDate ??
    snapshot.baseYm ??
    snapshot.dealYmTo ??
    snapshot.createdAt
  );
};

let naverMapsSdkPromise = null;

const loadNaverMapsSdk = (clientId) => {
  if (!clientId) return Promise.reject(new Error('Naver Maps client id is missing'));
  if (window.naver?.maps) return Promise.resolve(window.naver.maps);
  if (naverMapsSdkPromise) return naverMapsSdkPromise;

  naverMapsSdkPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`;
    script.async = true;
    script.dataset.naverMapsSdk = 'true';
    script.onload = () => {
      if (window.naver?.maps) {
        resolve(window.naver.maps);
        return;
      }
      naverMapsSdkPromise = null;
      reject(new Error('Naver Maps SDK did not initialize'));
    };
    script.onerror = () => {
      naverMapsSdkPromise = null;
      reject(new Error('Failed to load Naver Maps SDK'));
    };
    document.head.appendChild(script);
  });

  return naverMapsSdkPromise;
};

const StaticLocationMap = ({ title, address }) => (
  <div style={{
    position: 'relative', height: 300, overflow: 'hidden',
    background: '#eef3f7',
    backgroundImage: 'linear-gradient(90deg, rgba(255,255,255,0.75) 1px, transparent 1px), linear-gradient(rgba(255,255,255,0.75) 1px, transparent 1px), linear-gradient(135deg, #edf7f2 0%, #e8f1fb 52%, #f7f3ec 100%)',
    backgroundSize: '56px 56px, 56px 56px, 100% 100%',
  }}>
    <div style={{ position: 'absolute', inset: 0, opacity: 0.68 }}>
      <div style={{ position: 'absolute', left: '-6%', top: '44%', width: '112%', height: 24, borderRadius: 999, background: '#fff', transform: 'rotate(-8deg)', boxShadow: '0 0 0 1px rgba(0,0,0,0.03)' }} />
      <div style={{ position: 'absolute', left: '18%', top: '-10%', width: 24, height: '120%', borderRadius: 999, background: '#fff', transform: 'rotate(20deg)', boxShadow: '0 0 0 1px rgba(0,0,0,0.03)' }} />
      <div style={{ position: 'absolute', right: '12%', top: '-6%', width: 18, height: '118%', borderRadius: 999, background: '#fff', transform: 'rotate(-30deg)', boxShadow: '0 0 0 1px rgba(0,0,0,0.03)' }} />
    </div>
    <div style={{ position: 'absolute', left: '50%', top: '50%', width: 128, height: 128, borderRadius: '50%', background: 'rgba(255,56,92,0.10)', transform: 'translate(-50%, -50%)' }} />
    <div style={{ position: 'absolute', left: '50%', top: '50%', width: 58, height: 58, borderRadius: '50%', background: 'rgba(255,56,92,0.18)', transform: 'translate(-50%, -50%)' }} />
    <div style={{ position: 'absolute', left: '50%', top: '50%', transform: 'translate(-50%, -100%)', filter: 'drop-shadow(0 8px 14px rgba(255,56,92,0.28))' }}>
      <div style={{ width: 42, height: 42, borderRadius: '50% 50% 50% 0', background: '#ff385c', border: '3px solid #fff', transform: 'rotate(-45deg)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.4" width="18" height="18" style={{ transform: 'rotate(45deg)' }}>
          <path d="M3 21h18" /><path d="M5 21V7l8-4v18" /><path d="M19 21V11l-6-4" /><path d="M9 9h.01M9 13h.01M9 17h.01" />
        </svg>
      </div>
    </div>
    <div style={{ position: 'absolute', left: 20, bottom: 20, maxWidth: 'calc(100% - 40px)', padding: '12px 14px', borderRadius: 14, background: 'rgba(255,255,255,0.94)', boxShadow: '0 8px 24px rgba(0,0,0,0.12)', backdropFilter: 'blur(10px)' }}>
      <p style={{ fontSize: 14, fontWeight: 700, color: '#222', marginBottom: 4 }}>{title}</p>
      <p style={{ fontSize: 13, color: '#6a6a6a', lineHeight: 1.5 }}>{address || '주소 정보 없음'}</p>
    </div>
  </div>
);

const NaverLocationMap = ({ unit, title, address }) => {
  const mapRef = useRef(null);
  const [status, setStatus] = useState('loading');
  const lat = Number(unit.latitude);
  const lng = Number(unit.longitude);
  const hasCoords = Number.isFinite(lat) && Number.isFinite(lng);

  useEffect(() => {
    if (!hasCoords || !mapRef.current) {
      setStatus('fallback');
      return undefined;
    }

    let cancelled = false;
    let map = null;
    let marker = null;

    const initializeMap = async () => {
      setStatus('loading');
      try {
        const configResponse = await fetch('/api/config/naver-maps');
        if (!configResponse.ok) throw new Error('Naver Maps config request failed');
        const config = await configResponse.json();
        const naverMaps = await loadNaverMapsSdk(config.clientId);
        if (cancelled || !mapRef.current) return;

        const position = new naverMaps.LatLng(lat, lng);
        map = new naverMaps.Map(mapRef.current, {
          center: position,
          zoom: 15,
          draggable: true,
          scrollWheel: true,
          pinchZoom: true,
          keyboardShortcuts: true,
        });
        marker = new naverMaps.Marker({ position, map });
        setStatus('ready');
      } catch (error) {
        console.warn('네이버 지도를 불러오지 못했습니다.', error);
        if (!cancelled) setStatus('fallback');
      }
    };

    initializeMap();

    return () => {
      cancelled = true;
      if (marker) marker.setMap(null);
      if (map && window.naver?.maps?.Event) {
        window.naver.maps.Event.clearInstanceListeners(map);
      }
      if (mapRef.current) mapRef.current.innerHTML = '';
    };
  }, [lat, lng, hasCoords]);

  if (status === 'fallback') {
    return <StaticLocationMap title={title} address={address} />;
  }

  return (
    <div style={{ position: 'relative', height: 300, overflow: 'hidden', background: '#eef3f7' }}>
      <div ref={mapRef} style={{ width: '100%', height: '100%' }} />
      {status === 'loading' && (
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(90deg,#eef3f7 0%,#fafafa 50%,#eef3f7 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ padding: '10px 14px', borderRadius: 999, background: '#fff', color: '#6a6a6a', fontSize: 13, fontWeight: 700, boxShadow: '0 2px 10px rgba(0,0,0,0.08)' }}>네이버 지도 불러오는 중</div>
        </div>
      )}
      <div style={{ position: 'absolute', left: 20, bottom: 20, maxWidth: 'calc(100% - 40px)', padding: '12px 14px', borderRadius: 14, background: 'rgba(255,255,255,0.94)', boxShadow: '0 8px 24px rgba(0,0,0,0.12)', backdropFilter: 'blur(10px)' }}>
        <p style={{ fontSize: 14, fontWeight: 700, color: '#222', marginBottom: 4 }}>{title}</p>
        <p style={{ fontSize: 13, color: '#6a6a6a', lineHeight: 1.5 }}>{address || '주소 정보 없음'}</p>
      </div>
    </div>
  );
};

const LocationInfoSection = ({ sectionTitleStyle, announcementName, announcementAddress, units }) => {
  const normalizedUnits = (Array.isArray(units) ? units : []).map(normalizeLocationUnit);
  const representativeUnit = normalizedUnits.find((unit) =>
    unit.geocodeStatus === 'SUCCESS' &&
    unit.latitude != null &&
    unit.longitude != null
  );
  const fallbackUnit = representativeUnit || normalizedUnits[0] || normalizeLocationUnit({
    complexName: announcementName || '공급 위치',
    fullAddress: announcementAddress || '',
  });
  const address = String(fallbackUnit.fullAddress || announcementAddress || '').trim();
  const title = fallbackUnit.complexName || announcementName || '공급 위치';
  const hasRepresentative = Boolean(representativeUnit);
  const visibleUnits = normalizedUnits.slice(0, 3);
  const hiddenUnitCount = Math.max(normalizedUnits.length - visibleUnits.length, 0);

  if (!address && normalizedUnits.length === 0) return null;

  const naverMapUrl = address ? `https://map.naver.com/v5/search/${encodeURIComponent(address)}` : '';
  const handleCopyAddress = async () => {
    if (!address || !navigator.clipboard) return;
    try {
      await navigator.clipboard.writeText(address);
    } catch (error) {
      console.warn('주소 복사에 실패했습니다.', error);
    }
  };

  return (
    <div style={{ marginBottom: 40 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 16, marginBottom: 20, paddingBottom: 12, borderBottom: '1px solid rgba(0,0,0,0.08)' }}>
        <div>
          <h2 style={{ ...sectionTitleStyle, marginBottom: 6, paddingBottom: 0, borderBottom: 'none' }}>위치 정보</h2>
          <p style={{ fontSize: 13, color: '#6a6a6a', lineHeight: 1.5 }}>지도와 주소로 공급 위치를 확인해 주세요.</p>
        </div>
      </div>
      <div style={{ border: '1px solid rgba(0,0,0,0.08)', borderRadius: 20, overflow: 'hidden', background: '#fff', boxShadow: 'rgba(0,0,0,0.02) 0px 1px 4px' }}>
        {hasRepresentative ? (
          <NaverLocationMap unit={representativeUnit} title={title} address={address} />
        ) : (
          <div style={{ display: 'flex', gap: 14, alignItems: 'flex-start', padding: 20, background: '#fafafa', borderBottom: '1px solid rgba(0,0,0,0.08)' }}>
            <div style={{ width: 40, height: 40, borderRadius: 12, background: '#fff', color: '#ff385c', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="20" height="20"><path d="M21 10c0 7-9 12-9 12S3 17 3 10a9 9 0 1 1 18 0z" /><circle cx="12" cy="10" r="3" /></svg>
            </div>
            <div style={{ minWidth: 0 }}>
              <p style={{ fontSize: 15, fontWeight: 700, color: '#222', marginBottom: 6 }}>
                {address ? '주소를 기준으로 위치를 확인해 주세요.' : '주소 정보가 등록되면 위치를 확인할 수 있습니다.'}
              </p>
              <p style={{ fontSize: 13, color: '#6a6a6a', lineHeight: 1.6 }}>
                {address || '상세 주소는 공고 원문을 확인해 주세요.'}
              </p>
            </div>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto', gap: 16, alignItems: 'center', padding: 20 }}>
          <div>
            <p style={{ fontSize: 14, fontWeight: 500, color: '#222', lineHeight: 1.55 }}>{address || '주소 정보 없음'}</p>
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
            {naverMapUrl && (
              <a href={naverMapUrl} target="_blank" rel="noopener noreferrer" style={{ padding: '10px 14px', borderRadius: 10, background: '#03c75a', color: '#fff', fontSize: 13, fontWeight: 700, textDecoration: 'none' }}>
                네이버 지도에서 보기
              </a>
            )}
            {address && (
              <button type="button" onClick={handleCopyAddress} style={{ padding: '10px 14px', borderRadius: 10, background: '#fff', border: '1px solid #c1c1c1', color: '#222', fontSize: 13, fontWeight: 700, cursor: 'pointer' }}>
                주소 복사
              </button>
            )}
          </div>
        </div>

        {visibleUnits.length > 0 && (
          <div style={{ borderTop: '1px solid rgba(0,0,0,0.06)', padding: 16, background: '#fafafa' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 10 }}>
              {visibleUnits.map((unit, index) => (
                <div key={getLocationUnitKey(unit, index)} style={{ border: '1px solid rgba(0,0,0,0.08)', borderRadius: 14, padding: 12, background: representativeUnit === unit ? '#fff8f9' : '#fff', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <p style={{ fontSize: 13, fontWeight: 700, color: '#222', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: 8 }}>{unit.complexName}</p>
                  <p style={{ fontSize: 12, color: '#6a6a6a', lineHeight: 1.45 }}>{unit.fullAddress || '주소 정보 없음'}</p>
                </div>
              ))}
            </div>
            {hiddenUnitCount > 0 && (
              <p style={{ marginTop: 10, fontSize: 12, color: '#6a6a6a', lineHeight: 1.5 }}>
                외 {hiddenUnitCount}개 공급 위치는 아래 공급 단위 정보에서 확인해 주세요.
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

const MarketUnitSelector = ({ units, selectedUnitId, onSelect }) => {
  if (units.length <= 1) return null;

  return (
    <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginBottom: 18 }}>
      {units.map((unit, index) => {
        const selected = String(unit.unitId) === String(selectedUnitId);
        const title = unit.complexName || unit.houseType || `공급 단위 ${index + 1}`;
        const subtitle = [getMarketUnitAreaLabel(unit), [unit.regionLevel1, unit.regionLevel2].filter(Boolean).join(' ')].filter(Boolean).join(' · ');

        return (
          <button
            key={unit.unitId}
            type="button"
            onClick={() => onSelect(unit.unitId)}
            style={{
              flex: '1 1 190px', minWidth: 0, textAlign: 'left', padding: '13px 14px', borderRadius: 14,
              border: selected ? '1px solid #ff385c' : '1px solid rgba(0,0,0,0.08)',
              background: selected ? '#fff0f3' : '#fff', cursor: 'pointer', boxShadow: selected ? '0 10px 24px rgba(255,56,92,0.12)' : 'none',
            }}
          >
            <span style={{ display: 'block', fontSize: 13, fontWeight: 800, color: selected ? '#ff385c' : '#222', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{title}</span>
            <span style={{ display: 'block', marginTop: 5, fontSize: 12, color: '#6a6a6a', lineHeight: 1.35, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{subtitle || '단위별 시세 보기'}</span>
          </button>
        );
      })}
    </div>
  );
};

const MarketComparisonFallback = ({ message, onRetry }) => (
  <div style={{
    border: '1px dashed rgba(255,56,92,0.30)', borderRadius: 18, padding: 22, background: '#fff8f9',
    display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap', minWidth: 0,
  }}>
    <div style={{ minWidth: 0 }}>
      <p style={{ fontSize: 15, fontWeight: 800, color: '#222', marginBottom: 6 }}>{message}</p>
      <p style={{ fontSize: 13, color: '#6a6a6a', lineHeight: 1.55 }}>공고 조건과 주변 거래를 맞춰 볼 수 있을 때 다시 안내해 드릴게요.</p>
    </div>
    {onRetry && (
      <button
        type="button"
        onClick={onRetry}
        style={{ padding: '10px 14px', borderRadius: 10, border: '1px solid #ff385c', background: '#fff', color: '#ff385c', fontSize: 13, fontWeight: 800, cursor: 'pointer', flexShrink: 0 }}
      >
        다시 불러오기
      </button>
    )}
  </div>
);

const MarketSupportInfo = ({ items, style }) => {
  if (items.length === 0) return null;

  return (
    <div style={{ borderRadius: 18, padding: 16, background: 'rgba(255,255,255,0.78)', border: '1px solid rgba(0,0,0,0.06)', minWidth: 0, ...style }}>
      <div style={{ display: 'grid', gap: 10 }}>
        {items.map(([label, value]) => (
          <div key={label} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start', minWidth: 0 }}>
            <span style={{ fontSize: 12, color: '#6a6a6a', flexShrink: 0 }}>{label}</span>
            <strong style={{ fontSize: 12, color: '#222', textAlign: 'right', lineHeight: 1.45, minWidth: 0 }}>{value}</strong>
          </div>
        ))}
      </div>
    </div>
  );
};

const MarketMetricCard = ({ definition, metric }) => {
  const ratio = getMarketRatio(metric);
  const preferredRatio = getPreferredMarketRatio(metric);
  const unit = toFiniteNumber(metric.unitAmount);
  const market = toFiniteNumber(metric.marketAmount);
  const isAboveMarket = preferredRatio != null ? preferredRatio > 100 : unit != null && market != null && market > 0 && unit > market;
  const ratioLabel = ratio != null ? `${Math.round(ratio)}%${isAboveMarket ? ' 이상' : ''}` : '확인 중';

  return (
    <div style={{ border: '1px solid rgba(0,0,0,0.08)', borderRadius: 18, padding: 20, background: '#fff', minWidth: 0, boxShadow: 'rgba(0,0,0,0.025) 0px 8px 22px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 14 }}>
        <p style={{ fontSize: 15, fontWeight: 800, color: '#222' }}>{definition.label}</p>
        <span style={{ width: 10, height: 10, borderRadius: 999, background: definition.accent, flexShrink: 0 }} />
      </div>

      <div style={{ display: 'grid', gap: 8, marginBottom: 14 }}>
        {[
          [definition.marketLabel, formatMarketAmount(metric.marketAmount), '#6a6a6a'],
          [definition.unitLabel, formatMarketAmount(metric.unitAmount), '#222'],
        ].map(([label, value, color]) => (
          <div key={label} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'baseline', minWidth: 0 }}>
            <span style={{ fontSize: 12, color: '#6a6a6a', flexShrink: 0 }}>{label}</span>
            <strong style={{ fontSize: 16, color, textAlign: 'right', minWidth: 0 }}>{value}</strong>
          </div>
        ))}
      </div>

      <div style={{ marginBottom: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, marginBottom: 7 }}>
          <span style={{ fontSize: 12, color: '#6a6a6a' }}>공고/주변 비율</span>
          <span style={{ fontSize: 12, fontWeight: 800, color: definition.accent }}>{ratioLabel}</span>
        </div>
        <div style={{ height: 7, borderRadius: 999, background: '#f2f2f2', overflow: 'hidden' }}>
          <div style={{ width: `${ratio ?? 0}%`, height: '100%', borderRadius: 999, background: definition.accent, transition: 'width 0.25s ease' }} />
        </div>
      </div>

      <p style={{ fontSize: 12, color: '#222', lineHeight: 1.6, fontWeight: 500 }}>{getMarketMetricInterpretation(definition, metric)}</p>
    </div>
  );
};

const MarketComparisonSection = ({ sectionTitleStyle, announcement, units, selectedUnitId, onSelectUnit, comparison, loading, error, onRetry, dealRange }) => {
  const selectableUnits = units.filter((unit) => unit.unitId != null);
  const selectedUnit = selectableUnits.find((unit) => String(unit.unitId) === String(selectedUnitId)) || selectableUnits[0] || null;
  const normalizedComparison = normalizeMarketComparison(comparison);
  const sourceType = selectedUnit ? resolveMarketSourceType(selectedUnit, announcement) : 'APT_RENT';
  const sourceLabel = getMarketSourceLabel(sourceType);
  const availableMetrics = getAvailableMarketMetrics(normalizedComparison);
  const statusMessage = getMarketStatusMessage(normalizedComparison?.status, availableMetrics.length > 0);
  const summaryMetric = availableMetrics[0];
  const sampleCount = getMarketSampleCount(normalizedComparison?.snapshot);
  const comparisonSourceLabel = getUserSafeMarketText(normalizedComparison?.sourceLabel || normalizedComparison?.snapshot?.sourceLabel) || sourceLabel;
  const comparisonTypeLabel = getUserSafeMarketText(normalizedComparison?.comparisonType || normalizedComparison?.snapshot?.comparisonType);
  const regionLabel = getUserSafeMarketText(normalizedComparison?.regionName || normalizedComparison?.snapshot?.regionName) || (selectedUnit ? getMarketRegionLabel(selectedUnit, announcement) : '');
  const supportItems = [
    ['기준 지역', regionLabel],
    ['비교 기준', [comparisonSourceLabel, comparisonTypeLabel, dealRange.label].filter(Boolean).join(' · ')],
    ['표본 수', normalizedComparison?.snapshot ? (sampleCount != null ? `${sampleCount}건` : '확인 중') : ''],
    ['갱신일', getMarketUpdatedLabel(normalizedComparison?.snapshot)],
  ].filter(([, value]) => value);

  return (
    <div style={{ marginBottom: 40 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 16, marginBottom: 18, paddingBottom: 12, borderBottom: '1px solid rgba(0,0,0,0.08)', flexWrap: 'wrap' }}>
        <div style={{ minWidth: 0 }}>
          <h2 style={{ ...sectionTitleStyle, marginBottom: 6, paddingBottom: 0, borderBottom: 'none' }}>시세 비교</h2>
          <p style={{ fontSize: 13, color: '#6a6a6a', lineHeight: 1.5 }}>선택한 공급 단위와 주변 거래 금액을 함께 비교해 보세요.</p>
        </div>
      </div>

      {selectableUnits.length === 0 ? (
        <MarketComparisonFallback message="공급 단위 정보가 없어 시세 비교를 준비하기 어렵습니다." />
      ) : (
        <div style={{ borderRadius: 22, padding: 20, background: '#fff', border: '1px solid rgba(0,0,0,0.08)', boxShadow: 'rgba(0,0,0,0.03) 0px 12px 30px', minWidth: 0 }}>
          <MarketUnitSelector units={selectableUnits} selectedUnitId={selectedUnit?.unitId} onSelect={onSelectUnit} />

          {loading && !normalizedComparison ? (
            <div style={{ borderRadius: 18, padding: 22, background: '#fff', border: '1px solid rgba(0,0,0,0.06)' }}>
              <p style={{ fontSize: 15, fontWeight: 800, color: '#222', marginBottom: 8 }}>시세 정보를 확인하고 있습니다.</p>
              <div style={{ height: 10, borderRadius: 999, background: 'linear-gradient(90deg, #f2f2f2 0%, #fff0f3 50%, #f2f2f2 100%)' }} />
            </div>
          ) : error && !normalizedComparison ? (
            <>
              <MarketComparisonFallback message="시세 정보를 불러오지 못했습니다." onRetry={onRetry} />
              <MarketSupportInfo items={supportItems} style={{ marginTop: 14 }} />
            </>
          ) : !normalizedComparison ? (
            <>
              <MarketComparisonFallback message="시세 정보를 준비 중입니다." />
              <MarketSupportInfo items={supportItems} style={{ marginTop: 14 }} />
            </>
          ) : statusMessage ? (
            <>
              <MarketComparisonFallback message={statusMessage} />
              <MarketSupportInfo items={supportItems} style={{ marginTop: 14 }} />
            </>
          ) : (
            <>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16, alignItems: 'stretch', marginBottom: 16 }}>
                <div style={{ borderRadius: 18, padding: 20, background: '#fff', color: '#222', minWidth: 0, overflow: 'hidden', position: 'relative', border: '1px solid rgba(0,0,0,0.08)' }}>
                  <div style={{ position: 'absolute', right: -44, top: -54, width: 150, height: 150, borderRadius: '50%', background: 'rgba(255,56,92,0.08)' }} />
                  <p style={{ fontSize: 12, fontWeight: 800, color: '#6a6a6a', marginBottom: 10 }}>한눈에 보기</p>
                  <p style={{ position: 'relative', fontSize: 18, fontWeight: 700, lineHeight: 1.45, letterSpacing: '-0.4px', color: '#222' }}>{getMarketMetricInterpretation(summaryMetric.definition, summaryMetric.metric)}</p>
                </div>
                <MarketSupportInfo items={supportItems} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 14 }}>
                {availableMetrics.map(({ definition, metric }) => (
                  <MarketMetricCard key={definition.key} definition={definition} metric={metric} />
                ))}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default function AnnouncementDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const api = useApi();
  const { isLoggedIn } = useAuth();
  const toast = useToast();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [favorited, setFavorited] = useState(false);
  const [selectedMarketUnitId, setSelectedMarketUnitId] = useState(null);
  const [marketComparisons, setMarketComparisons] = useState({});
  const [marketLoadingByUnit, setMarketLoadingByUnit] = useState({});
  const [marketErrorsByUnit, setMarketErrorsByUnit] = useState({});
  const [marketRequestVersion, setMarketRequestVersion] = useState(0);
  const marketDealRange = getMarketDealYmRange();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const res = await api.get(`/api/announcements/${id}`);
        if (res.ok) {
          const d = await res.json();
          setData(d);
        }
        if (isLoggedIn) {
          const favRes = await api.get(`/api/me/favorites/${id}/exists`);
          if (favRes.ok) {
            const f = await favRes.json();
            setFavorited(f.exists || f === true);
          }
        }
      } catch { /* handled */ }
      setLoading(false);
    };
    load();
  }, [id, isLoggedIn]);

  useEffect(() => {
    setSelectedMarketUnitId(null);
    setMarketComparisons({});
    setMarketLoadingByUnit({});
    setMarketErrorsByUnit({});
    setMarketRequestVersion(0);
  }, [id]);

  useEffect(() => {
    const unitsWithId = (Array.isArray(data?.units) ? data.units : []).filter((unit) => unit.unitId != null);

    setSelectedMarketUnitId((current) => {
      if (unitsWithId.length === 0) return null;
      if (current != null && unitsWithId.some((unit) => String(unit.unitId) === String(current))) return current;
      return unitsWithId[0].unitId;
    });
  }, [data]);

  useEffect(() => {
    const hasSelectedMarketUnit = selectedMarketUnitId != null;
    if (!data || !hasSelectedMarketUnit) return undefined;

    const unitsWithId = (Array.isArray(data.units) ? data.units : []).filter((unit) => unit.unitId != null);
    const selectedUnit = unitsWithId.find((unit) => String(unit.unitId) === String(selectedMarketUnitId));
    if (!selectedUnit) return undefined;

    const cacheKey = String(selectedUnit.unitId);
    const cachedComparison = marketComparisons[cacheKey];
    const isLoadingCurrentUnit = marketLoadingByUnit[cacheKey] === id;
    const hasCurrentError = marketErrorsByUnit[cacheKey]?.announcementId === id;
    if ((cachedComparison?.fetched && cachedComparison.announcementId === id) || isLoadingCurrentUnit || hasCurrentError) return undefined;

    const fetchMarketComparison = async () => {
      const sourceType = resolveMarketSourceType(selectedUnit, data);
      const endpoint = `/api/announcements/${id}/units/${selectedUnit.unitId}/market-comparison?sourceType=${encodeURIComponent(sourceType)}&dealYmFrom=${marketDealRange.dealYmFrom}&dealYmTo=${marketDealRange.dealYmTo}`;

      setMarketLoadingByUnit((prev) => ({ ...prev, [cacheKey]: id }));
      setMarketErrorsByUnit((prev) => {
        if (!prev[cacheKey]) return prev;
        const next = { ...prev };
        delete next[cacheKey];
        return next;
      });

      try {
        const response = await api.get(endpoint);
        if (!response.ok) throw new Error('market comparison request failed');
        const payload = await response.json();

        setMarketComparisons((prev) => ({
          ...prev,
          [cacheKey]: {
            fetched: true,
            data: normalizeMarketComparison(payload),
            dealRange: marketDealRange,
            announcementId: id,
          },
        }));
      } catch {
        setMarketErrorsByUnit((prev) => ({ ...prev, [cacheKey]: { message: '시세 정보를 불러오지 못했습니다.', announcementId: id } }));
      } finally {
        setMarketLoadingByUnit((prev) => {
          if (prev[cacheKey] !== id) return prev;
          return { ...prev, [cacheKey]: false };
        });
      }
    };

    fetchMarketComparison();
  }, [data, id, selectedMarketUnitId, marketRequestVersion, marketDealRange.dealYmFrom, marketDealRange.dealYmTo]);

  const retryMarketComparison = () => {
    if (!(selectedMarketUnitId != null)) return;
    const cacheKey = String(selectedMarketUnitId);

    setMarketComparisons((prev) => {
      if (!prev[cacheKey]) return prev;
      const next = { ...prev };
      delete next[cacheKey];
      return next;
    });
    setMarketErrorsByUnit((prev) => {
      if (!prev[cacheKey]) return prev;
      const next = { ...prev };
      delete next[cacheKey];
      return next;
    });
    setMarketRequestVersion((version) => version + 1);
  };

  const toggleFavorite = async () => {
    if (!isLoggedIn) { navigate('/login'); return; }
    try {
      if (favorited) {
        await api.del(`/api/me/favorites/${id}`);
        setFavorited(false);
        toast('즐겨찾기가 해제되었습니다');
      } else {
        await api.post('/api/me/favorites', { announcementId: Number(id) });
        setFavorited(true);
        toast('즐겨찾기에 추가되었습니다');
      }
    } catch { toast('오류가 발생했습니다', 'error'); }
  };

  if (loading) return <LoadingSpinner />;
  if (!data) return <div style={{ textAlign: 'center', padding: 80, color: '#6a6a6a' }}>공고를 찾을 수 없습니다.</div>;

  const a = data;
  const dday = calcDday(a.applicationEndDate);
  const ddayColor = getDdayColor(a.applicationEndDate);
  const val = (v) => v ?? '-';
  const amount = (v) => v != null ? `${formatPrice(v)} 만원` : '-';
  const area = (unit) => unit.exclusiveAreaText || (unit.exclusiveAreaValue != null ? `${unit.exclusiveAreaValue}㎡` : '-');
  const salePrice = (unit) => {
    if (unit.salePriceMin != null && unit.salePriceMax != null) return `${formatPrice(unit.salePriceMin)} ~ ${formatPrice(unit.salePriceMax)} 만원`;
    if (unit.salePriceMin != null) return `${formatPrice(unit.salePriceMin)} 만원부터`;
    if (unit.salePriceMax != null) return `${formatPrice(unit.salePriceMax)} 만원까지`;
    return '-';
  };
  const units = Array.isArray(a.units) ? a.units : [];
  const marketUnits = units.filter((unit) => unit.unitId != null);
  const activeMarketUnit = marketUnits.find((unit) => String(unit.unitId) === String(selectedMarketUnitId)) || marketUnits[0] || null;
  const activeMarketUnitId = activeMarketUnit?.unitId ?? selectedMarketUnitId;
  const activeMarketKey = activeMarketUnitId != null ? String(activeMarketUnitId) : null;
  const activeMarketCache = activeMarketKey && marketComparisons[activeMarketKey]?.announcementId === id ? marketComparisons[activeMarketKey] : null;
  const activeMarketComparison = activeMarketCache?.data || activeMarketUnit?.marketComparison || null;
  const activeMarketLoading = activeMarketKey ? marketLoadingByUnit[activeMarketKey] === id : false;
  const activeMarketError = activeMarketKey && marketErrorsByUnit[activeMarketKey]?.announcementId === id ? marketErrorsByUnit[activeMarketKey].message : '';

  const infoItems = [
    ['공급기관', val(a.providerName)],
    ['단지명', val(a.complexName)],
    ['주소', val(a.fullAddress)],
    ['공급유형', val(a.supplyType)],
    ['주택유형', val(a.houseType)],
    ['난방방식', val(a.heatingType)],
  ];

  return (
    <div>
      <div style={S.layout}>
        {/* Left Content */}
        <div>
          <button style={S.backBtn} onClick={() => navigate('/announcements')}>
            <svg viewBox="0 0 24 24" fill="none" stroke="#6a6a6a" strokeWidth="2" width="16" height="16">
              <path d="M19 12H5M12 5l-7 7 7 7" />
            </svg>
            목록으로
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <StatusBadge status={a.noticeStatus} />
            {a.providerName && <span style={{ fontSize: 14, color: '#6a6a6a' }}>{a.providerName}</span>}
          </div>

          <h1 style={S.title}>{a.noticeName}</h1>

          <div style={{ display: 'flex', gap: 8, marginBottom: 32 }}>
            <button onClick={toggleFavorite}
              style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px', borderRadius: 8, border: '1px solid #c1c1c1', background: '#fff', cursor: 'pointer', fontSize: 14, fontWeight: 500, color: '#222' }}>
              <svg viewBox="0 0 24 24" fill={favorited ? '#ff385c' : 'none'} stroke={favorited ? '#ff385c' : '#222'} strokeWidth="2" width="16" height="16">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
              {favorited ? '즐겨찾기됨' : '즐겨찾기 추가'}
            </button>
            {a.sourceUrl && (
              <a href={a.sourceUrl} target="_blank" rel="noopener noreferrer"
                style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 16px', borderRadius: 8, border: '1px solid #c1c1c1', background: '#fff', fontSize: 14, fontWeight: 500, color: '#222' }}>
                <svg viewBox="0 0 24 24" fill="none" stroke="#222" strokeWidth="2" width="14" height="14">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /><polyline points="15 3 21 3 21 9" /><line x1="10" y1="14" x2="21" y2="3" />
                </svg>
                원문 보기
              </a>
            )}
          </div>

          {/* Application Period */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 40 }}>
            <div style={{ padding: 20, borderRadius: 16, background: '#fff', border: '1px solid rgba(0,0,0,0.08)' }}>
              <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 8 }}>신청 기간</p>
              <p style={{ fontSize: 15, fontWeight: 600, color: '#222' }}>
                {a.applicationStartDate || '-'} ~ {a.applicationEndDate || '-'}
              </p>
              {dday && <p style={{ fontSize: 13, fontWeight: 600, color: ddayColor, marginTop: 4 }}>마감까지 {dday}</p>}
            </div>
            <div style={{ padding: 20, borderRadius: 16, background: '#fff', border: '1px solid rgba(0,0,0,0.08)' }}>
              <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 8 }}>당첨자 발표일</p>
              <p style={{ fontSize: 15, fontWeight: 600, color: '#222' }}>{a.winnerAnnouncementDate || '미정'}</p>
            </div>
          </div>

          {/* Basic Info */}
          <div style={{ marginBottom: 40 }}>
            <h2 style={S.sectionTitle}>기본 정보</h2>
            <div style={S.infoGrid}>
              {infoItems.map(([l, v]) => (
                <div key={l} style={S.infoItem}>
                  <span style={S.infoLabel}>{l}</span>
                  <span style={S.infoValue}>{v}</span>
                </div>
              ))}
            </div>
          </div>


          <LocationInfoSection
            sectionTitleStyle={S.sectionTitle}
            announcementName={a.complexName || a.noticeName}
            announcementAddress={a.fullAddress}
            units={units}
          />

          <MarketComparisonSection
            sectionTitleStyle={S.sectionTitle}
            announcement={a}
            units={units}
            selectedUnitId={activeMarketUnitId}
            onSelectUnit={setSelectedMarketUnitId}
            comparison={activeMarketComparison}
            loading={activeMarketLoading}
            error={activeMarketError}
            onRetry={retryMarketComparison}
            dealRange={activeMarketCache?.dealRange || marketDealRange}
          />

          {/* Cost Info */}
          <div style={{ marginBottom: 40 }}>
            <h2 style={S.sectionTitle}>비용 정보</h2>
            <div style={S.infoGrid}>
              {[
                ['보증금', a.depositAmount != null ? `${formatPrice(a.depositAmount)} 만원` : '-'],
                ['월세', a.monthlyRentAmount != null ? `${formatPrice(a.monthlyRentAmount)} 만원` : '-'],
                ['전용면적', a.exclusiveAreaText || (a.exclusiveAreaValue ? `${a.exclusiveAreaValue}㎡` : '-')],
              ].map(([l, v]) => (
                <div key={l} style={S.infoItem}>
                  <span style={S.infoLabel}>{l}</span>
                  <span style={S.infoValue}>{v}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Supply Info */}
          <div style={{ marginBottom: 40 }}>
            <h2 style={S.sectionTitle}>공급 정보</h2>
            <div style={S.infoGrid}>
              {[
                ['공급세대수', a.supplyHouseholdCount != null ? `${a.supplyHouseholdCount} 세대` : '-'],
                ['입주예정', val(a.moveInExpectedYm)],
              ].map(([l, v]) => (
                <div key={l} style={S.infoItem}>
                  <span style={S.infoLabel}>{l}</span>
                  <span style={S.infoValue}>{v}</span>
                </div>
              ))}
            </div>
          </div>

          {units.length > 0 && (
            <div style={{ marginBottom: 40 }}>
              <h2 style={S.sectionTitle}>공급 단위 정보</h2>
              <div style={S.unitGrid}>
                {units.map((unit, idx) => (
                  <div key={`${unit.unitOrder ?? idx}-${unit.complexName ?? ''}-${unit.houseType ?? ''}`} style={S.unitCard}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'flex-start', marginBottom: 12 }}>
                      <div>
                        <div style={{ fontSize: 15, fontWeight: 700, color: '#222', lineHeight: 1.4 }}>{val(unit.complexName)}</div>
                        <div style={{ fontSize: 12, color: '#6a6a6a', lineHeight: 1.5, marginTop: 4 }}>
                          {[unit.regionLevel1, unit.regionLevel2].filter(Boolean).join(' ') || '-'}
                        </div>
                      </div>
                      <span style={S.unitBadge}>#{unit.unitOrder ?? idx + 1}</span>
                    </div>
                    <div style={{ fontSize: 12, color: '#6a6a6a', lineHeight: 1.6, marginBottom: 10 }}>{val(unit.fullAddress)}</div>
                    {[
                      ['공급유형', val(unit.supplyType)],
                      ['주택유형', val(unit.houseType)],
                      ['전용면적', area(unit)],
                      ['보증금', amount(unit.depositAmount)],
                      ['월세', amount(unit.monthlyRentAmount)],
                      ['분양가', salePrice(unit)],
                      ['공급세대수', unit.supplyHouseholdCount != null ? `${unit.supplyHouseholdCount} 세대` : '-'],
                    ].filter(([, value]) => value !== '-').map(([label, value]) => (
                      <div key={label} style={S.unitMeta}>
                        <span style={{ fontSize: 12, color: '#6a6a6a', flexShrink: 0 }}>{label}</span>
                        <span style={{ fontSize: 13, color: '#222', fontWeight: 600, textAlign: 'right' }}>{value}</span>
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Guide Text */}
          {a.guideText && (
            <div style={{ marginBottom: 40 }}>
              <h2 style={S.sectionTitle}>안내사항</h2>
              <div style={{ fontSize: 14, color: '#222', lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>{a.guideText}</div>
            </div>
          )}

          {/* Contact */}
          {a.contactPhone && (
            <p style={{ fontSize: 14, color: '#6a6a6a' }}>문의전화: <strong style={{ color: '#222' }}>{a.contactPhone}</strong></p>
          )}

          {/* Source link */}
          {a.sourceUrl && (
            <div style={{ marginTop: 24 }}>
              <a href={a.sourceUrl} target="_blank" rel="noopener noreferrer"
                style={{
                  display: 'inline-flex', alignItems: 'center', gap: 8, padding: '12px 24px',
                  borderRadius: 12, background: '#222', color: '#fff', fontSize: 15, fontWeight: 600,
                }}>
                원문 공고 보기
                <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" width="14" height="14">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" /><polyline points="15 3 21 3 21 9" /><line x1="10" y1="14" x2="21" y2="3" />
                </svg>
              </a>
            </div>
          )}
        </div>

        {/* Right Sticky Card */}
        <div>
          <div style={S.stickyCard}>
            <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 4 }}>보증금</p>
            <p style={{ fontSize: 26, fontWeight: 700, color: '#222', letterSpacing: '-0.5px', marginBottom: 4 }}>
              {a.depositAmount != null ? `${formatPrice(a.depositAmount)}만원` : '-'}
            </p>
            <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 4 }}>월 임대료</p>
            <p style={{ fontSize: 26, fontWeight: 700, color: '#222', letterSpacing: '-0.5px', marginBottom: 20 }}>
              {a.monthlyRentAmount != null ? `${formatPrice(a.monthlyRentAmount)}만원` : '-'}
            </p>

            {dday && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 16px', borderRadius: 12, background: '#fff0f3', marginBottom: 20 }}>
                <span style={{ fontSize: 14 }}>&#9200;</span>
                <span style={{ fontSize: 13, fontWeight: 600, color: '#ff385c' }}>신청 마감 {dday}</span>
              </div>
            )}

            {a.sourceUrl ? (
              <a href={a.sourceUrl} target="_blank" rel="noopener noreferrer"
                style={{ ...S.applyBtn, display: 'flex', alignItems: 'center', justifyContent: 'center', textDecoration: 'none' }}>
                신청하러 가기
              </a>
            ) : (
              <button style={{ ...S.applyBtn, opacity: 0.5, cursor: 'default' }} disabled>신청 링크 없음</button>
            )}

            <button style={{ ...S.saveBtn, borderColor: favorited ? '#ff385c' : '#c1c1c1' }} onClick={toggleFavorite}>
              <svg viewBox="0 0 24 24" fill={favorited ? '#ff385c' : 'none'} stroke={favorited ? '#ff385c' : 'currentColor'} strokeWidth="2" width="16" height="16">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
              {favorited ? '저장됨' : '공고 저장하기'}
            </button>

            <div style={{ borderTop: '1px solid rgba(0,0,0,0.08)', paddingTop: 20 }}>
              {[
                ['공급유형', a.supplyType || '-'],
                ['주택유형', a.houseType || '-'],
                ['전용면적', a.exclusiveAreaText || (a.exclusiveAreaValue ? `${a.exclusiveAreaValue}㎡` : '-')],
                ['공급세대', a.supplyHouseholdCount ? `${a.supplyHouseholdCount}세대` : '-'],
                ['신청기간', a.applicationStartDate && a.applicationEndDate ? `${a.applicationStartDate} ~ ${a.applicationEndDate}` : '-', true],
                ['당첨자 발표', a.winnerAnnouncementDate || '미정'],
              ].map(([l, v, red]) => (
                <div key={l} style={S.quickRow}>
                  <span style={{ fontSize: 13, color: '#6a6a6a' }}>{l}</span>
                  <span style={{ fontSize: 13, fontWeight: 600, color: red ? '#ff385c' : '#222' }}>{v}</span>
                </div>
              ))}
            </div>

            {a.contactPhone && (
              <div style={{ marginTop: 20, padding: 16, background: '#f2f2f2', borderRadius: 12, textAlign: 'center' }}>
                <p style={{ fontSize: 12, color: '#6a6a6a', marginBottom: 2 }}>문의전화</p>
                <p style={{ fontSize: 20, fontWeight: 700, color: '#222', letterSpacing: 1 }}>{a.contactPhone}</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
