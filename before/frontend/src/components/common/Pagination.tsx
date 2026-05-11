interface Props {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ currentPage, totalPages, onPageChange }: Props) {
  if (totalPages <= 1) return null;

  const pages: (number | '...')[] = [];
  if (totalPages <= 7) {
    for (let i = 0; i < totalPages; i++) pages.push(i);
  } else {
    pages.push(0);
    if (currentPage > 3) pages.push('...');
    for (let i = Math.max(1, currentPage - 2); i <= Math.min(totalPages - 2, currentPage + 2); i++) pages.push(i);
    if (currentPage < totalPages - 4) pages.push('...');
    pages.push(totalPages - 1);
  }

  const btnStyle = (active: boolean): React.CSSProperties => ({
    width: 36, height: 36, borderRadius: 8, border: active ? '1.5px solid #222' : '1px solid #e0e0e0',
    background: active ? '#222' : '#fff', color: active ? '#fff' : '#222',
    cursor: active ? 'default' : 'pointer', fontSize: 14, fontWeight: active ? 700 : 400,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontFamily: "'Noto Sans KR',sans-serif",
  });

  const arrowStyle: React.CSSProperties = {
    width: 36, height: 36, borderRadius: 8, border: '1px solid #e0e0e0',
    background: '#fff', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 4, padding: '32px 0' }}>
      <button style={{ ...arrowStyle, opacity: currentPage === 0 ? 0.3 : 1 }} disabled={currentPage === 0} onClick={() => onPageChange(currentPage - 1)}>
        <svg viewBox="0 0 24 24" fill="none" stroke="#222" strokeWidth="2" width="16" height="16"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      {pages.map((p, i) =>
        p === '...' ? (
          <span key={`ellipsis-${i}`} style={{ width: 36, textAlign: 'center', color: '#6a6a6a' }}>...</span>
        ) : (
          <button key={p} style={btnStyle(p === currentPage)} onClick={() => p !== currentPage && onPageChange(p as number)}>
            {(p as number) + 1}
          </button>
        )
      )}
      <button style={{ ...arrowStyle, opacity: currentPage === totalPages - 1 ? 0.3 : 1 }} disabled={currentPage === totalPages - 1} onClick={() => onPageChange(currentPage + 1)}>
        <svg viewBox="0 0 24 24" fill="none" stroke="#222" strokeWidth="2" width="16" height="16"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
    </div>
  );
}
