import { useState, useEffect, lazy, Suspense } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAnnouncementDetail } from '../api/announcements';
import type { AnnouncementDetail } from '../types';

const MiniMap = lazy(() => import('../components/common/MiniMap'));

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: '모집중', CLOSING: '마감임박', CLOSED: '마감', UPCOMING: '예정',
};
const STATUS_COLORS: Record<string, string> = {
  ACTIVE: '#ff385c', CLOSING: '#ff385c', CLOSED: '#6a6a6a', UPCOMING: '#1d4ed8',
};

function getDDay(endDate?: string) {
  if (!endDate) return '';
  const diff = Math.ceil((new Date(endDate).getTime() - Date.now()) / 86400000);
  if (diff < 0) return '마감';
  if (diff === 0) return '오늘 마감!';
  return `D-${diff}`;
}

const S = {
  sectionTitle: { fontSize: 20, fontWeight: 700, color: '#222', letterSpacing: '-0.3px', marginBottom: 20, paddingBottom: 12, borderBottom: '1px solid rgba(0,0,0,0.08)' } as React.CSSProperties,
  infoItem: { padding: '13px 0', borderBottom: '1px solid rgba(0,0,0,0.06)', display: 'flex', alignItems: 'flex-start', gap: 12 } as React.CSSProperties,
  infoLabel: { fontSize: 13, color: '#6a6a6a', fontWeight: 400, minWidth: 80, flexShrink: 0, paddingTop: 2 } as React.CSSProperties,
  infoValue: { fontSize: 14, color: '#222', fontWeight: 500, lineHeight: 1.5 } as React.CSSProperties,
  applyBtn: { width: '100%', height: 56, borderRadius: 12, background: '#ff385c', border: 'none', cursor: 'pointer', fontFamily: "'Noto Sans KR',sans-serif", fontSize: 16, fontWeight: 600, color: '#fff', marginBottom: 12, transition: 'background 0.2s' } as React.CSSProperties,
  saveBtn: { width: '100%', height: 48, borderRadius: 12, background: 'transparent', cursor: 'pointer', fontFamily: "'Noto Sans KR',sans-serif", fontSize: 15, fontWeight: 500, color: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, marginBottom: 24, transition: 'border-color 0.15s' } as React.CSSProperties,
  quickRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 0' } as React.CSSProperties,
};

export default function AnnouncementDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [item, setItem] = useState<AnnouncementDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getAnnouncementDetail(Number(id))
      .then(setItem)
      .catch(() => navigate('/'))
      .finally(() => setLoading(false));
  }, [id, navigate]);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 400, color: '#6a6a6a', fontSize: 14 }}>
        불러오는 중...
      </div>
    );
  }

  if (!item) return null;

  const dday = getDDay(item.recruitmentEndDate);
  const statusColor = STATUS_COLORS[item.status] ?? '#222';

  const INFO_ROWS = [
    ['공급기관', item.provider],
    ['공급유형', item.supplyType],
    ['주택유형', item.houseType],
    ['지역', `${item.regionLevel1} ${item.regionLevel2 ?? ''}`],
    item.totalUnits ? ['세대수', `${item.totalUnits}세대`] : null,
    item.recruitmentStartDate ? ['신청기간', `${item.recruitmentStartDate} ~ ${item.recruitmentEndDate}`] : null,
  ].filter(Boolean) as [string, string][];

  return (
    <div>
      {/* Hero */}
      <div style={{ width: '100%', height: 380, background: 'linear-gradient(135deg,#e8e8e8,#d0d0d0)', display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative', overflow: 'hidden' }}>
        {item.imageUrl ? (
          <img src={item.imageUrl} alt={item.title} style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <span style={{ fontSize: 90 }}>🏢</span>
        )}
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, rgba(0,0,0,0.3) 0%, transparent 50%)' }} />
        <div style={{ position: 'absolute', bottom: 20, left: 24, display: 'flex', gap: 8 }}>
          <span style={{ padding: '6px 14px', borderRadius: 20, fontSize: 13, fontWeight: 600, backdropFilter: 'blur(8px)', background: statusColor, color: '#fff' }}>
            {STATUS_LABELS[item.status] ?? item.status}
          </span>
          <span style={{ padding: '6px 14px', borderRadius: 20, fontSize: 13, fontWeight: 600, backdropFilter: 'blur(8px)', background: 'rgba(255,255,255,0.9)', color: '#222' }}>
            {item.supplyType}
          </span>
          {item.houseType && (
            <span style={{ padding: '6px 14px', borderRadius: 20, fontSize: 13, fontWeight: 600, backdropFilter: 'blur(8px)', background: 'rgba(255,255,255,0.9)', color: '#222' }}>
              {item.houseType}
            </span>
          )}
        </div>
      </div>

      {/* Content */}
      <div style={{ maxWidth: 1280, margin: '0 auto', padding: '40px 24px 80px', display: 'grid', gridTemplateColumns: '1fr 360px', gap: 64, alignItems: 'start' }}>
        {/* Left */}
        <div>
          <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 8 }}>{item.provider} · {item.regionLevel1} {item.regionLevel2 ?? ''}</p>
          <h1 style={{ fontSize: 26, fontWeight: 700, color: '#222', lineHeight: 1.35, letterSpacing: '-0.4px', marginBottom: 16 }}>{item.title}</h1>
          <div style={{ display: 'flex', gap: 6, marginBottom: 32, flexWrap: 'wrap' }}>
            <span style={{ padding: '5px 12px', borderRadius: 6, fontSize: 12, fontWeight: 600, background: '#fff0f3', color: '#ff385c', border: '1px solid rgba(255,56,92,0.2)' }}>
              {STATUS_LABELS[item.status] ?? item.status}
            </span>
            {item.categories.map((cat) => (
              <span key={cat} style={{ padding: '5px 12px', borderRadius: 6, fontSize: 12, fontWeight: 600, background: '#f2f2f2', color: '#222', border: '1px solid transparent' }}>{cat}</span>
            ))}
          </div>

          {/* 공고 정보 */}
          <div style={{ marginBottom: 40 }}>
            <h2 style={S.sectionTitle}>공고 정보</h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 0 }}>
              {INFO_ROWS.map(([l, v]) => (
                <div key={l} style={S.infoItem}>
                  <span style={S.infoLabel}>{l}</span>
                  <span style={S.infoValue}>{v}</span>
                </div>
              ))}
            </div>
          </div>

          {/* 신청 일정 */}
          {item.supplySchedule && item.supplySchedule.length > 0 && (
            <div style={{ marginBottom: 40 }}>
              <h2 style={S.sectionTitle}>신청 일정</h2>
              <div style={{ position: 'relative', paddingLeft: 24 }}>
                <div style={{ position: 'absolute', left: 8, top: 0, bottom: 0, width: 2, background: 'rgba(0,0,0,0.08)' }} />
                {item.supplySchedule.map((step, i) => {
                  const isActive = i === 1;
                  return (
                    <div key={i} style={{ position: 'relative', marginBottom: i < item.supplySchedule!.length - 1 ? 24 : 0 }}>
                      <div style={{ position: 'absolute', left: -20, top: 4, width: 12, height: 12, borderRadius: '50%', background: isActive ? '#ff385c' : '#d0d0d0', border: '2px solid #fff', boxShadow: `0 0 0 2px ${isActive ? '#ff385c' : '#d0d0d0'}` }} />
                      <p style={{ fontSize: 12, color: isActive ? '#ff385c' : '#6a6a6a', fontWeight: isActive ? 600 : 400, marginBottom: 2 }}>{step.date}</p>
                      <p style={{ fontSize: 14, fontWeight: isActive ? 700 : 600, color: '#222' }}>{step.stepName}</p>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* 위치 */}
          {item.latitude && item.longitude && (
            <div style={{ marginBottom: 40 }}>
              <h2 style={S.sectionTitle}>위치 안내</h2>
              <Suspense fallback={<div style={{ height: 240, background: '#f2f2f2', borderRadius: 16 }} />}>
                <MiniMap lat={item.latitude} lng={item.longitude} label={item.title} address={item.address ?? ''} />
              </Suspense>
            </div>
          )}

          {/* 설명 */}
          {item.description && (
            <div style={{ marginBottom: 40 }}>
              <h2 style={S.sectionTitle}>공고 내용</h2>
              <p style={{ fontSize: 14, color: '#444', lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>{item.description}</p>
            </div>
          )}
        </div>

        {/* Right - 스티키 카드 */}
        <div>
          <div style={{ position: 'sticky', top: 90, background: '#fff', borderRadius: 20, padding: 28, boxShadow: 'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px, rgba(0,0,0,0.1) 0px 4px 8px' }}>
            {item.deposit != null && (
              <>
                <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 4 }}>보증금</p>
                <p style={{ fontSize: 26, fontWeight: 700, color: '#222', letterSpacing: '-0.5px', marginBottom: 4 }}>{item.deposit.toLocaleString()}원</p>
              </>
            )}
            {item.monthlyRent != null && (
              <>
                <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 4 }}>월 임대료</p>
                <p style={{ fontSize: 26, fontWeight: 700, color: '#222', letterSpacing: '-0.5px', marginBottom: 20 }}>{item.monthlyRent.toLocaleString()}원</p>
              </>
            )}

            {dday && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 16px', borderRadius: 12, background: '#fff0f3', marginBottom: 20 }}>
                <span>⏰</span>
                <span style={{ fontSize: 13, fontWeight: 600, color: '#ff385c' }}>신청 마감 {dday} · {item.recruitmentEndDate}</span>
              </div>
            )}

            <button
              style={S.applyBtn}
              onMouseEnter={(e) => (e.currentTarget.style.background = '#e00b41')}
              onMouseLeave={(e) => (e.currentTarget.style.background = '#ff385c')}
            >
              신청하러 가기 &rarr;
            </button>
            <button
              style={{ ...S.saveBtn, border: `1px solid ${saved ? '#222' : '#c1c1c1'}` }}
              onClick={() => setSaved((s) => !s)}
            >
              <svg viewBox="0 0 24 24" fill={saved ? '#222' : 'none'} stroke={saved ? '#222' : 'currentColor'} strokeWidth="2" width="16" height="16">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
              {saved ? '저장됨' : '공고 저장하기'}
            </button>

            <div style={{ borderTop: '1px solid rgba(0,0,0,0.08)', paddingTop: 20 }}>
              {([
                ['공급유형', item.supplyType, false],
                ['주택유형', item.houseType, false],
                ['지역', `${item.regionLevel1} ${item.regionLevel2 ?? ''}`, false],
                ...(item.recruitmentStartDate ? [['신청기간', `${item.recruitmentStartDate} ~ ${item.recruitmentEndDate}`, true]] : []),
              ] as [string, string, boolean][]).map(([l, v, red]) => (
                <div key={l} style={S.quickRow}>
                  <span style={{ fontSize: 13, color: '#6a6a6a' }}>{l}</span>
                  <span style={{ fontSize: 13, fontWeight: 600, color: red ? '#ff385c' : '#222' }}>{v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
