import { useState, useEffect } from 'react';
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
  unitGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 16 },
  unitCard: { background: '#fff', border: '1px solid rgba(0,0,0,0.08)', borderRadius: 16, padding: 18 },
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

export default function AnnouncementDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const api = useApi();
  const { isLoggedIn } = useAuth();
  const toast = useToast();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [favorited, setFavorited] = useState(false);

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
                    ].map(([label, value]) => (
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
