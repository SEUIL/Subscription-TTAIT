import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import { useToast } from '../components/common/Toast';
import LoadingSpinner from '../components/common/LoadingSpinner';

const S = {
  container: { maxWidth: 1100, margin: '0 auto', padding: '32px 24px 80px' },
  backBtn: {
    display: 'inline-flex', alignItems: 'center', gap: 8, padding: '8px 16px', borderRadius: 8,
    border: 'none', background: 'transparent', cursor: 'pointer', fontSize: 14, fontWeight: 500, color: '#6a6a6a', marginBottom: 16,
  },
  card: { background: '#fff', borderRadius: 20, padding: 28, marginBottom: 24, boxShadow: 'var(--shadow-card)' },
  cardTitle: { fontSize: 18, fontWeight: 700, color: '#222', marginBottom: 20, paddingBottom: 12, borderBottom: '1px solid rgba(0,0,0,0.08)' },
  grid2: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 24 },
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: '#222', marginBottom: 8 },
  input: {
    width: '100%', height: 44, padding: '0 12px', border: '1.5px solid #c1c1c1', borderRadius: 12,
    fontSize: 14, color: '#222', background: '#fff',
  },
  select: {
    width: '100%', height: 44, padding: '0 12px', border: '1.5px solid #c1c1c1', borderRadius: 12,
    fontSize: 14, color: '#222', background: '#fff', cursor: 'pointer',
  },
  textarea: {
    width: '100%', height: 80, padding: '12px', border: '1.5px solid #c1c1c1', borderRadius: 12,
    fontSize: 14, color: '#222', background: '#fff', resize: 'vertical',
  },
  radioGroup: { display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 24 },
  radioLabel: (sel) => ({
    display: 'flex', alignItems: 'flex-start', gap: 12, padding: '16px 20px', borderRadius: 12,
    border: `1.5px solid ${sel ? '#222' : '#c1c1c1'}`, background: sel ? '#f9fafb' : '#fff',
    cursor: 'pointer', transition: 'all 0.15s',
  }),
  submitBtn: {
    padding: '14px 32px', borderRadius: 12, border: 'none', background: '#222', color: '#fff',
    fontSize: 15, fontWeight: 600, cursor: 'pointer', transition: 'background 0.2s',
  },
  tableWrap: { overflowX: 'auto', border: '1px solid rgba(0,0,0,0.06)', borderRadius: 16 },
  table: { width: '100%', minWidth: 980, borderCollapse: 'collapse' },
  th: { textAlign: 'left', padding: '12px 14px', background: '#fafafa', borderBottom: '1px solid rgba(0,0,0,0.08)', color: '#6a6a6a', fontSize: 12, fontWeight: 700, whiteSpace: 'nowrap' },
  td: { padding: '14px', borderBottom: '1px solid rgba(0,0,0,0.06)', fontSize: 13, verticalAlign: 'top', lineHeight: 1.5 },
};

const ACTIONS = [
  { value: 'APPROVE', label: '승인 (APPROVE)', desc: '검수 상태가 APPROVED가 되어 public API 노출 대상이 됩니다' },
  { value: 'CORRECT', label: '수정 (CORRECT)', desc: '아래에서 필드를 수정하고 CORRECTED 상태로 공개 대상이 됩니다' },
  { value: 'REJECT', label: '거절 (REJECT)', desc: '공고 부적합 또는 파싱 품질 불량' },
  { value: 'REIMPORT', label: '재수집 (REIMPORT)', desc: '필요 시 LH 후보/import 플로우에서 다시 처리합니다' },
];

const MARITAL_OPTIONS = [
  { value: 'ANY', label: 'ANY (무관)' },
  { value: 'SINGLE', label: 'SINGLE (미혼)' },
  { value: 'MARRIED', label: 'MARRIED (기혼)' },
  { value: 'ENGAGED', label: 'ENGAGED (예비신혼)' },
  { value: 'NEWLYWED', label: 'NEWLYWED (신혼)' },
];

function badgeStyle(kind, value) {
  if (kind === 'confidence') {
    if (value === 'HIGH') return ['#f0fdf4', '#166534'];
    if (value === 'LOW') return ['#fff7ed', '#c2410c'];
    return ['#eff6ff', '#1d4ed8'];
  }
  if (value === 'PDF_AI') return ['#fff7ed', '#c2410c'];
  if (value === 'MERGED') return ['#f0fdf4', '#166534'];
  if (value === 'LH_API') return ['#eff6ff', '#1d4ed8'];
  return ['#f2f2f2', '#6a6a6a'];
}

function Badge({ kind, value }) {
  const [bg, color] = badgeStyle(kind, value);
  return <span style={{ display: 'inline-flex', borderRadius: 999, padding: '5px 10px', background: bg, color, fontSize: 11, fontWeight: 800, whiteSpace: 'nowrap' }}>{value || '-'}</span>;
}

function fmt(value) {
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value === 'number') return value.toLocaleString('ko-KR');
  return value;
}

function boolLabel(value) {
  if (value === true) return '예';
  if (value === false) return '아니오';
  return '미확인';
}

function boolInputValue(value) {
  if (value === true) return 'true';
  if (value === false) return 'false';
  return '';
}

function boolFromInput(value) {
  if (value === 'true') return true;
  if (value === 'false') return false;
  return null;
}

function numberOrNull(value) {
  if (value === '' || value === null || value === undefined) return null;
  return Number(value);
}

function parseScheduleDetails(value) {
  if (!value) return { raw: '', entries: [] };
  const raw = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
  const parsed = typeof value === 'string' ? JSON.parse(value) : value;
  if (!parsed || typeof parsed !== 'object') return { raw, entries: [] };
  const entries = Array.isArray(parsed)
    ? parsed.map((item, idx) => [`일정 ${idx + 1}`, item])
    : Object.entries(parsed);
  return { raw, entries };
}

function ScheduleDetailsSection({ value }) {
  let details;
  try {
    details = parseScheduleDetails(value);
  } catch {
    details = { raw: typeof value === 'string' ? value : String(value ?? ''), entries: [] };
  }

  return (
    <div style={S.card}>
      <h2 style={S.cardTitle}>일정 세부 정보</h2>
      {details.entries.length > 0 ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 10, marginBottom: 14 }}>
          {details.entries.map(([key, item]) => (
            <div key={key} style={{ borderRadius: 14, background: '#fafafa', padding: 14 }}>
              <div style={{ fontSize: 12, fontWeight: 700, color: '#6a6a6a', marginBottom: 7 }}>{key}</div>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#222', lineHeight: 1.6, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                {typeof item === 'object' && item !== null ? JSON.stringify(item, null, 2) : fmt(item)}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p style={{ fontSize: 13, color: '#6a6a6a', margin: '0 0 14px' }}>구조화 가능한 일정 세부값이 없습니다.</p>
      )}
      <details>
        <summary style={{ cursor: 'pointer', color: '#222', fontSize: 13, fontWeight: 800 }}>JSON 원문 / 텍스트 보기</summary>
        <div style={{ marginTop: 10, borderRadius: 12, background: '#f8f8f8', color: '#6a6a6a', padding: 14, fontSize: 12, lineHeight: 1.7, whiteSpace: 'pre-wrap', maxHeight: 260, overflow: 'auto' }}>
          {details.raw || 'scheduleDetailsJson 없음'}
        </div>
      </details>
    </div>
  );
}

function AdminRawSection({ title, children, accent = false }) {
  return (
    <div style={{ ...S.card, border: accent ? '1px solid rgba(255,56,92,0.18)' : undefined, background: accent ? '#fffdfd' : '#fff' }}>
      <h2 style={S.cardTitle}>{title}</h2>
      {children}
    </div>
  );
}

function UnitRawDetails({ unit }) {
  const [open, setOpen] = useState(false);
  if (!unit.rawText && !unit.sourceUnitKey && !unit.salePriceRaw && !unit.unitId) return <span style={{ color: '#c1c1c1' }}>근거 없음</span>;
  return (
    <div>
      <button type="button" onClick={() => setOpen(v => !v)} style={{ border: 'none', borderRadius: 8, background: '#f2f2f2', color: '#222', padding: '7px 10px', fontSize: 12, fontWeight: 700, cursor: 'pointer' }}>
        {open ? '접기' : '원문 / 디버그 보기'}
      </button>
      {open && (
        <div style={{ marginTop: 10, maxWidth: 520, borderRadius: 12, background: '#f8f8f8', color: '#6a6a6a', padding: 12, fontSize: 12, lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
          {unit.unitId && <div style={{ marginBottom: 8, color: '#222', fontWeight: 700 }}>unitId: {unit.unitId}</div>}
          <div style={{ marginBottom: 8, color: '#222', fontWeight: 700 }}>지역: {[unit.regionLevel1, unit.regionLevel2].filter(Boolean).join(' ') || '-'}</div>
          {unit.exclusiveAreaValue != null && <div style={{ marginBottom: 8, color: '#222', fontWeight: 700 }}>exclusiveAreaValue: {unit.exclusiveAreaValue}</div>}
          {unit.salePriceRaw && <div style={{ marginBottom: 8, color: '#222', fontWeight: 700 }}>salePriceRaw: {unit.salePriceRaw}</div>}
          {unit.rawText || 'rawText 없음'}
          {unit.sourceUnitKey && <div style={{ marginTop: 8, color: '#222', fontWeight: 700 }}>sourceUnitKey: {unit.sourceUnitKey}</div>}
        </div>
      )}
    </div>
  );
}

function ReviewUnitsSection({ units = [] }) {
  const lowCount = units.filter(u => u.confidenceLevel === 'LOW').length;
  return (
    <div style={S.card}>
      <h2 style={S.cardTitle}>공급 단위 검수</h2>
      <p style={{ fontSize: 14, color: '#6a6a6a', lineHeight: 1.7, margin: '0 0 16px' }}>
        units[]는 <b>announcement_unit</b> 테이블 기반 관리자 검수용 데이터입니다. rawText, sourceUnitKey, salePriceRaw 등 원문/출처 필드는 admin-only 필드이므로 public 화면에 노출하면 안 됩니다.
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: 10, marginBottom: 16 }}>
        <div style={{ borderRadius: 14, background: '#fafafa', padding: 14 }}><div style={{ fontSize: 22, fontWeight: 800 }}>{units.length}</div><div style={{ color: '#6a6a6a', fontSize: 12, fontWeight: 700 }}>전체 units</div></div>
        <div style={{ borderRadius: 14, background: lowCount ? '#fff7ed' : '#f0fdf4', padding: 14 }}><div style={{ fontSize: 22, fontWeight: 800 }}>{lowCount}</div><div style={{ color: '#6a6a6a', fontSize: 12, fontWeight: 700 }}>LOW 신뢰도</div></div>
      </div>
      {lowCount > 0 && <div style={{ marginBottom: 16, borderRadius: 14, background: '#fff7ed', color: '#c2410c', padding: '14px 16px', fontSize: 13, fontWeight: 700 }}>LOW 신뢰도 unit은 원문 표와 보증금/월세/공급세대수를 대조해주세요.</div>}
      <div style={S.tableWrap}>
        <table style={S.table}>
          <thead>
            <tr>
              {['순서', '출처/근거', '단지/주소', '공급/주택 유형', '면적', '금액', '세대수', '관리자 근거'].map(h => <th key={h} style={S.th}>{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {units.map((unit, idx) => (
              <tr key={unit.id || unit.sourceUnitKey || idx}>
                <td style={S.td}>
                  <div style={{ fontWeight: 700 }}>{unit.unitOrder ?? idx + 1}</div>
                  <div style={{ color: '#6a6a6a', fontSize: 12, marginTop: 4 }}>unitId {fmt(unit.unitId)}</div>
                </td>
                <td style={S.td}>
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 8 }}>
                    <Badge kind="source" value={unit.unitSource} />
                    <Badge kind="confidence" value={unit.confidenceLevel} />
                  </div>
                  <div style={{ color: '#6a6a6a', fontSize: 12 }}>{unit.matchSource || '-'}</div>
                </td>
                <td style={S.td}>
                  <div style={{ fontWeight: 700 }}>{fmt(unit.complexName)}</div>
                  <div style={{ color: '#222', fontSize: 12, fontWeight: 600, marginTop: 4 }}>{[unit.regionLevel1, unit.regionLevel2].filter(Boolean).join(' ') || '-'}</div>
                  <div style={{ color: '#6a6a6a', fontSize: 12, marginTop: 4 }}>{fmt(unit.fullAddress)}</div>
                </td>
                <td style={S.td}>
                  <div>{fmt(unit.supplyTypeNormalized)} <span style={{ color: '#6a6a6a' }}>({fmt(unit.supplyTypeRaw)})</span></div>
                  <div style={{ marginTop: 6 }}>{fmt(unit.houseTypeNormalized)} <span style={{ color: '#6a6a6a' }}>({fmt(unit.houseTypeRaw)})</span></div>
                </td>
                <td style={S.td}>
                  <div>{fmt(unit.exclusiveAreaText)}</div>
                  <div style={{ color: '#6a6a6a', fontSize: 12, marginTop: 4 }}>전용 {unit.exclusiveAreaValue != null ? `${unit.exclusiveAreaValue}㎡` : '-'}</div>
                </td>
                <td style={S.td}>
                  <div>보증금: <b>{fmt(unit.depositAmount)}</b></div>
                  <div>월세: <b>{fmt(unit.monthlyRentAmount)}</b></div>
                  <div style={{ color: '#6a6a6a', fontSize: 12 }}>분양가: {fmt(unit.salePriceMin)} ~ {fmt(unit.salePriceMax)}</div>
                  {unit.salePriceRaw && <div style={{ color: '#ff385c', fontSize: 12, fontWeight: 700, marginTop: 4 }}>원문 가격 있음</div>}
                </td>
                <td style={S.td}>{fmt(unit.supplyHouseholdCount)}</td>
                <td style={S.td}><UnitRawDetails unit={unit} /></td>
              </tr>
            ))}
            {units.length === 0 && <tr><td style={{ ...S.td, textAlign: 'center', color: '#6a6a6a' }} colSpan="8">등록된 공급 단위가 없습니다.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default function AdminReviewDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const api = useApi();
  const toast = useToast();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState('');
  const [memo, setMemo] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [corrections, setCorrections] = useState({
    depositAmount: '', monthlyRentAmount: '', supplyHouseholdCount: '',
    ageMin: '', ageMax: '', maritalTargetType: 'ANY', marriageYearLimit: '',
    homelessRequired: '', lowIncomeRequired: '', elderlyRequired: '',
    elderlyAgeMin: '', childrenMinCount: '',
  });

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get(`/api/admin/review/${id}`);
        if (res.ok) {
          const d = await res.json();
          setData(d);
          setCorrections({
            depositAmount: d.depositAmount ?? '',
            monthlyRentAmount: d.monthlyRentAmount ?? '',
            supplyHouseholdCount: d.supplyHouseholdCount ?? '',
            ageMin: d.ageMin ?? '',
            ageMax: d.ageMax ?? '',
            maritalTargetType: d.maritalTargetType || 'ANY',
            marriageYearLimit: d.marriageYearLimit ?? '',
            homelessRequired: boolInputValue(d.homelessRequired),
            lowIncomeRequired: boolInputValue(d.lowIncomeRequired),
            elderlyRequired: boolInputValue(d.elderlyRequired),
            elderlyAgeMin: d.elderlyAgeMin ?? '',
            childrenMinCount: d.childrenMinCount ?? '',
          });
        }
      } catch { /* handled */ }
      setLoading(false);
    };
    load();
  }, [id, api]);

  const setCorr = (key, val) => setCorrections(prev => ({ ...prev, [key]: val }));

  const handleSubmit = async () => {
    if (!action) { toast('검수 액션을 선택해주세요', 'error'); return; }
    if ((action === 'CORRECT' || action === 'REJECT') && !memo.trim()) {
      toast('검수 메모를 입력해주세요', 'error'); return;
    }

    setSubmitting(true);
    try {
      const body = { action, note: memo.trim() || null };
      if (action === 'CORRECT') {
        body.corrections = {
          ...corrections,
          depositAmount: numberOrNull(corrections.depositAmount),
          monthlyRentAmount: numberOrNull(corrections.monthlyRentAmount),
          supplyHouseholdCount: numberOrNull(corrections.supplyHouseholdCount),
          ageMin: numberOrNull(corrections.ageMin),
          ageMax: numberOrNull(corrections.ageMax),
          marriageYearLimit: numberOrNull(corrections.marriageYearLimit),
          elderlyAgeMin: numberOrNull(corrections.elderlyAgeMin),
          childrenMinCount: numberOrNull(corrections.childrenMinCount),
          homelessRequired: boolFromInput(corrections.homelessRequired),
          lowIncomeRequired: boolFromInput(corrections.lowIncomeRequired),
          elderlyRequired: boolFromInput(corrections.elderlyRequired),
        };
      }
      const res = await api.post(`/api/admin/review/${id}`, body);
      if (res.ok) {
        toast('검수 처리가 완료되었습니다');
        navigate('/admin/review');
      } else {
        toast('검수 처리에 실패했습니다', 'error');
      }
    } catch {
      toast('서버 오류가 발생했습니다', 'error');
    }
    setSubmitting(false);
  };

  if (loading) return <LoadingSpinner />;
  if (!data) return <div style={{ textAlign: 'center', padding: 80, color: '#6a6a6a' }}>검수 데이터를 찾을 수 없습니다.</div>;

  const rawTextParts = [
    data.eligibilityRaw && `[자격조건]\n${data.eligibilityRaw}`,
    data.ageRawText && `[나이조건]\n${data.ageRawText}`,
    data.maritalRawText && `[혼인조건]\n${data.maritalRawText}`,
    data.childrenRawText && `[자녀조건]\n${data.childrenRawText}`,
    data.homelessRawText && `[무주택조건]\n${data.homelessRawText}`,
    data.incomeAssetCriteriaRaw && `[소득/자산기준]\n${data.incomeAssetCriteriaRaw}`,
    data.elderlyRawText && `[고령자조건]\n${data.elderlyRawText}`,
    data.specialSupplyRaw && `[특별공급]\n${data.specialSupplyRaw}`,
    data.depositMonthlyRentRaw && `[보증금/월세]\n${data.depositMonthlyRentRaw}`,
    data.supplyHouseholdCountRaw && `[공급세대수]\n${data.supplyHouseholdCountRaw}`,
  ].filter(Boolean).join('\n\n');

  const units = data.units || data.announcementUnits || [];
  const hasReviewHistory = data.reviewedBy || data.reviewedAt || data.reviewNote;
  const summaryItems = [
    ['원천', data.sourcePrimary, 'badge'],
    ['원천 공고 ID', data.sourceNoticeId],
    ['원천 URL', data.sourceNoticeUrl, 'link'],
    ['공고 상태', data.noticeStatus, 'badge'],
    ['공고 유형', data.noticeType],
    ['공고일', data.announcementDate],
    ['신청 시작일', data.applicationStartDate],
    ['신청 마감일', data.applicationEndDate],
    ['당첨자 발표일', data.winnerAnnouncementDate],
    ['시/군/구', data.regionLevel2],
    ['상세 주소', data.fullAddress],
    ['단지명', data.complexName],
    ['공급유형', data.supplyType],
    ['주택유형', data.houseType],
    ['Unit 수', data.unitCount ?? units.length],
  ];

  return (
    <div style={S.container}>
      <button style={S.backBtn} onClick={() => navigate('/admin/review')}>
        <svg viewBox="0 0 24 24" fill="none" stroke="#6a6a6a" strokeWidth="2" width="16" height="16"><path d="M19 12H5M12 5l-7 7 7 7" /></svg>
        검수 목록
      </button>

      <h1 style={{ fontSize: 22, fontWeight: 700, color: '#222', marginBottom: 8 }}>
        공고 검수 - ID: {id}
      </h1>
      <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 4 }}>공고명: {data.noticeName || '-'}</p>
      <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 4 }}>기관: {data.providerName || '-'} | 지역: {data.regionLevel1 || '-'}</p>
      <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 24 }}>
        현재 상태: <span style={{ fontWeight: 600, color: '#222' }}>{data.reviewStatus}</span>
        <span style={{ marginLeft: 10, color: '#6a6a6a' }}>APPROVED/CORRECTED 상태만 public API에 노출됩니다.</span>
      </p>

      <div style={S.card}>
        <h2 style={S.cardTitle}>공고 요약</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 12 }}>
          {summaryItems.map(([label, value, type]) => (
            <div key={label} style={{ borderRadius: 14, background: '#fafafa', padding: 14, minWidth: 0 }}>
              <div style={{ fontSize: 12, fontWeight: 700, color: '#6a6a6a', marginBottom: 7 }}>{label}</div>
              {type === 'link' ? (
                value ? <a href={value} target="_blank" rel="noreferrer" style={{ fontSize: 13, fontWeight: 700, color: '#222', wordBreak: 'break-all' }}>원문 열기</a> : <span style={{ fontSize: 14, color: '#222', fontWeight: 600 }}>-</span>
              ) : type === 'badge' ? (
                <Badge kind="source" value={value} />
              ) : (
                <div style={{ fontSize: 14, color: '#222', fontWeight: 600, lineHeight: 1.5, wordBreak: 'keep-all' }}>{fmt(value)}</div>
              )}
            </div>
          ))}
        </div>
      </div>

      <div style={S.card}>
        <h2 style={S.cardTitle}>AI 파싱 결과</h2>
        <div style={S.grid2}>
          <div>
            <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: '#222' }}>파싱된 조건</h3>
            {[
              ['보증금', data.depositAmount != null ? `${Number(data.depositAmount).toLocaleString()}만원` : null],
              ['월세', data.monthlyRentAmount != null ? `${Number(data.monthlyRentAmount).toLocaleString()}만원` : null],
              ['공급 세대수', data.supplyHouseholdCount != null ? `${data.supplyHouseholdCount}세대` : null],
              ['세대수 신뢰도', data.supplyHouseholdCountConfidence],
              ['세대수 산정 근거', data.supplyHouseholdCountBasis],
              ['최소 나이', data.ageMin],
              ['최대 나이', data.ageMax],
              ['혼인 조건', data.maritalTargetType],
              ['혼인 기간 제한', data.marriageYearLimit ? `${data.marriageYearLimit}년` : null],
              ['무주택 필수', boolLabel(data.homelessRequired)],
              ['저소득 필수', boolLabel(data.lowIncomeRequired)],
              ['고령자 필수', boolLabel(data.elderlyRequired)],
              ['고령자 기준나이', data.elderlyAgeMin ? `${data.elderlyAgeMin}세` : null],
              ['최소 자녀수', data.childrenMinCount],
            ].map(([l, v]) => (
              <div key={l} style={{ display: 'flex', padding: '8px 0', borderBottom: '1px solid rgba(0,0,0,0.04)' }}>
                <span style={{ width: 120, fontSize: 13, color: '#6a6a6a', flexShrink: 0 }}>{l}</span>
                <span style={{ fontSize: 14, fontWeight: 500, color: '#222' }}>{v ?? '-'}</span>
              </div>
            ))}
          </div>
          <div>
            <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: '#222' }}>원문 텍스트</h3>
            <div style={{ fontSize: 13, color: '#6a6a6a', lineHeight: 1.8, whiteSpace: 'pre-wrap', background: '#f9fafb', padding: 16, borderRadius: 12, maxHeight: 400, overflow: 'auto' }}>
              {rawTextParts || '원문 텍스트 없음'}
            </div>
            <details style={{ marginTop: 12 }}>
              <summary style={{ cursor: 'pointer', color: '#222', fontSize: 13, fontWeight: 800 }}>가격 원문 / 출처 근거 보기</summary>
              <div style={{ marginTop: 10, fontSize: 13, color: '#6a6a6a', lineHeight: 1.7, whiteSpace: 'pre-wrap', background: '#f9fafb', padding: 14, borderRadius: 12, maxHeight: 220, overflow: 'auto' }}>
                {data.salePriceRaw || 'salePriceRaw 없음'}
              </div>
            </details>
          </div>
        </div>
      </div>

      <ScheduleDetailsSection value={data.scheduleDetailsJson} />

      <AdminRawSection title="중요 안내 원문" accent>
        <div style={{ borderRadius: 14, background: '#fff0f3', color: '#222', padding: 16, fontSize: 13, lineHeight: 1.8, whiteSpace: 'pre-wrap', maxHeight: 180, overflow: 'auto' }}>
          {data.importantNotesRaw || 'importantNotesRaw 없음'}
        </div>
        <details style={{ marginTop: 12 }}>
          <summary style={{ cursor: 'pointer', color: '#ff385c', fontSize: 13, fontWeight: 800 }}>긴 원문 전체 보기</summary>
          <div style={{ marginTop: 10, borderRadius: 12, background: '#f8f8f8', color: '#6a6a6a', padding: 14, fontSize: 12, lineHeight: 1.7, whiteSpace: 'pre-wrap', maxHeight: 300, overflow: 'auto' }}>
            {data.importantNotesRaw || 'importantNotesRaw 없음'}
          </div>
        </details>
      </AdminRawSection>

      <ReviewUnitsSection units={units} />

      <div style={S.card}>
        <h2 style={S.cardTitle}>검수 액션</h2>

        <div style={S.radioGroup}>
          {ACTIONS.map(a => (
            <label key={a.value} style={S.radioLabel(action === a.value)} onClick={() => setAction(a.value)}>
              <input type="radio" name="action" checked={action === a.value} onChange={() => setAction(a.value)} style={{ marginTop: 2 }} />
              <div>
                <p style={{ fontSize: 14, fontWeight: 600, color: '#222', margin: '0 0 4px' }}>{a.label}</p>
                <p style={{ fontSize: 12, color: '#6a6a6a', margin: 0 }}>{a.desc}</p>
              </div>
            </label>
          ))}
        </div>

        {action === 'CORRECT' && (
          <div style={{ padding: 20, background: '#f9fafb', borderRadius: 16, marginBottom: 24 }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16 }}>
              <div><label style={S.label}>보증금 (만원)</label><input style={S.input} type="number" value={corrections.depositAmount} onChange={e => setCorr('depositAmount', e.target.value)} /></div>
              <div><label style={S.label}>월세 (만원)</label><input style={S.input} type="number" value={corrections.monthlyRentAmount} onChange={e => setCorr('monthlyRentAmount', e.target.value)} /></div>
              <div><label style={S.label}>공급 세대수</label><input style={S.input} type="number" value={corrections.supplyHouseholdCount} onChange={e => setCorr('supplyHouseholdCount', e.target.value)} /></div>
              <div><label style={S.label}>최소 나이</label><input style={S.input} type="number" value={corrections.ageMin} onChange={e => setCorr('ageMin', e.target.value)} /></div>
              <div><label style={S.label}>최대 나이</label><input style={S.input} type="number" value={corrections.ageMax} onChange={e => setCorr('ageMax', e.target.value)} /></div>
              <div><label style={S.label}>혼인 조건</label><select style={S.select} value={corrections.maritalTargetType} onChange={e => setCorr('maritalTargetType', e.target.value)}>{MARITAL_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}</select></div>
              <div><label style={S.label}>혼인 기간 제한 (년)</label><input style={S.input} type="number" value={corrections.marriageYearLimit} onChange={e => setCorr('marriageYearLimit', e.target.value)} /></div>
            </div>
            <div style={{ display: 'flex', gap: 24, marginTop: 16, flexWrap: 'wrap' }}>
              {[['homelessRequired', '무주택 필수'], ['lowIncomeRequired', '저소득 필수'], ['elderlyRequired', '고령자 필수']].map(([k, l]) => (
                <label key={k} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, color: '#222' }}>
                  {l}
                  <select style={{ ...S.select, width: 120, height: 38 }} value={corrections[k]} onChange={e => setCorr(k, e.target.value)}>
                    <option value="">변경 안 함</option>
                    <option value="true">예</option>
                    <option value="false">아니오</option>
                  </select>
                </label>
              ))}
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16, marginTop: 16 }}>
              <div><label style={S.label}>고령자 기준나이</label><input style={S.input} type="number" value={corrections.elderlyAgeMin} onChange={e => setCorr('elderlyAgeMin', e.target.value)} /></div>
              <div><label style={S.label}>최소 자녀수</label><input style={S.input} type="number" value={corrections.childrenMinCount} onChange={e => setCorr('childrenMinCount', e.target.value)} /></div>
            </div>
          </div>
        )}

        {(action === 'CORRECT' || action === 'REJECT') && (
          <div style={{ marginBottom: 24 }}>
            <label style={S.label}>검수 메모 (필수)</label>
            <textarea style={S.textarea} placeholder="검수 메모를 입력하세요" value={memo} onChange={e => setMemo(e.target.value)} />
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button style={{ ...S.submitBtn, opacity: submitting ? 0.7 : 1 }} onClick={handleSubmit} disabled={submitting} onMouseEnter={e => e.currentTarget.style.background = '#111'} onMouseLeave={e => e.currentTarget.style.background = '#222'}>
            {submitting ? '처리 중...' : '검수 처리 실행'}
          </button>
        </div>
      </div>

      {hasReviewHistory && (
        <div style={S.card}>
          <h2 style={S.cardTitle}>검수 이력</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <p style={{ fontSize: 14, color: '#222' }}>검수자: <strong>{data.reviewedBy || '검수자 미기록'}</strong></p>
            {data.reviewedAt && <p style={{ fontSize: 13, color: '#6a6a6a' }}>처리일: {new Date(data.reviewedAt).toLocaleString('ko-KR')}</p>}
            {data.reviewNote && <p style={{ fontSize: 13, color: '#6a6a6a' }}>메모: {data.reviewNote}</p>}
          </div>
        </div>
      )}
    </div>
  );
}
