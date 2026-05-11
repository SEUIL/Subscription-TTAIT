// AuthPage.jsx — login / 2-step sign-up
const AuthPage = ({ onSuccess }) => {
  const [tab, setTab] = React.useState('login');
  const [step, setStep] = React.useState(1);
  const [done, setDone] = React.useState(false);
  const [chips, setChips] = React.useState(['청년']);
  const [showPw, setShowPw] = React.useState(false);

  const S = {
    wrap: { flex:1, display:'flex', alignItems:'center', justifyContent:'center', padding:'40px 24px', minHeight:'calc(100vh - 73px)' },
    card: { width:'100%', maxWidth:480 },
    tabBar: { display:'flex', borderBottom:'1px solid rgba(0,0,0,0.1)', marginBottom:32 },
    tab: (active) => ({ flex:1, padding:'14px 0', border:'none', background:'transparent', cursor:'pointer',
      fontFamily:"'Noto Sans KR',sans-serif", fontSize:16, fontWeight: active?700:500,
      color: active?'#222':'#6a6a6a', borderBottom: active?'2px solid #222':'2px solid transparent',
      marginBottom:-1, transition:'color 0.15s,border-color 0.15s' }),
    title: { fontSize:24, fontWeight:700, color:'#222', letterSpacing:'-0.4px', marginBottom:8 },
    subtitle: { fontSize:14, color:'#6a6a6a', marginBottom:32, lineHeight:1.6 },
    label: { display:'block', fontSize:13, fontWeight:600, color:'#222', marginBottom:8 },
    input: { width:'100%', height:52, padding:'0 16px', border:'1.5px solid #c1c1c1', borderRadius:12,
      fontFamily:"'Noto Sans KR',sans-serif", fontSize:15, color:'#222', background:'#fff', outline:'none',
      transition:'border-color 0.15s, box-shadow 0.15s', boxSizing:'border-box' },
    submitBtn: { width:'100%', height:56, borderRadius:12, background:'#222', border:'none', cursor:'pointer',
      fontFamily:"'Noto Sans KR',sans-serif", fontSize:16, fontWeight:600, color:'#fff',
      marginTop:8, transition:'background 0.2s,transform 0.1s' },
    row: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:12, marginBottom:16 },
    stepDot: (s) => ({ width:28, height:28, borderRadius:'50%', border:'none', fontFamily:"'Noto Sans KR',sans-serif",
      fontSize:12, fontWeight:700, display:'flex', alignItems:'center', justifyContent:'center',
      background: s==='done'?'#222':s==='current'?'#ff385c':'#f2f2f2',
      color: s==='done'||s==='current'?'#fff':'#6a6a6a' }),
    stepLine: (done) => ({ flex:1, height:2, background: done?'#222':'rgba(0,0,0,0.1)' }),
    chipGrid: { display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:10, marginTop:8 },
    chip: (sel) => ({ padding:'12px 8px', borderRadius:12, border:`1.5px solid ${sel?'#ff385c':'#c1c1c1'}`,
      cursor:'pointer', textAlign:'center', background:sel?'#fff0f3':'#fff',
      transition:'border-color 0.15s,background 0.15s' }),
  };

  const CATS = [['청년','👤'],['신혼부부','💑'],['무주택자','🏠'],['고령자','👴'],['저소득층','💰'],['다자녀','👨‍👩‍👧‍👦']];

  const toggleChip = (c) => setChips(p => p.includes(c) ? p.filter(x=>x!==c) : [...p,c]);

  if (done) return (
    <div style={{...S.wrap}}>
      <div style={{textAlign:'center', padding:'40px 0'}}>
        <div style={{width:72,height:72,borderRadius:'50%',background:'#f0fff4',border:'2px solid #4ade80',display:'flex',alignItems:'center',justifyContent:'center',margin:'0 auto 24px',fontSize:32}}>🎉</div>
        <h2 style={{fontSize:22,fontWeight:700,marginBottom:8,letterSpacing:'-0.3px',color:'#222'}}>가입이 완료됐어요!</h2>
        <p style={{fontSize:14,color:'#6a6a6a',lineHeight:1.7,marginBottom:32}}>선택하신 정보를 기반으로<br/>맞춤 공고를 추천해드릴게요.</p>
        <button style={{padding:'16px 40px',background:'#ff385c',color:'#fff',borderRadius:12,fontSize:16,fontWeight:600,border:'none',cursor:'pointer'}}
          onMouseEnter={e=>e.currentTarget.style.background='#e00b41'} onMouseLeave={e=>e.currentTarget.style.background='#ff385c'}
          onClick={onSuccess}>공고 둘러보기 →</button>
      </div>
    </div>
  );

  return (
    <div style={S.wrap}>
      <div style={S.card}>
        <div style={S.tabBar}>
          <button style={S.tab(tab==='login')} onClick={()=>{setTab('login');setStep(1)}}>로그인</button>
          <button style={S.tab(tab==='signup')} onClick={()=>{setTab('signup');setStep(1)}}>회원가입</button>
        </div>

        {tab==='login' && (
          <div>
            <h1 style={S.title}>다시 만나서 반가워요 👋</h1>
            <p style={S.subtitle}>공공임대주택 공고를 저장하고<br/>맞춤 추천을 받아보세요.</p>
            <div style={{marginBottom:16}}>
              <label style={S.label}>아이디</label>
              <input style={S.input} type="text" placeholder="아이디를 입력하세요"
                onFocus={e=>{e.target.style.borderColor='#222';e.target.style.boxShadow='0 0 0 3px rgba(34,34,34,0.08)'}}
                onBlur={e=>{e.target.style.borderColor='#c1c1c1';e.target.style.boxShadow='none'}} />
            </div>
            <div style={{marginBottom:16}}>
              <label style={S.label}>비밀번호</label>
              <div style={{position:'relative'}}>
                <input style={{...S.input,paddingRight:44}} type={showPw?'text':'password'} placeholder="비밀번호를 입력하세요"
                  onFocus={e=>{e.target.style.borderColor='#222';e.target.style.boxShadow='0 0 0 3px rgba(34,34,34,0.08)'}}
                  onBlur={e=>{e.target.style.borderColor='#c1c1c1';e.target.style.boxShadow='none'}} />
                <button style={{position:'absolute',right:14,top:'50%',transform:'translateY(-50%)',background:'transparent',border:'none',cursor:'pointer',color:'#6a6a6a',display:'flex',alignItems:'center'}}
                  onClick={()=>setShowPw(s=>!s)}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
              </div>
            </div>
            <button style={S.submitBtn} onMouseEnter={e=>e.currentTarget.style.background='#111'} onMouseLeave={e=>e.currentTarget.style.background='#222'} onClick={onSuccess}>로그인</button>
            <p style={{fontSize:12,color:'#6a6a6a',textAlign:'center',marginTop:16}}>
              <a href="#" style={{color:'#222',fontWeight:600}}>비밀번호를 잊으셨나요?</a>
            </p>
          </div>
        )}

        {tab==='signup' && step===1 && (
          <div>
            <div style={{display:'flex',alignItems:'center',gap:8,marginBottom:24}}>
              <div style={S.stepDot('current')}>1</div>
              <div style={S.stepLine(false)}></div>
              <div style={S.stepDot('pending')}>2</div>
            </div>
            <h1 style={S.title}>계정 만들기</h1>
            <p style={S.subtitle}>기본 정보를 입력해주세요.</p>
            {[['아이디','text','영문, 숫자 4~20자'],['비밀번호','password','8자 이상, 영문+숫자 조합']].map(([l,t,ph])=>(
              <div key={l} style={{marginBottom:16}}>
                <label style={S.label}>{l}</label>
                <input style={S.input} type={t} placeholder={ph}
                  onFocus={e=>{e.target.style.borderColor='#222';e.target.style.boxShadow='0 0 0 3px rgba(34,34,34,0.08)'}}
                  onBlur={e=>{e.target.style.borderColor='#c1c1c1';e.target.style.boxShadow='none'}} />
              </div>
            ))}
            <div style={S.row}>
              {[['이메일','email','이메일 주소'],['휴대폰 번호','tel','010-0000-0000']].map(([l,t,ph])=>(
                <div key={l}>
                  <label style={S.label}>{l}</label>
                  <input style={S.input} type={t} placeholder={ph}
                    onFocus={e=>{e.target.style.borderColor='#222';e.target.style.boxShadow='0 0 0 3px rgba(34,34,34,0.08)'}}
                    onBlur={e=>{e.target.style.borderColor='#c1c1c1';e.target.style.boxShadow='none'}} />
                </div>
              ))}
            </div>
            <button style={S.submitBtn} onClick={()=>setStep(2)}>다음 단계 →</button>
            <p style={{fontSize:12,color:'#6a6a6a',textAlign:'center',marginTop:16,lineHeight:1.7}}>
              가입하면 <a href="#" style={{color:'#222',fontWeight:600}}>이용약관</a>과 <a href="#" style={{color:'#222',fontWeight:600}}>개인정보처리방침</a>에 동의하는 것으로 간주됩니다.
            </p>
          </div>
        )}

        {tab==='signup' && step===2 && (
          <div>
            <div style={{display:'flex',alignItems:'center',gap:8,marginBottom:24}}>
              <div style={S.stepDot('done')}>✓</div>
              <div style={S.stepLine(true)}></div>
              <div style={S.stepDot('current')}>2</div>
            </div>
            <h1 style={S.title}>맞춤 정보 입력</h1>
            <p style={S.subtitle}>나에게 맞는 공고를 추천받으려면<br/>아래 정보를 입력해주세요. (선택 사항)</p>
            <div style={{marginBottom:16}}>
              <label style={S.label}>나이</label>
              <input style={S.input} type="number" placeholder="나이를 입력하세요"
                onFocus={e=>{e.target.style.borderColor='#222';e.target.style.boxShadow='0 0 0 3px rgba(34,34,34,0.08)'}}
                onBlur={e=>{e.target.style.borderColor='#c1c1c1';e.target.style.boxShadow='none'}} />
            </div>
            <div style={S.row}>
              {[['혼인 상태',['미혼','기혼','기타']],['자녀 수',[]]].map(([l,opts])=>(
                <div key={l}>
                  <label style={S.label}>{l}</label>
                  {opts.length ? (
                    <select style={{...S.input,cursor:'pointer'}}>{opts.map(o=><option key={o}>{o}</option>)}</select>
                  ) : (
                    <input style={S.input} type="number" placeholder="0명" min="0" />
                  )}
                </div>
              ))}
            </div>
            <div style={{marginBottom:16}}>
              <label style={S.label}>해당하는 유형을 모두 선택해주세요</label>
              <div style={S.chipGrid}>
                {CATS.map(([c,e])=>(
                  <div key={c} style={S.chip(chips.includes(c))} onClick={()=>toggleChip(c)}>
                    <div style={{fontSize:24,marginBottom:4}}>{e}</div>
                    <div style={{fontSize:12,fontWeight:600,color:chips.includes(c)?'#ff385c':'#222'}}>{c}</div>
                  </div>
                ))}
              </div>
            </div>
            <div style={{display:'flex',gap:12,marginTop:8}}>
              <button style={{...S.submitBtn,background:'#f2f2f2',color:'#222',flex:'0 0 auto',width:'auto',padding:'0 24px'}} onClick={()=>setStep(1)}>← 이전</button>
              <button style={{...S.submitBtn,flex:1}} onMouseEnter={e=>e.currentTarget.style.background='#111'} onMouseLeave={e=>e.currentTarget.style.background='#222'} onClick={()=>setDone(true)}>가입 완료</button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

Object.assign(window, { AuthPage });
