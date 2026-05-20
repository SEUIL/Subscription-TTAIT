import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import Pagination from '../components/common/Pagination';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';

const TABS = [
  { value: '', label: '전체' },
  { value: 'PENDING', label: '대기중' },
  { value: 'APPROVED', label: '승인' },
  { value: 'CORRECTED', label: '수정확정' },
  { value: 'REJECTED', label: '거절' },
  { value: 'RE_IMPORT', label: '재수집' },
];

const STATUS_COLORS = {
  PENDING: { bg: '#FFFBEB', color: '#F59E0B' },
  APPROVED: { bg: '#F0FDF4', color: '#22C55E' },
  CORRECTED: { bg: '#EFF6FF', color: '#3B82F6' },
  REJECTED: { bg: '#FEF2F2', color: '#EF4444' },
  RE_IMPORT: { bg: '#FAF5FF', color: '#8B5CF6' },
};

const NOTICE_STATUS_COLORS = {
  OPEN: { bg: '#f0fdf4', color: '#166534' },
  UPCOMING: { bg: '#eff6ff', color: '#1d4ed8' },
  CLOSED: { bg: '#f2f2f2', color: '#6a6a6a' },
  ENDED: { bg: '#f2f2f2', color: '#6a6a6a' },
  DRAFT: { bg: '#fff7ed', color: '#c2410c' },
};

function fmt(value) {
  if (value === null || value === undefined || value === '') return '-';
  if (typeof value === 'number') return value.toLocaleString('ko-KR');
  return value;
}

function money(value) {
  return value != null ? `${Number(value).toLocaleString('ko-KR')}만` : '-';
}

function period(start, end) {
  if (!start && !end) return '-';
  return `${fmt(start)} ~ ${fmt(end)}`;
}

function MiniBadge({ children, tone = 'neutral' }) {
  const palette = {
    accent: ['#fff0f3', '#ff385c'],
    info: ['#eff6ff', '#1d4ed8'],
    success: ['#f0fdf4', '#166534'],
    warning: ['#fff7ed', '#c2410c'],
    neutral: ['#f2f2f2', '#6a6a6a'],
  }[tone];
  return <span style={{ display: 'inline-flex', borderRadius: 999, padding: '4px 9px', background: palette[0], color: palette[1], fontSize: 11, fontWeight: 800, whiteSpace: 'nowrap' }}>{children}</span>;
}

export default function AdminReviewListPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const api = useApi();

  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [reviews, setReviews] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const s = searchParams.get('status');
    if (s !== null) setStatus(s);
  }, [searchParams]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const params = new URLSearchParams();
        params.set('page', String(page - 1));
        params.set('size', '20');
        if (status) params.set('status', status);
        const res = await api.get(`/api/admin/review?${params}`);
        if (res.ok) {
          const data = await res.json();
          setReviews(data.content || []);
          setTotalPages(data.totalPages || 0);
        }
      } catch { /* handled */ }
      setLoading(false);
    };
    load();
  }, [page, status]);

  const changeStatus = (s) => {
    setStatus(s);
    setPage(1);
    navigate(`/admin/review${s ? `?status=${s}` : ''}`, { replace: true });
  };

  return (
    <div style={{ maxWidth: 1280, margin: '0 auto', padding: '32px 24px 80px' }}>
      <h1 style={{ fontSize: 26, fontWeight: 700, color: '#222', marginBottom: 24 }}>AI 파싱 검수</h1>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 4, marginBottom: 24, flexWrap: 'wrap' }}>
        {TABS.map(tab => (
          <button key={tab.value} onClick={() => changeStatus(tab.value)}
            style={{
              padding: '8px 20px', borderRadius: 8, border: 'none', cursor: 'pointer',
              fontSize: 14, fontWeight: status === tab.value ? 600 : 400,
              background: status === tab.value ? '#222' : '#f2f2f2',
              color: status === tab.value ? '#fff' : '#6a6a6a',
              transition: 'all 0.15s',
            }}>
            {tab.label}
          </button>
        ))}
      </div>

      {loading ? <LoadingSpinner /> : reviews.length === 0 ? (
        <EmptyState icon="📋" title="검수 항목이 없습니다" />
      ) : (
        <>
          {/* Table */}
          <div style={{ background: '#fff', borderRadius: 16, overflowX: 'auto', boxShadow: 'var(--shadow-card)' }}>
            <table style={{ width: '100%', minWidth: 1280, borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid rgba(0,0,0,0.08)' }}>
                  {['ID', '공고/단지/기관', '지역/주소', '접수 기간', '공고 상태', '유형', '금액', '세대/Unit', '검수 상태'].map(h => (
                    <th key={h} style={{ padding: '14px 16px', fontSize: 13, fontWeight: 600, color: '#6a6a6a', textAlign: 'left' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {reviews.map(r => {
                  const sc = STATUS_COLORS[r.reviewStatus] || { bg: '#f2f2f2', color: '#6a6a6a' };
                  const nc = NOTICE_STATUS_COLORS[r.noticeStatus] || { bg: '#f2f2f2', color: '#6a6a6a' };
                  return (
                    <tr key={r.announcementId || r.id}
                      onClick={() => navigate(`/admin/review/${r.announcementId || r.id}`)}
                      style={{
                        borderBottom: '1px solid rgba(0,0,0,0.04)', cursor: 'pointer',
                        background: r.reviewStatus === 'PENDING' ? '#FFFDF5' : 'transparent',
                        transition: 'background 0.15s',
                      }}
                      onMouseEnter={e => e.currentTarget.style.background = '#f9fafb'}
                      onMouseLeave={e => e.currentTarget.style.background = r.reviewStatus === 'PENDING' ? '#FFFDF5' : 'transparent'}
                    >
                      <td style={{ padding: '14px 16px', fontSize: 14, color: '#6a6a6a' }}>{r.announcementId || r.id}</td>
                      <td style={{ padding: '14px 16px', maxWidth: 360 }}>
                        <div style={{ fontSize: 14, fontWeight: 700, color: '#222', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.noticeName || '-'}</div>
                        <div style={{ fontSize: 12, color: '#222', fontWeight: 600, marginTop: 5 }}>{fmt(r.complexName)}</div>
                        <div style={{ fontSize: 12, color: '#6a6a6a', marginTop: 4 }}>{fmt(r.providerName)}</div>
                      </td>
                      <td style={{ padding: '14px 16px', fontSize: 13, color: '#222' }}>
                        <div style={{ fontWeight: 700 }}>{[r.regionLevel1, r.regionLevel2].filter(Boolean).join(' ') || '-'}</div>
                        <div style={{ color: '#6a6a6a', marginTop: 5, maxWidth: 220, lineHeight: 1.5 }}>{fmt(r.fullAddress)}</div>
                      </td>
                      <td style={{ padding: '14px 16px', fontSize: 13, color: '#222', lineHeight: 1.6 }}>
                        <div style={{ fontWeight: 700 }}>{period(r.applicationStartDate, r.applicationEndDate)}</div>
                        <div style={{ color: '#6a6a6a', marginTop: 3 }}>마감 {fmt(r.applicationEndDate)}</div>
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{ fontSize: 11, fontWeight: 700, color: '#6a6a6a', marginBottom: 6 }}>공고 상태</div>
                        <span style={{ padding: '4px 10px', borderRadius: 6, fontSize: 11, fontWeight: 700, background: nc.bg, color: nc.color }}>
                          {fmt(r.noticeStatus)}
                        </span>
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                          <MiniBadge tone="accent">{fmt(r.supplyType)}</MiniBadge>
                          <MiniBadge tone="info">{fmt(r.houseType)}</MiniBadge>
                        </div>
                      </td>
                      <td style={{ padding: '14px 16px', fontSize: 13, color: '#222', lineHeight: 1.6 }}>
                        <div>보증금 <b>{money(r.depositAmount)}</b></div>
                        <div>월세 <b>{money(r.monthlyRentAmount)}</b></div>
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                          <MiniBadge tone="success">{r.supplyHouseholdCount != null ? `${r.supplyHouseholdCount}세대` : '세대 -'}</MiniBadge>
                          <MiniBadge tone="neutral">{r.unitCount != null ? `unit ${r.unitCount}` : 'unit -'}</MiniBadge>
                        </div>
                      </td>
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{ fontSize: 11, fontWeight: 700, color: '#6a6a6a', marginBottom: 6 }}>검수 상태</div>
                        <span style={{ padding: '4px 10px', borderRadius: 6, fontSize: 11, fontWeight: 600, background: sc.bg, color: sc.color }}>
                          {r.reviewStatus}
                        </span>
                        <div style={{ fontSize: 12, color: '#6a6a6a', marginTop: 8 }}>{r.reviewedBy || '미검수'}</div>
                        <div style={{ fontSize: 12, color: '#6a6a6a', marginTop: 3 }}>
                          {r.reviewedAt ? new Date(r.reviewedAt).toLocaleString('ko-KR') : '-'}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
