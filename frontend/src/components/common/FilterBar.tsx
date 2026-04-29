const PILLS = [
  { id: 'all', label: '전체', icon: <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22"><path d="M12 3L2 12h3v9h6v-6h2v6h6v-9h3L12 3z"/></svg> },
  { id: '국민임대', label: '국민임대', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg> },
  { id: '행복주택', label: '행복주택', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-4h6v4"/></svg> },
  { id: '영구임대', label: '영구임대', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2"/></svg> },
  { id: '매입임대', label: '매입임대', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><path d="M20 9v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9"/><path d="M9 22V12h6v10M2 10.6L12 2l10 8.6"/></svg> },
  { id: '공공분양', label: '공공분양', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg> },
  { id: '기타', label: '기타', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><circle cx="12" cy="12" r="3"/><path d="M12 1v4M12 19v4M4.22 4.22l2.83 2.83M16.95 16.95l2.83 2.83M1 12h4M19 12h4"/></svg> },
];

interface Props {
  activeCategory: string;
  onCategoryChange: (id: string) => void;
  resultCount: number;
  sortBy: string;
  onSortChange: (sort: string) => void;
}

export default function FilterBar({ activeCategory, onCategoryChange, resultCount, sortBy, onSortChange }: Props) {
  const pillStyle = (active: boolean): React.CSSProperties => ({
    display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 20px', gap: 6,
    cursor: 'pointer', background: 'none', border: 'none',
    borderBottom: active ? '2px solid #222' : '2px solid transparent',
    whiteSpace: 'nowrap', flexShrink: 0,
    opacity: active ? 1 : 0.6,
    transition: 'border-color 0.15s, opacity 0.15s',
    fontFamily: "'Noto Sans KR',sans-serif",
  });

  const filterTagStyle = (active: boolean, red: boolean): React.CSSProperties => ({
    display: 'flex', alignItems: 'center', gap: 8, padding: '8px 16px',
    border: red ? '1px solid #ff385c' : active ? '1px solid #222' : '1px solid #c1c1c1',
    borderRadius: 8, fontSize: 13, fontWeight: 500, cursor: 'pointer',
    background: active && !red ? '#222' : '#fff',
    color: red ? '#ff385c' : active ? '#fff' : '#222',
    transition: 'box-shadow 0.15s, border-color 0.15s',
    fontFamily: "'Noto Sans KR',sans-serif",
  });

  return (
    <div>
      {/* 카테고리 필 */}
      <div style={{ borderBottom: '1px solid rgba(0,0,0,0.08)', background: '#fff' }}>
        <div style={{ maxWidth: 1280, margin: '0 auto', padding: '0 24px', display: 'flex', overflowX: 'auto', scrollbarWidth: 'none' }}>
          {PILLS.map((p) => (
            <button key={p.id} style={pillStyle(activeCategory === p.id)} onClick={() => onCategoryChange(p.id)}>
              <div style={{ width: 24, height: 24, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#222' }}>{p.icon}</div>
              <span style={{ fontSize: 12, fontWeight: 600, color: '#222' }}>{p.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 필터 태그 */}
      <div style={{ maxWidth: 1280, margin: '0 auto', padding: '14px 24px', display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        {(['추천순', '마감순', '최신순'] as const).map((s) => (
          <button key={s} style={filterTagStyle(sortBy === s, sortBy === s && s === '추천순')} onClick={() => onSortChange(s)}>
            {s === '추천순' && <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="13" height="13"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>}
            {s}
          </button>
        ))}
        <span style={{ marginLeft: 'auto', fontSize: 13, color: '#6a6a6a', fontWeight: 400 }}>
          총 <strong style={{ color: '#222', fontWeight: 600 }}>{resultCount}</strong>개의 공고
        </span>
      </div>
    </div>
  );
}
