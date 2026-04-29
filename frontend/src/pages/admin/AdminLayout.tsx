import { NavLink, Outlet } from 'react-router-dom';

const NAV = [
  { to: '/admin', label: '대시보드', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>, end: true },
  { to: '/admin/review', label: '공고 리뷰', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>, end: false },
  { to: '/admin/import', label: 'LH 임포트', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><polyline points="8 17 12 21 16 17"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.88 18.09A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.29"/></svg>, end: true },
];

export default function AdminLayout() {
  return (
    <div style={{ display: 'flex', minHeight: 'calc(100vh - 80px)', background: '#f2f2f2' }}>
      {/* 사이드바 */}
      <div style={{ width: 220, background: '#fff', borderRight: '1px solid rgba(0,0,0,0.08)', padding: '24px 0', flexShrink: 0, position: 'sticky', top: 80, height: 'calc(100vh - 80px)' }}>
        <div style={{ padding: '0 20px 20px', borderBottom: '1px solid rgba(0,0,0,0.06)', marginBottom: 8 }}>
          <p style={{ fontSize: 11, fontWeight: 700, color: '#6a6a6a', letterSpacing: '0.5px' }}>ADMIN</p>
          <p style={{ fontSize: 16, fontWeight: 700, color: '#222' }}>관리자 패널</p>
        </div>
        {NAV.map((n) => (
          <NavLink
            key={n.to}
            to={n.to}
            end={n.end}
            style={({ isActive }) => ({
              display: 'flex', alignItems: 'center', gap: 10, padding: '12px 20px',
              fontSize: 14, fontWeight: isActive ? 600 : 500,
              color: isActive ? '#ff385c' : '#6a6a6a',
              background: isActive ? '#fff0f3' : 'transparent',
              textDecoration: 'none', transition: 'background 0.15s, color 0.15s',
            })}
          >
            {n.icon}{n.label}
          </NavLink>
        ))}
      </div>

      {/* 콘텐츠 */}
      <div style={{ flex: 1, padding: '32px', overflow: 'auto' }}>
        <Outlet />
      </div>
    </div>
  );
}
