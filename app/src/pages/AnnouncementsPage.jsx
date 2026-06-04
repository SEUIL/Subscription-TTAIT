import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import AnnouncementCard from '../components/common/AnnouncementCard';
import Pagination from '../components/common/Pagination';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../components/common/Toast';

const S = {
  container: { maxWidth: 1200, margin: '0 auto', padding: '24px', display: 'flex', gap: 32, alignItems: 'flex-start' },
  sidebar: { width: 280, flexShrink: 0, position: 'sticky', top: 96, background: '#fff', borderRadius: 20, padding: 24, boxShadow: 'var(--shadow-card)' },
  main: { flex: 1, minWidth: 0 },
  sectionTitle: { fontSize: 14, fontWeight: 700, color: '#222', marginBottom: 12 },
  select: {
    width: '100%', height: 44, padding: '0 12px', border: '1.5px solid #c1c1c1', borderRadius: 12,
    fontSize: 14, color: '#222', background: '#fff', cursor: 'pointer', marginBottom: 16,
  },
  radioGroup: { display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 16 },
  radioLabel: { display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, color: '#222', cursor: 'pointer' },
  checkbox: { display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, color: '#222', cursor: 'pointer', marginBottom: 6 },
  rangeWrap: { marginBottom: 16 },
  rangeRow: { display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 },
  rangeInput: { width: '100%', height: 40, padding: '0 12px', border: '1.5px solid #c1c1c1', borderRadius: 8, fontSize: 13, color: '#222', background: '#fff' },
  filterActions: { display: 'flex', gap: 8, marginTop: 20 },
  resetBtn: {
    flex: 1, height: 44, borderRadius: 12, border: '1px solid #c1c1c1', background: '#fff',
    fontSize: 14, fontWeight: 500, color: '#6a6a6a', cursor: 'pointer',
  },
  applyBtn: {
    flex: 1, height: 44, borderRadius: 12, border: 'none', background: '#222', color: '#fff',
    fontSize: 14, fontWeight: 600, cursor: 'pointer',
  },
  searchBar: {
    display: 'flex', gap: 12, marginBottom: 20, alignItems: 'center',
  },
  searchInput: {
    flex: 1, height: 48, padding: '0 16px', border: '1.5px solid #c1c1c1', borderRadius: 12,
    fontSize: 14, color: '#222', background: '#fff',
  },
  sortSelect: {
    height: 48, padding: '0 12px', border: '1.5px solid #c1c1c1', borderRadius: 12,
    fontSize: 14, color: '#222', background: '#fff', cursor: 'pointer', minWidth: 140,
  },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 24 },
};

const STATUS_OPTIONS = [
  { value: '', label: '전체' },
  { value: 'OPEN', label: '모집중' },
  { value: 'SCHEDULED', label: '모집예정' },
  { value: 'CLOSED', label: '모집마감' },
];

const CATEGORY_OPTIONS = [
  { value: 'YOUTH', label: '청년' },
  { value: 'NEWLYWED', label: '신혼부부' },
  { value: 'HOMELESS', label: '무주택' },
  { value: 'ELDERLY', label: '고령자' },
  { value: 'LOW_INCOME', label: '저소득' },
  { value: 'MULTI_CHILD', label: '다자녀' },
];

export default function AnnouncementsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const api = useApi();
  const { isLoggedIn } = useAuth();
  const toast = useToast();

  const [announcements, setAnnouncements] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filter options from API
  const [regions, setRegions] = useState([]);
  const [regions2, setRegions2] = useState([]);
  const [supplyTypes, setSupplyTypes] = useState([]);
  const [houseTypes, setHouseTypes] = useState([]);
  const [providers, setProviders] = useState([]);

  // Filter state
  const [filters, setFilters] = useState({
    regionLevel1: searchParams.get('regionLevel1') || '',
    regionLevel2: searchParams.get('regionLevel2') || '',
    supplyType: searchParams.get('supplyType') || '',
    houseType: searchParams.get('houseType') || '',
    provider: searchParams.get('provider') || '',
    status: searchParams.get('status') || '',
    categories: searchParams.getAll('categories') || [],
    depositMin: searchParams.get('depositMin') || '',
    depositMax: searchParams.get('depositMax') || '',
    rentMin: searchParams.get('rentMin') || '',
    rentMax: searchParams.get('rentMax') || '',
  });
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '');
  const [sort, setSort] = useState(searchParams.get('sort') || 'deadline');
  const [page, setPage] = useState(Number(searchParams.get('page')) || 1);

  // Load filter options
  useEffect(() => {
    const loadFilters = async () => {
      try {
        const [r1, st, ht, pv] = await Promise.all([
          api.get('/api/filters/regions').then(r => r.ok ? r.json() : []),
          api.get('/api/filters/supply-types').then(r => r.ok ? r.json() : []),
          api.get('/api/filters/house-types').then(r => r.ok ? r.json() : []),
          api.get('/api/filters/providers').then(r => r.ok ? r.json() : []),
        ]);
        setRegions(r1?.items ?? (Array.isArray(r1) ? r1 : []));
        setSupplyTypes(st?.items ?? (Array.isArray(st) ? st : []));
        setHouseTypes(ht?.items ?? (Array.isArray(ht) ? ht : []));
        setProviders(pv?.items ?? (Array.isArray(pv) ? pv : []));
      } catch { /* filters optional */ }
    };
    loadFilters();
  }, []);

  // Load level2 regions when level1 changes
  useEffect(() => {
    if (!filters.regionLevel1) { setRegions2([]); return; }
    api.get(`/api/filters/regions/level2?level1=${encodeURIComponent(filters.regionLevel1)}`)
      .then(r => r.ok ? r.json() : [])
      .then(d => setRegions2(d?.items ?? (Array.isArray(d) ? d : [])))
      .catch(() => setRegions2([]));
  }, [filters.regionLevel1]);

  // Load announcements
  const loadAnnouncements = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.set('page', String(page - 1));
      params.set('size', '12');
      if (sort && sort !== 'deadline') params.set('sort', sort);
      if (keyword) params.set('keyword', keyword);
      if (filters.regionLevel1) params.set('regionLevel1', filters.regionLevel1);
      if (filters.regionLevel2) params.set('regionLevel2', filters.regionLevel2);
      if (filters.supplyType) params.set('supplyType', filters.supplyType);
      if (filters.houseType) params.set('houseType', filters.houseType);
      if (filters.provider) params.set('provider', filters.provider);
      if (filters.status) params.set('status', filters.status);
      filters.categories.forEach(c => params.append('categories', c));
      if (filters.depositMin) params.set('minDeposit', filters.depositMin);
      if (filters.depositMax) params.set('maxDeposit', filters.depositMax);
      if (filters.rentMin) params.set('minMonthlyRent', filters.rentMin);
      if (filters.rentMax) params.set('maxMonthlyRent', filters.rentMax);

      const res = await api.get(`/api/announcements?${params}`);
      if (res.ok) {
        const data = await res.json();
        setAnnouncements(data.content || []);
        setTotalPages(data.totalPages || 0);
        setTotalCount(data.totalElements || 0);
      }
    } catch { /* handled */ }
    setLoading(false);
  }, [page, keyword, sort, filters]);

  useEffect(() => { loadAnnouncements(); }, [loadAnnouncements]);

  // Sync filters to URL
  useEffect(() => {
    const params = new URLSearchParams();
    if (page > 1) params.set('page', String(page));
    if (keyword) params.set('keyword', keyword);
    if (sort && sort !== 'deadline') params.set('sort', sort);
    Object.entries(filters).forEach(([k, v]) => {
      if (k === 'categories') {
        v.forEach(c => params.append('categories', c));
      } else if (v) {
        params.set(k, v);
      }
    });
    setSearchParams(params, { replace: true });
  }, [page, keyword, sort, filters]);

  const updateFilter = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPage(1);
  };

  const toggleCategory = (cat) => {
    setFilters(prev => ({
      ...prev,
      categories: prev.categories.includes(cat)
        ? prev.categories.filter(c => c !== cat)
        : [...prev.categories, cat],
    }));
    setPage(1);
  };

  const resetFilters = () => {
    setFilters({
      regionLevel1: '', regionLevel2: '', supplyType: '', houseType: '',
      provider: '', status: '', categories: [], depositMin: '', depositMax: '',
      rentMin: '', rentMax: '',
    });
    setKeyword('');
    setPage(1);
  };

  const handleToggleFavorite = async (announcementId) => {
    if (!isLoggedIn) return;
    try {
      const existing = announcements.find(a => a.announcementId === announcementId);
      if (existing?.favorited) {
        await api.del(`/api/me/favorites/${announcementId}`);
        toast('즐겨찾기가 해제되었습니다');
      } else {
        await api.post('/api/me/favorites', { announcementId });
        toast('즐겨찾기에 추가되었습니다');
      }
      setAnnouncements(prev => prev.map(a => a.announcementId === announcementId ? { ...a, favorited: !a.favorited } : a));
    } catch { toast('오류가 발생했습니다', 'error'); }
  };

  return (
    <div style={S.container}>
      {/* Sidebar Filters */}
      <aside style={S.sidebar}>
        <h3 style={{ fontSize: 18, fontWeight: 700, color: '#222', marginBottom: 20 }}>필터</h3>

        <div>
          <p style={S.sectionTitle}>지역</p>
          <select style={S.select} value={filters.regionLevel1}
            onChange={e => { updateFilter('regionLevel1', e.target.value); updateFilter('regionLevel2', ''); }}>
            <option value="">전체</option>
            {regions.map(r => <option key={r} value={r}>{r}</option>)}
          </select>
          {filters.regionLevel1 && (
            <select style={S.select} value={filters.regionLevel2}
              onChange={e => updateFilter('regionLevel2', e.target.value)}>
              <option value="">전체</option>
              {regions2.map(r => <option key={r} value={r}>{r}</option>)}
            </select>
          )}
        </div>

        <div>
          <p style={S.sectionTitle}>공급 유형</p>
          <select style={S.select} value={filters.supplyType}
            onChange={e => updateFilter('supplyType', e.target.value)}>
            <option value="">전체</option>
            {supplyTypes.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>

        <div>
          <p style={S.sectionTitle}>주택 유형</p>
          <select style={S.select} value={filters.houseType}
            onChange={e => updateFilter('houseType', e.target.value)}>
            <option value="">전체</option>
            {houseTypes.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
        </div>

        <div>
          <p style={S.sectionTitle}>공급 기관</p>
          <select style={S.select} value={filters.provider}
            onChange={e => updateFilter('provider', e.target.value)}>
            <option value="">전체</option>
            {providers.map(p => <option key={p} value={p}>{p}</option>)}
          </select>
        </div>

        <div>
          <p style={S.sectionTitle}>모집 상태</p>
          <div style={S.radioGroup}>
            {STATUS_OPTIONS.map(opt => (
              <label key={opt.value} style={S.radioLabel}>
                <input type="radio" name="status" checked={filters.status === opt.value}
                  onChange={() => updateFilter('status', opt.value)} />
                {opt.label}
              </label>
            ))}
          </div>
        </div>

        <div>
          <p style={S.sectionTitle}>카테고리</p>
          {CATEGORY_OPTIONS.map(opt => (
            <label key={opt.value} style={S.checkbox}>
              <input type="checkbox" checked={filters.categories.includes(opt.value)}
                onChange={() => toggleCategory(opt.value)} />
              {opt.label}
            </label>
          ))}
        </div>

        <div style={S.rangeWrap}>
          <p style={S.sectionTitle}>보증금 범위 (만원)</p>
          <div style={S.rangeRow}>
            <input style={S.rangeInput} type="number" placeholder="0" value={filters.depositMin}
              onChange={e => updateFilter('depositMin', e.target.value)} />
            <span>~</span>
            <input style={S.rangeInput} type="number" placeholder="50000" value={filters.depositMax}
              onChange={e => updateFilter('depositMax', e.target.value)} />
          </div>
        </div>

        <div style={S.rangeWrap}>
          <p style={S.sectionTitle}>월세 범위 (만원)</p>
          <div style={S.rangeRow}>
            <input style={S.rangeInput} type="number" placeholder="0" value={filters.rentMin}
              onChange={e => updateFilter('rentMin', e.target.value)} />
            <span>~</span>
            <input style={S.rangeInput} type="number" placeholder="100" value={filters.rentMax}
              onChange={e => updateFilter('rentMax', e.target.value)} />
          </div>
        </div>

        <div style={S.filterActions}>
          <button style={S.resetBtn} onClick={resetFilters}>필터 초기화</button>
          <button style={S.applyBtn} onClick={() => { setPage(1); loadAnnouncements(); }}>적용</button>
        </div>
      </aside>

      {/* Main Content */}
      <div style={S.main}>
        <div style={S.searchBar}>
          <input style={S.searchInput} type="text" placeholder="공고명, 지역, 기관으로 검색"
            value={keyword} onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') { setPage(1); loadAnnouncements(); } }} />
          <select style={S.sortSelect} value={sort} onChange={e => { setSort(e.target.value); setPage(1); }}>
            <option value="deadline">마감임박순</option>
            <option value="latest">최신순</option>
            <option value="deposit_asc">보증금 낮은순</option>
            <option value="deposit_desc">보증금 높은순</option>
          </select>
        </div>

        <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 16 }}>
          총 <strong style={{ color: '#222' }}>{totalCount.toLocaleString()}</strong>건의 공고
        </p>

        {loading ? (
          <LoadingSpinner />
        ) : announcements.length === 0 ? (
          <EmptyState icon="🔍" title="검색 결과가 없습니다"
            description="다른 검색어나 필터를 시도해보세요."
            actionLabel="필터 초기화" onAction={resetFilters} />
        ) : (
          <>
            <div style={S.grid}>
              {announcements.map(a => (
                <AnnouncementCard key={a.id} announcement={a} onToggleFavorite={handleToggleFavorite} />
              ))}
            </div>
            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
          </>
        )}
      </div>
    </div>
  );
}
