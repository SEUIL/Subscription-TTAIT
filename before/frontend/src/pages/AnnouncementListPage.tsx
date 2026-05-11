import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import FilterBar from '../components/common/FilterBar';
import ListingCard from '../components/common/ListingCard';
import Pagination from '../components/common/Pagination';
import { getAnnouncements } from '../api/announcements';
import type { Announcement, Page } from '../types';

const PAGE_SIZE = 12;

export default function AnnouncementListPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const [data, setData] = useState<Page<Announcement> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const category = searchParams.get('category') ?? 'all';
  const keyword = searchParams.get('keyword') ?? '';
  const page = Number(searchParams.get('page') ?? '0');
  const sortBy = searchParams.get('sort') ?? '추천순';

  const setParam = (key: string, value: string) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (value) next.set(key, value); else next.delete(key);
      if (key !== 'page') next.delete('page');
      return next;
    });
  };

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const params: Record<string, string | number> = { page, size: PAGE_SIZE };
      if (category !== 'all') params.categories = category;
      if (keyword) params.keyword = keyword;
      const result = await getAnnouncements(params);
      setData(result);
    } catch {
      setError('공고를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, [category, keyword, page]);

  useEffect(() => { fetchData(); }, [fetchData]);

  return (
    <div>
      <FilterBar
        activeCategory={category}
        onCategoryChange={(id) => setParam('category', id === 'all' ? '' : id)}
        resultCount={data?.totalElements ?? 0}
        sortBy={sortBy}
        onSortChange={(s) => setParam('sort', s)}
      />

      <div style={{ maxWidth: 1280, margin: '0 auto', padding: '24px 24px 0' }}>
        {loading && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 24 }}>
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} style={{ borderRadius: 20, overflow: 'hidden', background: '#f2f2f2', paddingTop: '66.67%', animation: 'pulse 1.5s ease-in-out infinite' }} />
            ))}
          </div>
        )}

        {error && (
          <div style={{ textAlign: 'center', padding: '80px 0', color: '#6a6a6a' }}>
            <div style={{ fontSize: 40, marginBottom: 16 }}>😕</div>
            <p style={{ fontSize: 16, marginBottom: 8 }}>{error}</p>
            <button
              style={{ padding: '10px 24px', background: '#222', color: '#fff', border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: 14, fontWeight: 600 }}
              onClick={fetchData}
            >
              다시 시도
            </button>
          </div>
        )}

        {!loading && !error && data && (
          <>
            {data.content.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '80px 0', color: '#6a6a6a' }}>
                <div style={{ fontSize: 40, marginBottom: 16 }}>🔍</div>
                <p style={{ fontSize: 16 }}>조건에 맞는 공고가 없습니다.</p>
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 24 }}>
                {data.content.map((item) => (
                  <ListingCard key={item.announcementId} item={item} />
                ))}
              </div>
            )}

            <Pagination
              currentPage={page}
              totalPages={data.totalPages}
              onPageChange={(p) => setParam('page', String(p))}
            />
          </>
        )}
      </div>

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
        @media (max-width: 1024px) {
          .listing-grid { grid-template-columns: repeat(3, 1fr) !important; }
        }
        @media (max-width: 768px) {
          .listing-grid { grid-template-columns: repeat(2, 1fr) !important; }
        }
      `}</style>
    </div>
  );
}
