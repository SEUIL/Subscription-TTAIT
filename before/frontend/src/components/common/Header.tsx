import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function Header() {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();
  const [q, setQ] = useState('');

  const handleSearch = (keyword: string) => {
    if (keyword.trim()) navigate(`/?keyword=${encodeURIComponent(keyword.trim())}`);
  };

  const S = {
    header: { position: 'sticky' as const, top: 0, zIndex: 100, background: '#fff', borderBottom: '1px solid rgba(0,0,0,0.08)', padding: '0 24px' },
    inner: { maxWidth: 1280, margin: '0 auto', display: 'flex', alignItems: 'center', gap: 16, height: 80 },
    logo: { display: 'flex', alignItems: 'center', gap: 8, textDecoration: 'none', cursor: 'pointer', flexShrink: 0 } as React.CSSProperties,
    logoIcon: { width: 32, height: 32, background: '#ff385c', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' },
    logoText: { fontSize: 20, fontWeight: 700, color: '#ff385c', letterSpacing: '-0.3px' },
    searchBar: {
      flex: 1, maxWidth: 560, display: 'flex', alignItems: 'center', background: '#fff',
      boxShadow: 'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px, rgba(0,0,0,0.1) 0px 4px 8px',
      borderRadius: 40, padding: '0 8px 0 20px', height: 56, gap: 12,
    },
    searchInput: { flex: 1, border: 'none', outline: 'none', fontFamily: "'Noto Sans KR',sans-serif", fontSize: 14, fontWeight: 500, color: '#222', background: 'transparent' } as React.CSSProperties,
    searchBtn: { width: 40, height: 40, borderRadius: '50%', background: '#ff385c', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
    actions: { display: 'flex', alignItems: 'center', gap: 8, marginLeft: 'auto', flexShrink: 0 },
    ghostBtn: { padding: '0 16px', height: 42, border: 'none', background: 'transparent', cursor: 'pointer', fontFamily: "'Noto Sans KR',sans-serif", fontSize: 14, fontWeight: 500, color: '#222', borderRadius: 21 },
    outlineBtn: { padding: '0 16px', height: 42, border: '1.5px solid #222', background: 'transparent', cursor: 'pointer', fontFamily: "'Noto Sans KR',sans-serif", fontSize: 14, fontWeight: 600, color: '#222', borderRadius: 21 },
    iconBtn: { width: 42, height: 42, borderRadius: '50%', background: '#f2f2f2', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative' as const },
    notifDot: { position: 'absolute' as const, top: 2, right: 2, width: 8, height: 8, borderRadius: '50%', background: '#ff385c', border: '2px solid #fff' },
    avatar: {
      width: 42, height: 42, borderRadius: '50%', background: '#222', border: 'none', cursor: 'pointer',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      boxShadow: 'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px, rgba(0,0,0,0.1) 0px 4px 8px',
    },
  };

  const initials = user?.username?.charAt(0).toUpperCase() ?? '?';

  return (
    <header style={S.header}>
      <div style={S.inner}>
        {/* 로고 */}
        <div style={S.logo} onClick={() => navigate('/')}>
          <div style={S.logoIcon}>
            <svg viewBox="0 0 24 24" width="18" height="18" fill="white">
              <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" />
            </svg>
          </div>
          <span style={S.logoText}>집구해</span>
        </div>

        {/* 검색창 */}
        <div style={S.searchBar}>
          <input
            style={S.searchInput}
            type="text"
            placeholder="지역, 공급유형, 기관으로 검색"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch(q)}
          />
          <button style={S.searchBtn} onClick={() => handleSearch(q)}>
            <svg viewBox="0 0 24 24" fill="white" width="16" height="16">
              <path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z" />
            </svg>
          </button>
        </div>

        {/* 액션 */}
        <div style={S.actions}>
          {isAuthenticated ? (
            <>
              <button style={S.ghostBtn} onClick={() => navigate('/recommendations')}>추천 공고</button>
              <button style={S.iconBtn}>
                <svg viewBox="0 0 24 24" fill="none" stroke="#222" strokeWidth="2" width="18" height="18">
                  <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                  <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                </svg>
                <div style={S.notifDot} />
              </button>
              <button style={S.avatar} onClick={() => navigate('/me')}>
                <span style={{ color: '#fff', fontSize: 14, fontWeight: 600, fontFamily: "'Noto Sans KR',sans-serif" }}>{initials}</span>
              </button>
            </>
          ) : (
            <>
              <button style={S.ghostBtn} onClick={() => navigate('/auth')}>로그인</button>
              <button style={S.outlineBtn} onClick={() => navigate('/auth')}>회원가입</button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
