// DetailPage.jsx — announcement detail screen

const MiniMap = ({ lat, lng, label, address }) => {
  const ref = React.useRef(null);
  const mapInst = React.useRef(null);

  React.useEffect(() => {
    if (mapInst.current || !window.L) return;
    const L = window.L;
    const map = L.map(ref.current, { center:[lat,lng], zoom:15, zoomControl:false, scrollWheelZoom:false, dragging:false });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:'© OpenStreetMap', maxZoom:18,
    }).addTo(map);

    // 커스텀 마커
    const icon = L.divIcon({
      className:'',
      html:`<div style="width:32px;height:32px;border-radius:50% 50% 50% 0;background:#ff385c;border:2.5px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.3);transform:rotate(-45deg);display:flex;align-items:center;justify-content:center;">
        <span style="transform:rotate(45deg);font-size:13px">🏢</span>
      </div>`,
      iconSize:[32,32], iconAnchor:[16,32],
    });
    L.marker([lat,lng],{icon}).addTo(map);

    // 원 표시
    L.circle([lat,lng],{color:'#ff385c',fillColor:'#ff385c',fillOpacity:0.08,weight:1.5,radius:300}).addTo(map);

    mapInst.current = map;
    return () => { map.remove(); mapInst.current = null; };
  }, []);

  return (
    <div style={{borderRadius:16,overflow:'hidden',border:'1px solid rgba(0,0,0,0.08)',position:'relative'}}>
      <div ref={ref} style={{width:'100%',height:240}} />
      {/* 주소 오버레이 */}
      <div style={{position:'absolute',bottom:0,left:0,right:0,
        background:'linear-gradient(to top,rgba(0,0,0,0.55) 0%,transparent 100%)',
        padding:'32px 16px 14px',pointerEvents:'none'}}>
        <p style={{fontSize:13,fontWeight:700,color:'#fff',marginBottom:2}}>{label}</p>
        <p style={{fontSize:12,color:'rgba(255,255,255,0.85)',display:'flex',alignItems:'center',gap:5}}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="12" height="12"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
          {address}
        </p>
      </div>
      {/* 지도 확대 버튼 */}
      <a href={`https://map.naver.com/v5/search/${encodeURIComponent(address)}`} target="_blank" rel="noreferrer"
        style={{position:'absolute',top:10,right:10,background:'#fff',border:'none',borderRadius:8,
          padding:'6px 10px',fontSize:11,fontWeight:600,color:'#222',cursor:'pointer',
          boxShadow:'rgba(0,0,0,0.12) 0px 2px 8px',textDecoration:'none',
          display:'flex',alignItems:'center',gap:4}}>
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="11" height="11"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
        네이버 지도에서 보기
      </a>
    </div>
  );
};

const DetailPage = ({ item, onBack }) => {
  const [saved, setSaved] = React.useState(false);

  const S = {
    hero: { width:'100%', height:380, background:'linear-gradient(135deg,#e8e8e8,#d0d0d0)', display:'flex',
      alignItems:'center', justifyContent:'center', fontSize:90, position:'relative', overflow:'hidden' },
    heroOverlay: { position:'absolute', inset:0, background:'linear-gradient(to top, rgba(0,0,0,0.3) 0%, transparent 50%)' },
    heroBadges: { position:'absolute', bottom:20, left:24, display:'flex', gap:8 },
    badge: { padding:'6px 14px', borderRadius:20, fontSize:13, fontWeight:600, backdropFilter:'blur(8px)' },
    layout: { maxWidth:1280, margin:'0 auto', padding:'40px 24px 80px', display:'grid', gridTemplateColumns:'1fr 360px', gap:64, alignItems:'start' },
    sectionTitle: { fontSize:20, fontWeight:700, color:'#222', letterSpacing:'-0.3px', marginBottom:20, paddingBottom:12, borderBottom:'1px solid rgba(0,0,0,0.08)' },
    infoGrid: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:0 },
    infoItem: { padding:'13px 0', borderBottom:'1px solid rgba(0,0,0,0.06)', display:'flex', alignItems:'flex-start', gap:12 },
    infoLabel: { fontSize:13, color:'#6a6a6a', fontWeight:400, minWidth:80, flexShrink:0, paddingTop:2 },
    infoValue: { fontSize:14, color:'#222', fontWeight:500, lineHeight:1.5 },
    stickyCard: { position:'sticky', top:90, background:'#fff', borderRadius:20, padding:28,
      boxShadow:'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px, rgba(0,0,0,0.1) 0px 4px 8px' },
    applyBtn: { width:'100%', height:56, borderRadius:12, background:'#ff385c', border:'none', cursor:'pointer',
      fontFamily:"'Noto Sans KR',sans-serif", fontSize:16, fontWeight:600, color:'#fff', marginBottom:12, transition:'background 0.2s' },
    saveBtn: { width:'100%', height:48, borderRadius:12, background:'transparent', border:'1px solid #c1c1c1',
      cursor:'pointer', fontFamily:"'Noto Sans KR',sans-serif", fontSize:15, fontWeight:500, color:'#222',
      display:'flex', alignItems:'center', justifyContent:'center', gap:8, marginBottom:24, transition:'border-color 0.15s' },
    quickRow: { display:'flex', justifyContent:'space-between', alignItems:'center', padding:'8px 0' },
  };

  const INFO = [
    ['공급기관','LH (한국토지주택공사)'],['공급유형','국민임대'],
    ['주택유형','아파트'],['단지명','오산세교2 21단지 (A-15블록)'],
    ['전용면적','46.01㎡'],['세대수','총 694세대 (공급 155세대)'],
    ['난방방식','지역난방'],['입주예정','2026년 10월'],
  ];

  return (
    <div>
      {/* Hero */}
      <div style={S.hero}>
        <span style={{fontSize:90}}>🏢</span>
        <div style={S.heroOverlay}></div>
        <div style={S.heroBadges}>
          <span style={{...S.badge, background:'#ff385c', color:'#fff'}}>모집중</span>
          <span style={{...S.badge, background:'rgba(255,255,255,0.9)', color:'#222'}}>국민임대</span>
          <span style={{...S.badge, background:'rgba(255,255,255,0.9)', color:'#222'}}>아파트</span>
        </div>
      </div>

      <div style={S.layout}>
        {/* Left */}
        <div>
          <p style={{fontSize:14,color:'#6a6a6a',marginBottom:8}}>LH · 경기도 오산시 초평중앙로 15</p>
          <h1 style={{fontSize:26,fontWeight:700,color:'#222',lineHeight:1.35,letterSpacing:'-0.4px',marginBottom:16}}>
            오산세교2 21단지 국민임대주택<br/>예비입주자 모집공고
          </h1>
          <div style={{display:'flex',gap:6,marginBottom:32,flexWrap:'wrap'}}>
            {[['모집중','#fff0f3','#ff385c','rgba(255,56,92,0.2)'],['국민임대','#f2f2f2','#222','transparent'],['아파트','#f2f2f2','#222','transparent'],['청년','#eff6ff','#1d4ed8','rgba(29,78,216,0.15)']].map(([l,bg,c,b])=>(
              <span key={l} style={{padding:'5px 12px',borderRadius:6,fontSize:12,fontWeight:600,background:bg,color:c,border:`1px solid ${b}`}}>{l}</span>
            ))}
          </div>

          {/* Info grid */}
          <div style={{marginBottom:40}}>
            <h2 style={S.sectionTitle}>공고 정보</h2>
            <div style={S.infoGrid}>
              {INFO.map(([l,v])=>(
                <div key={l} style={S.infoItem}>
                  <span style={S.infoLabel}>{l}</span>
                  <span style={S.infoValue}>{v}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Timeline */}
          <div style={{marginBottom:40}}>
            <h2 style={S.sectionTitle}>신청 일정</h2>
            <div style={{position:'relative',paddingLeft:24}}>
              <div style={{position:'absolute',left:8,top:0,bottom:0,width:2,background:'rgba(0,0,0,0.08)'}}></div>
              {[
                {date:'2026년 4월 4일',title:'공고일',active:false},
                {date:'2026년 4월 6일 ~ 4월 14일',title:'신청 접수 기간',sub:'2026.04.13 10:00 ~ 2026.04.15 17:00',active:true,badge:'D-6'},
                {date:'2026년 5월 7일',title:'당첨자 발표',active:false},
              ].map((t,i)=>(
                <div key={i} style={{position:'relative',marginBottom:i<2?24:0}}>
                  <div style={{position:'absolute',left:-20,top:4,width:12,height:12,borderRadius:'50%',
                    background:t.active?'#ff385c':'#d0d0d0',border:'2px solid #fff',boxShadow:`0 0 0 2px ${t.active?'#ff385c':'#d0d0d0'}`}}></div>
                  <p style={{fontSize:12,color:t.active?'#ff385c':'#6a6a6a',fontWeight:t.active?600:400,marginBottom:2}}>
                    {t.date}
                    {t.badge&&<span style={{background:'#ff385c',color:'#fff',padding:'2px 6px',borderRadius:4,fontSize:10,marginLeft:6}}>{t.badge}</span>}
                  </p>
                  <p style={{fontSize:14,fontWeight:t.active?700:600,color:'#222'}}>{t.title}</p>
                  {t.sub&&<p style={{fontSize:13,color:'#6a6a6a',marginTop:2}}>{t.sub}</p>}
                </div>
              ))}
            </div>
          </div>

          {/* 위치 안내 미니맵 */}
          <div style={{marginBottom:40}}>
            <h2 style={S.sectionTitle}>위치 안내</h2>
            <MiniMap lat={37.152} lng={127.077} label="오산세교2 21단지" address="경기도 오산시 초평중앙로 15" />
          </div>

          {/* Market comparison */}
          <div style={{marginBottom:40}}>
            <h2 style={S.sectionTitle}>시세 비교</h2>
            <div style={{background:'#fff8f9',border:'1px solid rgba(255,56,92,0.15)',borderRadius:20,padding:24}}>
              <p style={{fontSize:14,fontWeight:600,color:'#222',marginBottom:20,display:'flex',alignItems:'center',gap:8}}>
                <span style={{width:8,height:8,borderRadius:'50%',background:'#ff385c',display:'inline-block'}}></span>
                주변 시세 대비 혜택 분석
              </p>
              {[['보증금','2,800만원','1,119만원',40],['월세','32만원','149,570원',47]].map(([l,mkt,pub,pct])=>(
                <div key={l} style={{marginBottom:16}}>
                  <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
                    <span style={{fontSize:13,color:'#6a6a6a'}}>{l}</span>
                    <div style={{display:'flex',alignItems:'center',gap:12}}>
                      <span style={{fontSize:14,fontWeight:500,color:'#6a6a6a',textDecoration:'line-through'}}>{mkt}</span>
                      <span style={{fontSize:18}}>→</span>
                      <span style={{fontSize:15,fontWeight:700,color:'#ff385c'}}>{pub}</span>
                    </div>
                  </div>
                  <div style={{height:6,borderRadius:3,background:'rgba(0,0,0,0.08)',overflow:'hidden'}}>
                    <div style={{height:'100%',borderRadius:3,background:'#ff385c',width:`${pct}%`}}></div>
                  </div>
                </div>
              ))}
              <div style={{marginTop:16,padding:'12px 16px',background:'rgba(255,56,92,0.08)',borderRadius:12,fontSize:13,fontWeight:600,color:'#ff385c',textAlign:'center'}}>
                🏠 주변 시세 대비 월세 부담이 약 53% 낮음
              </div>
            </div>
          </div>
        </div>

        {/* Right — sticky card */}
        <div>
          <div style={S.stickyCard}>
            <p style={{fontSize:13,color:'#6a6a6a',marginBottom:4}}>보증금</p>
            <p style={{fontSize:26,fontWeight:700,color:'#222',letterSpacing:'-0.5px',marginBottom:4}}>1,119만원</p>
            <p style={{fontSize:13,color:'#6a6a6a',marginBottom:4}}>월 임대료</p>
            <p style={{fontSize:26,fontWeight:700,color:'#222',letterSpacing:'-0.5px',marginBottom:20}}>149,570원</p>

            <div style={{display:'flex',alignItems:'center',gap:8,padding:'12px 16px',borderRadius:12,background:'#fff0f3',marginBottom:20}}>
              <span>⏰</span>
              <span style={{fontSize:13,fontWeight:600,color:'#ff385c'}}>신청 마감 D-6 · 4월 14일까지</span>
            </div>

            <button style={S.applyBtn} onMouseEnter={e=>e.currentTarget.style.background='#e00b41'} onMouseLeave={e=>e.currentTarget.style.background='#ff385c'}>
              신청하러 가기 →
            </button>
            <button style={{...S.saveBtn, borderColor:saved?'#222':'#c1c1c1'}} onClick={()=>setSaved(s=>!s)}>
              <svg viewBox="0 0 24 24" fill={saved?'#222':'none'} stroke={saved?'#222':'currentColor'} strokeWidth="2" width="16" height="16"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              {saved ? '저장됨' : '공고 저장하기'}
            </button>

            <div style={{borderTop:'1px solid rgba(0,0,0,0.08)',paddingTop:20}}>
              {[['공급유형','국민임대'],['주택유형','아파트'],['전용면적','46.01㎡'],['공급세대','155세대'],['신청기간','4월 6일 ~ 4월 14일',true],['당첨자 발표','2026년 5월 7일']].map(([l,v,red])=>(
                <div key={l} style={S.quickRow}>
                  <span style={{fontSize:13,color:'#6a6a6a'}}>{l}</span>
                  <span style={{fontSize:13,fontWeight:600,color:red?'#ff385c':'#222'}}>{v}</span>
                </div>
              ))}
            </div>

            <div style={{marginTop:20,padding:16,background:'#f2f2f2',borderRadius:12,textAlign:'center'}}>
              <p style={{fontSize:12,color:'#6a6a6a',marginBottom:2}}>문의전화</p>
              <p style={{fontSize:20,fontWeight:700,color:'#222',letterSpacing:1}}>1600-1004</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

Object.assign(window, { DetailPage });
