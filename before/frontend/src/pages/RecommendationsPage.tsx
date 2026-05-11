import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ListingCard from '../components/common/ListingCard';
import Pagination from '../components/common/Pagination';
import { getRecommendations } from '../api/recommendations';
import { useAuth } from '../context/AuthContext';
import type { Announcement, Page } from '../types';

export default function RecommendationsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [data, setData] = useState<Page<Announcement> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    setLoading(true);
    setError('');
    getRecommendations(page)
      .then(setData)
      .catch(() => setError('추천 공고를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [page]);

  const profileIncomplete = !user?.profileCompleted;

  return (
    <div style={{ maxWidth: 1280, margin: '0 auto', padding: '40px 24px 80px' }}>
      {/* 헤더 */}
      <div style={{ marginBottom: 32 }}>
        <h1 style={{ fontSize: 26, fontWeight: 700, color: '#222', letterSpacing: '-0.4px', marginBottom: 8 }}>
          나에게 맞는 공고 ✨
        </h1>
        <p style={{ fontSize: 14, color: '#6a6a6a' }}>
          프로필 정보를 기반으로 맞춤 공고를 추천해드려요.
        </p>
      </div>

      {/* 프로필 미완성 배너 */}
      {profileIncomplete && (
        <div style={{ background: '#fff0f3', border: '1px solid rgba(255,56,92,0.2)', borderRadius: 16, padding: '20px 24px', marginBottom: 32, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}>
          <div>
            <p style={{ fontSize: 15, fontWeight: 700, color: '#222', marginBottom: 4 }}>프로필을 완성하면 더 정확한 추천을 받을 수 있어요</p>
            <p style={{ fontSize: 13, color: '#6a6a6a' }}>나이, 혼인 상태, 선호 카테고리를 설정해보세요.</p>
          </div>
          <button
            style={{ padding: '12px 20px', background: '#ff385c', color: '#fff', border: 'none', borderRadius: 10, fontSize: 14, fontWeight: 600, cursor: 'pointer', flexShrink: 0 }}
            onMouseEnter={(e) => (e.currentTarget.style.background = '#e00b41')}
            onMouseLeave={(e) => (e.currentTarget.style.background = '#ff385c')}
            onClick={() => navigate('/me')}
          >
            프로필 설정하기 &rarr;
          </button>
        </div>
      )}

      {/* 로딩 */}
      {loading && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 24 }}>
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} style={{ borderRadius: 20, background: '#f2f2f2', paddingTop: '66.67%', animation: 'pulse 1.5s ease-in-out infinite' }} />
          ))}
        </div>
      )}

      {/* 에러 */}
      {error && (
        <div style={{ textAlign: 'center', padding: '80px 0', color: '#6a6a6a' }}>
          <div style={{ fontSize: 40, marginBottom: 16 }}>😕</div>
          <p style={{ fontSize: 16, marginBottom: 16 }}>{error}</p>
          <button
            style={{ padding: '10px 24px', background: '#222', color: '#fff', border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: 14, fontWeight: 600 }}
            onClick={() => { setPage(0); }}
          >
            다시 시도
          </button>
        </div>
      )}

      {/* 결과 */}
      {!loading && !error && data && (
        <>
          {data.content.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '80px 0', color: '#6a6a6a' }}>
              <div style={{ fontSize: 40, marginBottom: 16 }}>🔍</div>
              <p style={{ fontSize: 16, fontWeight: 600, marginBottom: 8 }}>추천 공고가 없습니다</p>
              <p style={{ fontSize: 14, marginBottom: 24 }}>프로필 카테고리를 설정하면 맞춤 공고를 추천해드려요.</p>
              <button
                style={{ padding: '12px 24px', background: '#ff385c', color: '#fff', border: 'none', borderRadius: 10, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
                onClick={() => navigate('/me')}
              >
                프로필 설정하기
              </button>
            </div>
          ) : (
            <>
              <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 24 }}>
                총 <strong style={{ color: '#222', fontWeight: 700 }}>{data.totalElements}</strong>개의 맞춤 공고
              </p>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 24 }}>
                {data.content.map((item) => (
                  <ListingCard key={item.announcementId} item={item} />
                ))}
              </div>
              <Pagination currentPage={page} totalPages={data.totalPages} onPageChange={setPage} />
            </>
          )}
        </>
      )}

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
      `}</style>
    </div>
  );
}
