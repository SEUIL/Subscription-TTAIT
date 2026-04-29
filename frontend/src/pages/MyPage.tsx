import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getMe, updateProfile } from '../api/user';
import type { User } from '../types';

type Section = 'profile' | 'saved' | 'notifications';

const CATS: [string, string][] = [
  ['청년', '👤'], ['신혼부부', '💑'], ['무주택자', '🏠'],
  ['고령자', '👴'], ['저소득층', '💰'], ['다자녀', '👨‍👩‍👧‍👦'],
];

const NAV_ITEMS = [
  { id: 'profile' as Section, label: '내 프로필', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg> },
  { id: 'saved' as Section, label: '저장한 공고', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg> },
  { id: 'notifications' as Section, label: '알림', icon: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg> },
];

function Toggle({ on, onToggle }: { on: boolean; onToggle: () => void }) {
  return (
    <button
      style={{ width: 44, height: 24, borderRadius: 12, background: on ? '#ff385c' : '#c1c1c1', border: 'none', cursor: 'pointer', position: 'relative', transition: 'background 0.2s', flexShrink: 0 }}
      onClick={onToggle}
    >
      <div style={{ position: 'absolute', width: 20, height: 20, borderRadius: '50%', background: '#fff', top: 2, left: 2, transition: 'transform 0.2s', transform: on ? 'translateX(20px)' : 'none', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
    </button>
  );
}

const S = {
  card: { background: '#fff', borderRadius: 20, marginBottom: 16, overflow: 'hidden', boxShadow: 'rgba(0,0,0,0.02) 0px 0px 0px 1px,rgba(0,0,0,0.04) 0px 2px 6px,rgba(0,0,0,0.1) 0px 4px 8px' } as React.CSSProperties,
  cardHeader: { padding: '20px 24px 16px', borderBottom: '1px solid rgba(0,0,0,0.06)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' } as React.CSSProperties,
  cardTitle: { fontSize: 16, fontWeight: 700, color: '#222' } as React.CSSProperties,
  cardBadge: { padding: '4px 10px', borderRadius: 20, fontSize: 11, fontWeight: 700, background: '#fff0f3', color: '#ff385c' } as React.CSSProperties,
  label: { display: 'block', fontSize: 12, fontWeight: 600, color: '#6a6a6a', marginBottom: 6, letterSpacing: '0.3px' } as React.CSSProperties,
  input: { width: '100%', height: 46, padding: '0 14px', border: '1.5px solid rgba(0,0,0,0.12)', borderRadius: 10, fontFamily: "'Noto Sans KR',sans-serif", fontSize: 14, color: '#222', background: '#fff', outline: 'none', boxSizing: 'border-box' } as React.CSSProperties,
  row2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 } as React.CSSProperties,
  saveBtn: { width: '100%', height: 52, borderRadius: 12, background: '#222', border: 'none', cursor: 'pointer', fontFamily: "'Noto Sans KR',sans-serif", fontSize: 15, fontWeight: 600, color: '#fff', marginTop: 8, transition: 'background 0.2s' } as React.CSSProperties,
  chip: (sel: boolean): React.CSSProperties => ({ padding: '8px 16px', borderRadius: 20, border: `1.5px solid ${sel ? '#ff385c' : '#c1c1c1'}`, fontFamily: "'Noto Sans KR',sans-serif", fontSize: 13, fontWeight: 600, color: sel ? '#ff385c' : '#6a6a6a', background: sel ? '#fff0f3' : '#fff', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, transition: 'all 0.15s' }),
  navItem: (active: boolean): React.CSSProperties => ({ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 20px', cursor: 'pointer', fontSize: 14, fontWeight: active ? 600 : 500, color: active ? '#ff385c' : '#6a6a6a', background: active ? '#fff0f3' : 'transparent', transition: 'background 0.15s,color 0.15s', border: 'none', width: '100%', textAlign: 'left', fontFamily: "'Noto Sans KR',sans-serif" }),
};

export default function MyPage() {
  const navigate = useNavigate();
  const { user: authUser, logout } = useAuth();
  const [section, setSection] = useState<Section>('profile');
  const [user, setUser] = useState<User | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveMsg, setSaveMsg] = useState('');

  // profile form
  const [age, setAge] = useState('');
  const [maritalStatus, setMaritalStatus] = useState('미혼');
  const [childrenCount, setChildrenCount] = useState('0');
  const [regionLevel1, setRegionLevel1] = useState('');
  const [regionLevel2, setRegionLevel2] = useState('');
  const [maxDeposit, setMaxDeposit] = useState(20000000);
  const [maxRent, setMaxRent] = useState(300000);
  const [chips, setChips] = useState<string[]>([]);

  const [toggles, setToggles] = useState({ d3: true, d1: true, newAlert: false });

  useEffect(() => {
    getMe().then((u) => {
      setUser(u);
    }).catch(() => {});
  }, []);

  const toggleChip = (c: string) => setChips((p) => p.includes(c) ? p.filter((x) => x !== c) : [...p, c]);
  const fmtMoney = (v: number) => (v / 10000).toLocaleString() + '만원';

  const handleSave = async () => {
    setSaving(true);
    setSaveMsg('');
    try {
      await updateProfile({
        age: age ? Number(age) : undefined,
        maritalStatus,
        childrenCount: Number(childrenCount),
        regionLevel1,
        regionLevel2,
        maxDeposit,
        maxMonthlyRent: maxRent,
        categories: chips,
      });
      setSaveMsg('저장됐습니다.');
      setTimeout(() => setSaveMsg(''), 3000);
    } catch {
      setSaveMsg('저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const initials = (user?.username ?? authUser?.username ?? '?').charAt(0).toUpperCase();

  return (
    <div style={{ background: '#f2f2f2', minHeight: '100vh' }}>
      <div style={{ maxWidth: 960, margin: '0 auto', padding: '32px 24px 80px', display: 'grid', gridTemplateColumns: '240px 1fr', gap: 24, alignItems: 'start' }}>

        {/* 사이드바 */}
        <div style={{ background: '#fff', borderRadius: 20, overflow: 'hidden', boxShadow: 'rgba(0,0,0,0.02) 0px 0px 0px 1px,rgba(0,0,0,0.04) 0px 2px 6px,rgba(0,0,0,0.1) 0px 4px 8px', position: 'sticky', top: 90 }}>
          <div style={{ padding: 24, textAlign: 'center', borderBottom: '1px solid rgba(0,0,0,0.06)' }}>
            <div style={{ width: 72, height: 72, borderRadius: '50%', background: '#222', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 12px', fontSize: 28, color: '#fff', fontWeight: 700 }}>
              {initials}
            </div>
            <p style={{ fontSize: 16, fontWeight: 700, color: '#222', marginBottom: 2 }}>{user?.username ?? authUser?.username}</p>
            <p style={{ fontSize: 13, color: '#6a6a6a' }}>@{user?.username ?? authUser?.username}</p>
          </div>
          <div style={{ padding: '8px 0' }}>
            {NAV_ITEMS.map((n) => (
              <button key={n.id} style={S.navItem(section === n.id)} onClick={() => setSection(n.id)}>
                {n.icon}{n.label}
              </button>
            ))}
            <div style={{ height: 1, background: 'rgba(0,0,0,0.06)', margin: '4px 0' }} />
            <button
              style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 20px', cursor: 'pointer', fontSize: 14, fontWeight: 500, color: '#ff385c', border: 'none', background: 'transparent', width: '100%', textAlign: 'left', fontFamily: "'Noto Sans KR',sans-serif" }}
              onClick={() => { logout(); navigate('/'); }}
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
              로그아웃
            </button>
          </div>
        </div>

        {/* 메인 */}
        <div>
          {/* 프로필 */}
          {section === 'profile' && (
            <>
              <div style={S.card}>
                <div style={S.cardHeader}><span style={S.cardTitle}>기본 정보</span></div>
                <div style={{ padding: '20px 24px' }}>
                  <div style={S.row2}>
                    <div>
                      <label style={S.label}>아이디</label>
                      <input style={{ ...S.input, background: '#f2f2f2', color: '#6a6a6a' }} type="text" value={user?.username ?? ''} readOnly />
                    </div>
                    <div>
                      <label style={S.label}>이메일</label>
                      <input style={{ ...S.input, background: '#f2f2f2', color: '#6a6a6a' }} type="email" value={user?.email ?? ''} readOnly />
                    </div>
                    <div>
                      <label style={S.label}>휴대폰 번호</label>
                      <input style={{ ...S.input, background: '#f2f2f2', color: '#6a6a6a' }} type="tel" value={user?.phone ?? ''} readOnly />
                    </div>
                    <div>
                      <label style={S.label}>나이</label>
                      <input style={S.input} type="number" placeholder="나이" value={age} onChange={(e) => setAge(e.target.value)} />
                    </div>
                  </div>
                  <div style={S.row2}>
                    <div>
                      <label style={S.label}>혼인 상태</label>
                      <select style={{ ...S.input, cursor: 'pointer' }} value={maritalStatus} onChange={(e) => setMaritalStatus(e.target.value)}>
                        {['미혼', '기혼', '기타'].map((o) => <option key={o}>{o}</option>)}
                      </select>
                    </div>
                    <div>
                      <label style={S.label}>자녀 수</label>
                      <input style={S.input} type="number" min="0" value={childrenCount} onChange={(e) => setChildrenCount(e.target.value)} />
                    </div>
                  </div>
                </div>
              </div>

              <div style={S.card}>
                <div style={S.cardHeader}><span style={S.cardTitle}>주거 선호 설정</span></div>
                <div style={{ padding: '20px 24px' }}>
                  <div style={S.row2}>
                    <div>
                      <label style={S.label}>선호 지역 (시/도)</label>
                      <input style={S.input} type="text" placeholder="예: 경기도" value={regionLevel1} onChange={(e) => setRegionLevel1(e.target.value)} />
                    </div>
                    <div>
                      <label style={S.label}>선호 지역 (시/군/구)</label>
                      <input style={S.input} type="text" placeholder="예: 오산시" value={regionLevel2} onChange={(e) => setRegionLevel2(e.target.value)} />
                    </div>
                  </div>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                      <span style={{ fontSize: 13, color: '#6a6a6a' }}>최대 보증금</span>
                      <span style={{ fontSize: 16, fontWeight: 700, color: '#222' }}>{fmtMoney(maxDeposit)}</span>
                    </div>
                    <input type="range" min="0" max="100000000" step="1000000" value={maxDeposit} onChange={(e) => setMaxDeposit(Number(e.target.value))} style={{ width: '100%', height: 4, borderRadius: 2, background: '#f2f2f2', outline: 'none', border: 'none', cursor: 'pointer' }} />
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 16, marginBottom: 8 }}>
                      <span style={{ fontSize: 13, color: '#6a6a6a' }}>최대 월세</span>
                      <span style={{ fontSize: 16, fontWeight: 700, color: '#222' }}>{fmtMoney(maxRent)}</span>
                    </div>
                    <input type="range" min="0" max="1000000" step="10000" value={maxRent} onChange={(e) => setMaxRent(Number(e.target.value))} style={{ width: '100%', height: 4, borderRadius: 2, background: '#f2f2f2', outline: 'none', border: 'none', cursor: 'pointer' }} />
                  </div>
                </div>
              </div>

              <div style={S.card}>
                <div style={S.cardHeader}>
                  <span style={S.cardTitle}>해당 유형 선택</span>
                  <span style={S.cardBadge}>추천 정확도에 영향</span>
                </div>
                <div style={{ padding: '20px 24px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {CATS.map(([c, e]) => (
                      <button key={c} style={S.chip(chips.includes(c))} onClick={() => toggleChip(c)}>
                        <span>{e}</span>{c}
                      </button>
                    ))}
                  </div>
                  {saveMsg && (
                    <p style={{ fontSize: 13, color: saveMsg.includes('실패') ? '#ff385c' : '#166534', marginTop: 8, fontWeight: 600 }}>{saveMsg}</p>
                  )}
                  <button
                    style={{ ...S.saveBtn, opacity: saving ? 0.7 : 1 }}
                    disabled={saving}
                    onMouseEnter={(e) => (e.currentTarget.style.background = '#111')}
                    onMouseLeave={(e) => (e.currentTarget.style.background = '#222')}
                    onClick={handleSave}
                  >
                    {saving ? '저장 중...' : '변경 사항 저장'}
                  </button>
                </div>
              </div>
            </>
          )}

          {/* 저장한 공고 */}
          {section === 'saved' && (
            <div style={S.card}>
              <div style={S.cardHeader}>
                <span style={S.cardTitle}>저장한 공고</span>
                <span style={S.cardBadge}>준비 중</span>
              </div>
              <div style={{ padding: '48px 24px', textAlign: 'center', color: '#6a6a6a' }}>
                <div style={{ fontSize: 40, marginBottom: 16 }}>🔖</div>
                <p style={{ fontSize: 15, fontWeight: 600, marginBottom: 8 }}>저장 기능은 준비 중입니다</p>
                <p style={{ fontSize: 13 }}>공고 상세 페이지에서 ♡ 버튼을 눌러 저장할 수 있어요.</p>
              </div>
            </div>
          )}

          {/* 알림 */}
          {section === 'notifications' && (
            <>
              <div style={S.card}>
                <div style={S.cardHeader}><span style={S.cardTitle}>알림 설정</span></div>
                <div style={{ padding: '8px 24px' }}>
                  {([
                    ['d3', '마감 D-3 알림', '저장한 공고 마감 3일 전 알림'],
                    ['d1', '마감 D-1 알림', '저장한 공고 마감 하루 전 알림'],
                    ['newAlert', '새 추천 공고 알림', '내 조건에 맞는 새 공고 등록 시 알림'],
                  ] as const).map(([k, l, d]) => (
                    <div key={k} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 0', borderBottom: '1px solid rgba(0,0,0,0.06)' }}>
                      <div>
                        <p style={{ fontSize: 14, fontWeight: 600, color: '#222', marginBottom: 2 }}>{l}</p>
                        <p style={{ fontSize: 12, color: '#6a6a6a' }}>{d}</p>
                      </div>
                      <Toggle on={toggles[k]} onToggle={() => setToggles((p) => ({ ...p, [k]: !p[k] }))} />
                    </div>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
