import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAdminStats } from '../../api/admin';
import type { AdminStats } from '../../api/admin';

const STATUS_CARDS = [
  { key: 'pending',   label: 'PENDING',   color: '#f59e0b', bg: '#fffbeb' },
  { key: 'approved',  label: 'APPROVED',  color: '#166534', bg: '#f0fdf4' },
  { key: 'corrected', label: 'CORRECTED', color: '#1d4ed8', bg: '#eff6ff' },
  { key: 'rejected',  label: 'REJECTED',  color: '#ff385c', bg: '#fff0f3' },
];

export default function AdminDashboardPage() {
  const navigate = useNavigate();
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAdminStats()
      .then(setStats)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      <h1 style={{ fontSize: 22, fontWeight: 700, color: '#222', marginBottom: 24 }}>대시보드</h1>

      {loading ? (
        <div style={{ color: '#6a6a6a', fontSize: 14 }}>불러오는 중...</div>
      ) : stats ? (
        <>
          {/* 요약 카드 */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 32 }}>
            <div style={{ background: '#fff', borderRadius: 16, padding: '20px 24px', boxShadow: 'rgba(0,0,0,0.04) 0px 2px 8px' }}>
              <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 8 }}>전체 공고</p>
              <p style={{ fontSize: 32, fontWeight: 700, color: '#222' }}>{stats.total.toLocaleString()}</p>
            </div>
            <div style={{ background: '#fff', borderRadius: 16, padding: '20px 24px', boxShadow: 'rgba(0,0,0,0.04) 0px 2px 8px' }}>
              <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 8 }}>오늘 처리</p>
              <p style={{ fontSize: 32, fontWeight: 700, color: '#ff385c' }}>{stats.todayProcessed.toLocaleString()}</p>
            </div>
            <div style={{ background: '#fff0f3', border: '1px solid rgba(255,56,92,0.2)', borderRadius: 16, padding: '20px 24px' }}>
              <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 8 }}>대기 중 (PENDING)</p>
              <p style={{ fontSize: 32, fontWeight: 700, color: '#ff385c' }}>{stats.pending.toLocaleString()}</p>
            </div>
          </div>

          {/* 상태별 */}
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#222', marginBottom: 16 }}>상태별 현황</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 32 }}>
            {STATUS_CARDS.map(({ key, label, color, bg }) => (
              <div
                key={key}
                style={{ background: bg, border: `1px solid ${color}30`, borderRadius: 14, padding: '16px 20px', cursor: 'pointer' }}
                onClick={() => navigate(`/admin/review?status=${key.toUpperCase()}`)}
              >
                <p style={{ fontSize: 11, fontWeight: 700, color, letterSpacing: '0.5px', marginBottom: 8 }}>{label}</p>
                <p style={{ fontSize: 28, fontWeight: 700, color: '#222' }}>{(stats[key as keyof AdminStats] as number).toLocaleString()}</p>
                <p style={{ fontSize: 12, color: '#6a6a6a', marginTop: 4 }}>클릭해서 보기 &rarr;</p>
              </div>
            ))}
          </div>

          {/* 바로가기 */}
          <h2 style={{ fontSize: 16, fontWeight: 700, color: '#222', marginBottom: 16 }}>바로가기</h2>
          <div style={{ display: 'flex', gap: 12 }}>
            <button
              style={{ padding: '12px 24px', background: '#ff385c', color: '#fff', border: 'none', borderRadius: 10, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
              onClick={() => navigate('/admin/review?status=PENDING')}
            >
              PENDING 리뷰하기
            </button>
            <button
              style={{ padding: '12px 24px', background: '#222', color: '#fff', border: 'none', borderRadius: 10, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
              onClick={() => navigate('/admin/import')}
            >
              LH 공고 임포트
            </button>
          </div>
        </>
      ) : (
        <p style={{ color: '#ff385c' }}>통계를 불러오지 못했습니다.</p>
      )}
    </div>
  );
}
