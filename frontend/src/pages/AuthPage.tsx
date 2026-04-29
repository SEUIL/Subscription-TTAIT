import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { login, signup } from '../api/auth';

type Tab = 'login' | 'signup';
type Step = 1 | 2;

const CATS: [string, string][] = [
  ['청년', '👤'],
  ['신혼부부', '💑'],
  ['무주택자', '🏠'],
  ['고령자', '👴'],
  ['저소득층', '💰'],
  ['다자녀', '👨‍👩‍👧‍👦'],
];

const S = {
  wrap: { flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '40px 24px', minHeight: '100vh' },
  card: { width: '100%', maxWidth: 480 },
  tabBar: { display: 'flex', borderBottom: '1px solid rgba(0,0,0,0.1)', marginBottom: 32 },
  tab: (active: boolean): React.CSSProperties => ({
    flex: 1, padding: '14px 0', border: 'none', background: 'transparent', cursor: 'pointer',
    fontFamily: "'Noto Sans KR',sans-serif", fontSize: 16, fontWeight: active ? 700 : 500,
    color: active ? '#222' : '#6a6a6a', borderBottom: active ? '2px solid #222' : '2px solid transparent',
    marginBottom: -1, transition: 'color 0.15s,border-color 0.15s',
  }),
  title: { fontSize: 24, fontWeight: 700, color: '#222', letterSpacing: '-0.4px', marginBottom: 8 } as React.CSSProperties,
  subtitle: { fontSize: 14, color: '#6a6a6a', marginBottom: 32, lineHeight: 1.6 } as React.CSSProperties,
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: '#222', marginBottom: 8 } as React.CSSProperties,
  input: {
    width: '100%', height: 52, padding: '0 16px', border: '1.5px solid #c1c1c1', borderRadius: 12,
    fontFamily: "'Noto Sans KR',sans-serif", fontSize: 15, color: '#222', background: '#fff', outline: 'none',
    transition: 'border-color 0.15s, box-shadow 0.15s', boxSizing: 'border-box',
  } as React.CSSProperties,
  submitBtn: {
    width: '100%', height: 56, borderRadius: 12, background: '#222', border: 'none', cursor: 'pointer',
    fontFamily: "'Noto Sans KR',sans-serif", fontSize: 16, fontWeight: 600, color: '#fff',
    marginTop: 8, transition: 'background 0.2s,transform 0.1s',
  } as React.CSSProperties,
  row: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 16 } as React.CSSProperties,
  stepDot: (s: 'done' | 'current' | 'pending'): React.CSSProperties => ({
    width: 28, height: 28, borderRadius: '50%', border: 'none',
    fontFamily: "'Noto Sans KR',sans-serif", fontSize: 12, fontWeight: 700,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: s === 'done' ? '#222' : s === 'current' ? '#ff385c' : '#f2f2f2',
    color: s === 'done' || s === 'current' ? '#fff' : '#6a6a6a',
  }),
  stepLine: (done: boolean): React.CSSProperties => ({ flex: 1, height: 2, background: done ? '#222' : 'rgba(0,0,0,0.1)' }),
  chipGrid: { display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10, marginTop: 8 } as React.CSSProperties,
  chip: (sel: boolean): React.CSSProperties => ({
    padding: '12px 8px', borderRadius: 12, border: `1.5px solid ${sel ? '#ff385c' : '#c1c1c1'}`,
    cursor: 'pointer', textAlign: 'center', background: sel ? '#fff0f3' : '#fff',
    transition: 'border-color 0.15s,background 0.15s',
  }),
};

function focusInput(e: React.FocusEvent<HTMLInputElement | HTMLSelectElement>) {
  e.target.style.borderColor = '#222';
  e.target.style.boxShadow = '0 0 0 3px rgba(34,34,34,0.08)';
}
function blurInput(e: React.FocusEvent<HTMLInputElement | HTMLSelectElement>) {
  e.target.style.borderColor = '#c1c1c1';
  e.target.style.boxShadow = 'none';
}

export default function AuthPage() {
  const navigate = useNavigate();
  const { login: authLogin } = useAuth();

  const [tab, setTab] = useState<Tab>('login');
  const [step, setStep] = useState<Step>(1);
  const [done, setDone] = useState(false);
  const [chips, setChips] = useState<string[]>(['청년']);
  const [showPw, setShowPw] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Login form state
  const [loginForm, setLoginForm] = useState({ username: '', password: '' });

  // Signup form state
  const [signupForm, setSignupForm] = useState({
    username: '', password: '', email: '', phone: '',
    age: '', maritalStatus: '미혼', childrenCount: '0',
  });

  const toggleChip = (c: string) =>
    setChips((p) => (p.includes(c) ? p.filter((x) => x !== c) : [...p, c]));

  const handleLogin = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await login(loginForm);
      authLogin(res.token, res.user);
      navigate('/');
    } catch {
      setError('아이디 또는 비밀번호가 올바르지 않습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleSignupStep1 = () => {
    if (!signupForm.username || !signupForm.password || !signupForm.email || !signupForm.phone) {
      setError('모든 항목을 입력해주세요.');
      return;
    }
    setError('');
    setStep(2);
  };

  const handleSignupComplete = async () => {
    setError('');
    setLoading(true);
    try {
      const res = await signup({
        username: signupForm.username,
        password: signupForm.password,
        email: signupForm.email,
        phone: signupForm.phone,
        age: signupForm.age ? Number(signupForm.age) : undefined,
        maritalStatus: signupForm.maritalStatus,
        childrenCount: Number(signupForm.childrenCount),
        categories: chips,
      });
      authLogin(res.token, res.user);
      setDone(true);
    } catch {
      setError('회원가입에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  if (done) {
    return (
      <div style={S.wrap}>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <div style={{ width: 72, height: 72, borderRadius: '50%', background: '#f0fff4', border: '2px solid #4ade80', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', fontSize: 32 }}>
            🎉
          </div>
          <h2 style={{ fontSize: 22, fontWeight: 700, marginBottom: 8, letterSpacing: '-0.3px', color: '#222' }}>
            가입이 완료됐어요!
          </h2>
          <p style={{ fontSize: 14, color: '#6a6a6a', lineHeight: 1.7, marginBottom: 32 }}>
            선택하신 정보를 기반으로<br />맞춤 공고를 추천해드릴게요.
          </p>
          <button
            style={{ padding: '16px 40px', background: '#ff385c', color: '#fff', borderRadius: 12, fontSize: 16, fontWeight: 600, border: 'none', cursor: 'pointer' }}
            onMouseEnter={(e) => (e.currentTarget.style.background = '#e00b41')}
            onMouseLeave={(e) => (e.currentTarget.style.background = '#ff385c')}
            onClick={() => navigate('/')}
          >
            공고 둘러보기 &rarr;
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* 미니 헤더 */}
      <header style={{ padding: '20px 24px', borderBottom: '1px solid rgba(0,0,0,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <a href="/" style={{ display: 'flex', alignItems: 'center', gap: 8, textDecoration: 'none' }}>
          <div style={{ width: 32, height: 32, background: '#ff385c', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="white"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>
          </div>
          <span style={{ fontSize: 20, fontWeight: 700, color: '#ff385c', letterSpacing: '-0.3px' }}>집구해</span>
        </a>
      </header>

    <div style={S.wrap}>
      <div style={S.card}>
        <div style={S.tabBar}>
          <button style={S.tab(tab === 'login')} onClick={() => { setTab('login'); setStep(1); setError(''); }}>로그인</button>
          <button style={S.tab(tab === 'signup')} onClick={() => { setTab('signup'); setStep(1); setError(''); }}>회원가입</button>
        </div>

        {error && (
          <div style={{ background: '#fff0f3', border: '1px solid #ff385c', borderRadius: 8, padding: '12px 16px', marginBottom: 16, fontSize: 13, color: '#e00b41' }}>
            {error}
          </div>
        )}

        {/* 로그인 */}
        {tab === 'login' && (
          <div>
            <h1 style={S.title}>다시 만나서 반가워요 👋</h1>
            <p style={S.subtitle}>공공임대주택 공고를 저장하고<br />맞춤 추천을 받아보세요.</p>
            <div style={{ marginBottom: 16 }}>
              <label style={S.label}>아이디</label>
              <input
                style={S.input} type="text" placeholder="아이디를 입력하세요"
                value={loginForm.username}
                onChange={(e) => setLoginForm((f) => ({ ...f, username: e.target.value }))}
                onFocus={focusInput} onBlur={blurInput}
                onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
              />
            </div>
            <div style={{ marginBottom: 16 }}>
              <label style={S.label}>비밀번호</label>
              <div style={{ position: 'relative' }}>
                <input
                  style={{ ...S.input, paddingRight: 44 }}
                  type={showPw ? 'text' : 'password'} placeholder="비밀번호를 입력하세요"
                  value={loginForm.password}
                  onChange={(e) => setLoginForm((f) => ({ ...f, password: e.target.value }))}
                  onFocus={focusInput} onBlur={blurInput}
                  onKeyDown={(e) => e.key === 'Enter' && handleLogin()}
                />
                <button
                  style={{ position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', background: 'transparent', border: 'none', cursor: 'pointer', color: '#6a6a6a', display: 'flex', alignItems: 'center' }}
                  onClick={() => setShowPw((s) => !s)}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                </button>
              </div>
            </div>
            <button
              style={{ ...S.submitBtn, opacity: loading ? 0.7 : 1 }}
              disabled={loading}
              onMouseEnter={(e) => (e.currentTarget.style.background = '#111')}
              onMouseLeave={(e) => (e.currentTarget.style.background = '#222')}
              onClick={handleLogin}
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </div>
        )}

        {/* 회원가입 Step 1 */}
        {tab === 'signup' && step === 1 && (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 24 }}>
              <div style={S.stepDot('current')}>1</div>
              <div style={S.stepLine(false)} />
              <div style={S.stepDot('pending')}>2</div>
            </div>
            <h1 style={S.title}>계정 만들기</h1>
            <p style={S.subtitle}>기본 정보를 입력해주세요.</p>
            {([['아이디', 'text', '영문, 숫자 4~20자', 'username'], ['비밀번호', 'password', '8자 이상, 영문+숫자 조합', 'password']] as const).map(([label, type, ph, field]) => (
              <div key={field} style={{ marginBottom: 16 }}>
                <label style={S.label}>{label}</label>
                <input
                  style={S.input} type={type} placeholder={ph}
                  value={signupForm[field]}
                  onChange={(e) => setSignupForm((f) => ({ ...f, [field]: e.target.value }))}
                  onFocus={focusInput} onBlur={blurInput}
                />
              </div>
            ))}
            <div style={S.row}>
              {([['이메일', 'email', '이메일 주소', 'email'], ['휴대폰 번호', 'tel', '010-0000-0000', 'phone']] as const).map(([label, type, ph, field]) => (
                <div key={field}>
                  <label style={S.label}>{label}</label>
                  <input
                    style={S.input} type={type} placeholder={ph}
                    value={signupForm[field]}
                    onChange={(e) => setSignupForm((f) => ({ ...f, [field]: e.target.value }))}
                    onFocus={focusInput} onBlur={blurInput}
                  />
                </div>
              ))}
            </div>
            <button
              style={S.submitBtn}
              onMouseEnter={(e) => (e.currentTarget.style.background = '#111')}
              onMouseLeave={(e) => (e.currentTarget.style.background = '#222')}
              onClick={handleSignupStep1}
            >
              다음 단계 &rarr;
            </button>
            <p style={{ fontSize: 12, color: '#6a6a6a', textAlign: 'center', marginTop: 16, lineHeight: 1.7 }}>
              가입하면 <a href="#" style={{ color: '#222', fontWeight: 600 }}>이용약관</a>과 <a href="#" style={{ color: '#222', fontWeight: 600 }}>개인정보처리방침</a>에 동의하는 것으로 간주됩니다.
            </p>
          </div>
        )}

        {/* 회원가입 Step 2 */}
        {tab === 'signup' && step === 2 && (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 24 }}>
              <div style={S.stepDot('done')}>✓</div>
              <div style={S.stepLine(true)} />
              <div style={S.stepDot('current')}>2</div>
            </div>
            <h1 style={S.title}>맞춤 정보 입력</h1>
            <p style={S.subtitle}>나에게 맞는 공고를 추천받으려면<br />아래 정보를 입력해주세요. (선택 사항)</p>
            <div style={{ marginBottom: 16 }}>
              <label style={S.label}>나이</label>
              <input
                style={S.input} type="number" placeholder="나이를 입력하세요"
                value={signupForm.age}
                onChange={(e) => setSignupForm((f) => ({ ...f, age: e.target.value }))}
                onFocus={focusInput} onBlur={blurInput}
              />
            </div>
            <div style={S.row}>
              <div>
                <label style={S.label}>혼인 상태</label>
                <select
                  style={{ ...S.input, cursor: 'pointer' }}
                  value={signupForm.maritalStatus}
                  onChange={(e) => setSignupForm((f) => ({ ...f, maritalStatus: e.target.value }))}
                  onFocus={focusInput} onBlur={blurInput}
                >
                  {['미혼', '기혼', '기타'].map((o) => <option key={o}>{o}</option>)}
                </select>
              </div>
              <div>
                <label style={S.label}>자녀 수</label>
                <input
                  style={S.input} type="number" placeholder="0명" min="0"
                  value={signupForm.childrenCount}
                  onChange={(e) => setSignupForm((f) => ({ ...f, childrenCount: e.target.value }))}
                  onFocus={focusInput} onBlur={blurInput}
                />
              </div>
            </div>
            <div style={{ marginBottom: 16 }}>
              <label style={S.label}>해당하는 유형을 모두 선택해주세요</label>
              <div style={S.chipGrid}>
                {CATS.map(([c, e]) => (
                  <div key={c} style={S.chip(chips.includes(c))} onClick={() => toggleChip(c)}>
                    <div style={{ fontSize: 24, marginBottom: 4 }}>{e}</div>
                    <div style={{ fontSize: 12, fontWeight: 600, color: chips.includes(c) ? '#ff385c' : '#222' }}>{c}</div>
                  </div>
                ))}
              </div>
            </div>
            <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
              <button
                style={{ ...S.submitBtn, background: '#f2f2f2', color: '#222', flex: '0 0 auto', width: 'auto', padding: '0 24px' }}
                onClick={() => setStep(1)}
              >
                &larr; 이전
              </button>
              <button
                style={{ ...S.submitBtn, flex: 1, opacity: loading ? 0.7 : 1 }}
                disabled={loading}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#111')}
                onMouseLeave={(e) => (e.currentTarget.style.background = '#222')}
                onClick={handleSignupComplete}
              >
                {loading ? '처리 중...' : '가입 완료'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
    </div>
  );
}
