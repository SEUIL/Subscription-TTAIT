// MyPage.jsx — profile, saved listings, notifications
const MyPage = ({ onCardClick }) => {
  const [section, setSection] = React.useState('profile');
  const [toggles, setToggles] = React.useState({d3:true, d1:true, new:false});
  const [deposit, setDeposit] = React.useState(20000000);
  const [rent, setRent] = React.useState(300000);
  const [chips, setChips] = React.useState(['청년','무주택자']);
  const toggleChip = (c) => setChips(p => p.includes(c)?p.filter(x=>x!==c):[...p,c]);

  const SAVED = [
    {emoji:'🏢',title:'오산세교2 21단지 국민임대주택 예비입주자 모집공고',meta:'LH · 경기도 오산시 · 보증금 1,119만원 / 월세 149,570원',deadline:'마감 D-6 · 4월 14일',status:'open',statusLabel:'모집중'},
    {emoji:'🏗️',title:'26년 2차 신혼·신생아Ⅰ 예비입주자 모집공고 (인천,부천)',meta:'LH · 인천광역시 · 보증금 8,500만원 / 월세 320,000원',deadline:'마감 D-1 · 오늘!',status:'closing',statusLabel:'마감임박'},
  ];
  const NOTIFS = [
    {unread:true,title:'[공고 알림] 마감 하루 전',msg:'[정정공고][인천지역본부]26년 2차 신혼·신생아Ⅰ 예비입주자 모집공고(인천,부천) 공고가 마감 하루 전입니다. (마감일: 2026-04-08)',time:'2026년 4월 7일 오후 5:03'},
    {unread:false,title:'[공고 알림] 새 추천 공고',msg:'회원님의 조건에 맞는 새로운 공고가 등록되었습니다. 지금 확인해보세요.',time:'2026년 4월 6일 오전 9:00'},
    {unread:false,title:'[공고 알림] 마감 3일 전',msg:'저장하신 오산세교2 21단지 공고가 마감 3일 전입니다. (마감일: 2026-04-14)',time:'2026년 4월 5일 오전 9:00'},
  ];

  const fmtMoney = v => (v/10000).toLocaleString()+'만원';

  const S = {
    layout: { maxWidth:960, margin:'0 auto', padding:'32px 24px 80px', display:'grid', gridTemplateColumns:'240px 1fr', gap:24, alignItems:'start' },
    sidebar: { background:'#fff', borderRadius:20, overflow:'hidden',
      boxShadow:'rgba(0,0,0,0.02) 0px 0px 0px 1px,rgba(0,0,0,0.04) 0px 2px 6px,rgba(0,0,0,0.1) 0px 4px 8px',
      position:'sticky', top:90 },
    navItem: (active) => ({ display:'flex', alignItems:'center', gap:10, padding:'12px 20px', cursor:'pointer',
      fontSize:14, fontWeight:active?600:500, color:active?'#ff385c':'#6a6a6a',
      background:active?'#fff0f3':'transparent', transition:'background 0.15s,color 0.15s',
      border:'none', width:'100%', textAlign:'left', fontFamily:"'Noto Sans KR',sans-serif" }),
    card: { background:'#fff', borderRadius:20, marginBottom:16, overflow:'hidden',
      boxShadow:'rgba(0,0,0,0.02) 0px 0px 0px 1px,rgba(0,0,0,0.04) 0px 2px 6px,rgba(0,0,0,0.1) 0px 4px 8px' },
    cardHeader: { padding:'20px 24px 16px', borderBottom:'1px solid rgba(0,0,0,0.06)', display:'flex', alignItems:'center', justifyContent:'space-between' },
    cardTitle: { fontSize:16, fontWeight:700, color:'#222' },
    cardBadge: { padding:'4px 10px', borderRadius:20, fontSize:11, fontWeight:700, background:'#fff0f3', color:'#ff385c' },
    label: { display:'block', fontSize:12, fontWeight:600, color:'#6a6a6a', marginBottom:6, textTransform:'uppercase', letterSpacing:'0.3px' },
    input: { width:'100%', height:46, padding:'0 14px', border:'1.5px solid rgba(0,0,0,0.12)', borderRadius:10,
      fontFamily:"'Noto Sans KR',sans-serif", fontSize:14, color:'#222', background:'#fff', outline:'none', boxSizing:'border-box' },
    row2: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:16, marginBottom:16 },
    saveBtn: { width:'100%', height:52, borderRadius:12, background:'#222', border:'none', cursor:'pointer',
      fontFamily:"'Noto Sans KR',sans-serif", fontSize:15, fontWeight:600, color:'#fff', marginTop:8, transition:'background 0.2s' },
    toggleSwitch: (on) => ({ width:44, height:24, borderRadius:12, background:on?'#ff385c':'#c1c1c1', border:'none', cursor:'pointer',
      position:'relative', transition:'background 0.2s', flexShrink:0 }),
    chip: (sel) => ({ padding:'8px 16px', borderRadius:20, border:`1.5px solid ${sel?'#ff385c':'#c1c1c1'}`,
      fontFamily:"'Noto Sans KR',sans-serif", fontSize:13, fontWeight:600,
      color:sel?'#ff385c':'#6a6a6a', background:sel?'#fff0f3':'#fff',
      cursor:'pointer', display:'flex', alignItems:'center', gap:6, transition:'all 0.15s' }),
  };

  const Toggle = ({on, onToggle}) => (
    <button style={S.toggleSwitch(on)} onClick={onToggle}>
      <div style={{position:'absolute',width:20,height:20,borderRadius:'50%',background:'#fff',top:2,left:2,transition:'transform 0.2s',transform:on?'translateX(20px)':'none',boxShadow:'0 1px 3px rgba(0,0,0,0.2)'}}></div>
    </button>
  );

  const NAV_ITEMS = [
    {id:'profile',label:'내 프로필',icon:<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>},
    {id:'saved',label:'저장한 공고',icon:<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>},
    {id:'notifications',label:'알림',icon:<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>,badge:'1'},
  ];

  const CATS = [['청년','👤'],['신혼부부','💑'],['무주택자','🏠'],['고령자','👴'],['저소득층','💰'],['다자녀','👨‍👩‍👧‍👦']];

  return (
    <div style={{background:'#f2f2f2', minHeight:'100vh'}}>
      <div style={S.layout}>
        {/* Sidebar */}
        <div style={S.sidebar}>
          <div style={{padding:24, textAlign:'center', borderBottom:'1px solid rgba(0,0,0,0.06)'}}>
            <div style={{width:72,height:72,borderRadius:'50%',background:'#222',display:'flex',alignItems:'center',justifyContent:'center',margin:'0 auto 12px',fontSize:28,color:'#fff',fontWeight:700}}>관</div>
            <p style={{fontSize:16,fontWeight:700,color:'#222',marginBottom:2}}>관리자</p>
            <p style={{fontSize:13,color:'#6a6a6a'}}>@admin</p>
          </div>
          <div style={{padding:'8px 0'}}>
            {NAV_ITEMS.map(n=>(
              <button key={n.id} style={S.navItem(section===n.id)} onClick={()=>setSection(n.id)}>
                {n.icon}{n.label}
                {n.badge&&<span style={{marginLeft:'auto',background:'#ff385c',color:'#fff',fontSize:10,fontWeight:700,padding:'2px 6px',borderRadius:10}}>{n.badge}</span>}
              </button>
            ))}
            <div style={{height:1,background:'rgba(0,0,0,0.06)',margin:'4px 0'}}></div>
            <button style={{display:'flex',alignItems:'center',gap:10,padding:'12px 20px',cursor:'pointer',fontSize:14,fontWeight:500,color:'#ff385c',border:'none',background:'transparent',width:'100%',textAlign:'left',fontFamily:"'Noto Sans KR',sans-serif"}}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="15" height="15"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
              로그아웃
            </button>
          </div>
        </div>

        {/* Main */}
        <div>
          {/* PROFILE */}
          {section==='profile' && <>
            <div style={S.card}>
              <div style={S.cardHeader}><span style={S.cardTitle}>기본 정보</span></div>
              <div style={{padding:'20px 24px'}}>
                <div style={S.row2}>
                  {[['아이디','text','admin',true],['이메일','email','admin@example.com',false],['휴대폰 번호','tel','010-0000-0000',false],['나이','number','29',false]].map(([l,t,v,ro])=>(
                    <div key={l}>
                      <label style={S.label}>{l}</label>
                      <input style={{...S.input,background:ro?'#f2f2f2':undefined,color:ro?'#6a6a6a':undefined}} type={t} defaultValue={v} readOnly={ro} />
                    </div>
                  ))}
                </div>
                <div style={S.row2}>
                  {[['혼인 상태',['미혼','기혼','기타']],['자녀 수',[]]].map(([l,opts])=>(
                    <div key={l}>
                      <label style={S.label}>{l}</label>
                      {opts.length?<select style={{...S.input,cursor:'pointer'}}>{opts.map(o=><option key={o}>{o}</option>)}</select>:<input style={S.input} type="number" defaultValue="0" min="0"/>}
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div style={S.card}>
              <div style={S.cardHeader}><span style={S.cardTitle}>주거 선호 설정</span></div>
              <div style={{padding:'20px 24px'}}>
                <div style={S.row2}>
                  {[['선호 지역 (시/도)',['경기도','서울특별시','인천광역시']],['선호 지역 (시/군/구)',['오산시','수원시','용인시']]].map(([l,opts])=>(
                    <div key={l}>
                      <label style={S.label}>{l}</label>
                      <select style={{...S.input,cursor:'pointer'}}>{opts.map(o=><option key={o}>{o}</option>)}</select>
                    </div>
                  ))}
                </div>
                <div style={{marginTop:8}}>
                  <div style={{display:'flex',justifyContent:'space-between',marginBottom:8}}>
                    <span style={{fontSize:13,color:'#6a6a6a'}}>최대 보증금</span>
                    <span style={{fontSize:16,fontWeight:700,color:'#222'}}>{fmtMoney(deposit)}</span>
                  </div>
                  <input type="range" min="0" max="100000000" step="1000000" value={deposit} onChange={e=>setDeposit(+e.target.value)}
                    style={{width:'100%',height:4,borderRadius:2,background:'#f2f2f2',outline:'none',border:'none',cursor:'pointer',WebkitAppearance:'none'}} />
                  <div style={{display:'flex',justifyContent:'space-between',marginTop:16,marginBottom:8}}>
                    <span style={{fontSize:13,color:'#6a6a6a'}}>최대 월세</span>
                    <span style={{fontSize:16,fontWeight:700,color:'#222'}}>{fmtMoney(rent)}</span>
                  </div>
                  <input type="range" min="0" max="1000000" step="10000" value={rent} onChange={e=>setRent(+e.target.value)}
                    style={{width:'100%',height:4,borderRadius:2,background:'#f2f2f2',outline:'none',border:'none',cursor:'pointer',WebkitAppearance:'none'}} />
                </div>
              </div>
            </div>

            <div style={S.card}>
              <div style={S.cardHeader}><span style={S.cardTitle}>해당 유형 선택</span><span style={S.cardBadge}>추천 정확도에 영향</span></div>
              <div style={{padding:'20px 24px'}}>
                <div style={{display:'flex',flexWrap:'wrap',gap:8}}>
                  {CATS.map(([c,e])=>(
                    <button key={c} style={S.chip(chips.includes(c))} onClick={()=>toggleChip(c)}>
                      <span>{e}</span>{c}
                    </button>
                  ))}
                </div>
                <button style={S.saveBtn} onMouseEnter={e=>e.currentTarget.style.background='#111'} onMouseLeave={e=>e.currentTarget.style.background='#222'}>변경 사항 저장</button>
              </div>
            </div>
          </>}

          {/* SAVED */}
          {section==='saved' && (
            <div style={S.card}>
              <div style={S.cardHeader}><span style={S.cardTitle}>저장한 공고</span><span style={S.cardBadge}>2개</span></div>
              <div style={{padding:'20px 24px',display:'flex',flexDirection:'column',gap:12}}>
                {SAVED.map((s,i)=>(
                  <div key={i} style={{display:'flex',alignItems:'center',gap:16,padding:16,borderRadius:14,border:'1px solid rgba(0,0,0,0.08)',cursor:'pointer',transition:'box-shadow 0.15s'}}
                    onMouseEnter={e=>e.currentTarget.style.boxShadow='rgba(0,0,0,0.08) 0px 4px 12px'} onMouseLeave={e=>e.currentTarget.style.boxShadow='none'}
                    onClick={()=>onCardClick&&onCardClick()}>
                    <div style={{width:64,height:64,borderRadius:10,background:'#f2f2f2',flexShrink:0,display:'flex',alignItems:'center',justifyContent:'center',fontSize:28}}>{s.emoji}</div>
                    <div style={{flex:1}}>
                      <p style={{fontSize:14,fontWeight:600,color:'#222',lineHeight:1.4,marginBottom:4}}>{s.title}</p>
                      <p style={{fontSize:12,color:'#6a6a6a'}}>{s.meta}</p>
                      <p style={{fontSize:12,color:'#ff385c',fontWeight:600,marginTop:2}}>{s.deadline}</p>
                    </div>
                    <span style={{fontSize:11,fontWeight:700,padding:'4px 8px',borderRadius:6,
                      background:s.status==='closing'?'#ff385c':'#fff0f3',color:s.status==='closing'?'#fff':'#ff385c',flexShrink:0}}>
                      {s.statusLabel}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* NOTIFICATIONS */}
          {section==='notifications' && <>
            <div style={S.card}>
              <div style={S.cardHeader}><span style={S.cardTitle}>알림</span><span style={S.cardBadge}>읽지 않은 알림 1개</span></div>
              <div style={{padding:'8px 24px'}}>
                {NOTIFS.map((n,i)=>(
                  <div key={i} style={{display:'flex',alignItems:'flex-start',gap:14,padding:'12px 8px',marginLeft:-8,marginRight:-8,borderBottom:i<NOTIFS.length-1?'1px solid rgba(0,0,0,0.06)':'none',cursor:'pointer',borderRadius:8,transition:'background 0.1s'}}
                    onMouseEnter={e=>e.currentTarget.style.background='#f2f2f2'} onMouseLeave={e=>e.currentTarget.style.background='transparent'}>
                    <div style={{width:8,height:8,borderRadius:'50%',background:n.unread?'#ff385c':'transparent',border:n.unread?'none':'2px solid #c1c1c1',flexShrink:0,marginTop:6}}></div>
                    <div style={{flex:1}}>
                      <p style={{fontSize:13,fontWeight:600,color:n.unread?'#222':'#6a6a6a',marginBottom:3}}>{n.title}</p>
                      <p style={{fontSize:12,color:'#6a6a6a',lineHeight:1.5}}>{n.msg}</p>
                      <p style={{fontSize:11,color:'#c1c1c1',marginTop:4}}>{n.time}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div style={S.card}>
              <div style={S.cardHeader}><span style={S.cardTitle}>알림 설정</span></div>
              <div style={{padding:'8px 24px'}}>
                {[['d3','마감 D-3 알림','저장한 공고 마감 3일 전 알림'],['d1','마감 D-1 알림','저장한 공고 마감 하루 전 알림'],['new','새 추천 공고 알림','내 조건에 맞는 새 공고 등록 시 알림']].map(([k,l,d])=>(
                  <div key={k} style={{display:'flex',alignItems:'center',justifyContent:'space-between',padding:'14px 0',borderBottom:'1px solid rgba(0,0,0,0.06)'}}>
                    <div>
                      <p style={{fontSize:14,fontWeight:600,color:'#222',marginBottom:2}}>{l}</p>
                      <p style={{fontSize:12,color:'#6a6a6a'}}>{d}</p>
                    </div>
                    <Toggle on={toggles[k]} onToggle={()=>setToggles(p=>({...p,[k]:!p[k]}))} />
                  </div>
                ))}
              </div>
            </div>
          </>}
        </div>
      </div>
    </div>
  );
};

Object.assign(window, { MyPage });
