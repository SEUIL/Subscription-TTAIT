import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApi } from '../hooks/useApi';
import LoadingSpinner from '../components/common/LoadingSpinner';

const STAT_CARDS = [
  { key: 'pending', label: '검수 대기', code: 'PENDING', color: '#F59E0B', bg: '#FFFBEB' },
  { key: 'approved', label: '승인 완료', code: 'APPROVED', color: '#22C55E', bg: '#F0FDF4' },
  { key: 'corrected', label: '수정 확정', code: 'CORRECTED', color: '#3B82F6', bg: '#EFF6FF' },
  { key: 'rejected', label: '거절', code: 'REJECTED', color: '#EF4444', bg: '#FEF2F2' },
  { key: 'reImport', label: '재수집대기', code: 'RE_IMPORT', color: '#8B5CF6', bg: '#FAF5FF' },
  { key: 'totalAnnouncements', label: '전체 공고', code: 'TOTAL', color: '#6B7280', bg: '#F9FAFB' },
];

const TODAY_CARD = { key: 'processedToday', label: '오늘 처리', code: 'TODAY', color: '#ff385c', bg: '#fff0f3' };

export default function AdminDashboardPage() {
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(true);
  const api = useApi();
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/api/admin/review/stats');
        if (res.ok) setStats(await res.json());
      } catch { /* handled */ }
      setLoading(false);
    };
    load();
  }, []);

  if (loading) return <LoadingSpinner />;

  const getCount = (key) => stats[key] ?? 0;

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: '32px 24px 80px' }}>
      <h1 style={{ fontSize: 26, fontWeight: 700, color: '#222', marginBottom: 32 }}>관리자 대시보드</h1>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 16 }}>
        {[TODAY_CARD, ...STAT_CARDS.slice(0, 3)].map(card => (
          <div key={card.key}
            onClick={() => card.code === 'TODAY' ? navigate('/admin/review') : navigate(`/admin/review?status=${card.code}`)}
            style={{
              padding: 24, borderRadius: 16, background: card.bg, cursor: 'pointer',
              transition: 'transform 0.15s, box-shadow 0.15s', textAlign: 'center',
            }}
            onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-2px)'; e.currentTarget.style.boxShadow = 'var(--shadow-hover)'; }}
            onMouseLeave={e => { e.currentTarget.style.transform = 'none'; e.currentTarget.style.boxShadow = 'none'; }}
          >
            <p style={{ fontSize: 13, fontWeight: 600, color: card.color, marginBottom: 8 }}>{card.label}</p>
            <p style={{ fontSize: 32, fontWeight: 700, color: card.color }}>{getCount(card.key)}</p>
            <p style={{ fontSize: 11, color: '#6a6a6a', marginTop: 4 }}>{card.code}</p>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 }}>
        {STAT_CARDS.slice(3).map(card => (
          <div key={card.key}
            onClick={() => card.code === 'RE_IMPORT' ? navigate('/admin/review?status=RE_IMPORT') : navigate('/admin/review')}
            style={{
              padding: 24, borderRadius: 16, background: card.bg, cursor: 'pointer',
              transition: 'transform 0.15s', textAlign: 'center',
            }}
            onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-2px)'}
            onMouseLeave={e => e.currentTarget.style.transform = 'none'}
          >
            <p style={{ fontSize: 13, fontWeight: 600, color: card.color, marginBottom: 8 }}>{card.label}</p>
            <p style={{ fontSize: 32, fontWeight: 700, color: card.color }}>{getCount(card.key)}</p>
            <p style={{ fontSize: 11, color: '#6a6a6a', marginTop: 4 }}>{card.code}</p>
          </div>
        ))}
      </div>

      {/* Quick Menu */}
      <div style={{ background: '#fff', borderRadius: 20, padding: 28, boxShadow: 'var(--shadow-card)' }}>
        <h2 style={{ fontSize: 18, fontWeight: 700, color: '#222', marginBottom: 20 }}>빠른 메뉴</h2>
        {[
          ['검수 대기 목록 보기', '/admin/review?status=PENDING'],
          ['수동 공고 등록', '/admin/announcements/new'],
          ['LH 공고 수집', '/admin/import'],
        ].map(([label, path]) => (
          <div key={label}
            onClick={() => navigate(path)}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              padding: '16px 20px', borderRadius: 12, background: '#f9fafb', cursor: 'pointer',
              marginBottom: 8, transition: 'background 0.15s',
            }}
            onMouseEnter={e => e.currentTarget.style.background = '#f2f2f2'}
            onMouseLeave={e => e.currentTarget.style.background = '#f9fafb'}
          >
            <span style={{ fontSize: 14, fontWeight: 500, color: '#222' }}>{label}</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="#6a6a6a" strokeWidth="2" width="16" height="16">
              <path d="M9 18l6-6-6-6" />
            </svg>
          </div>
        ))}
      </div>
    </div>
  );
}
