// DetailPage.jsx - announcement detail screen

const GEOCODE_STATUS_META = {
  SUCCESS: {
    label: '위치 확인 완료',
    tone: { bg: '#ecfdf5', color: '#047857', border: '#a7f3d0' },
    message: '지도와 마커를 표시합니다.',
  },
  NOT_REQUESTED: {
    label: '위치 확인 중',
    tone: { bg: '#f2f2f2', color: '#6a6a6a', border: 'rgba(0,0,0,0.08)' },
    message: '위치 확인 중입니다. 주소를 먼저 확인해 주세요.',
  },
  NO_RESULT: {
    label: '위치 없음',
    tone: { bg: '#fff7ed', color: '#c2410c', border: '#fed7aa' },
    message: '주소로 위치를 찾을 수 없습니다.',
  },
  FAILED: {
    label: '불러오기 실패',
    tone: { bg: '#fff0f3', color: '#ff385c', border: 'rgba(255,56,92,0.22)' },
    message: '위치 정보를 불러오지 못했습니다.',
  },
  SKIPPED_NO_ADDRESS: {
    label: '주소 없음',
    tone: { bg: '#f8fafc', color: '#475569', border: '#e2e8f0' },
    message: '주소 정보가 없습니다.',
  },
};

const SAMPLE_UNITS = [
  {
    id: 'unit-a15-46',
    complexName: '오산세교2 21단지 (A-15블록)',
    fullAddress: '경기도 오산시 초평중앙로 15',
    latitude: 37.152,
    longitude: 127.077,
    geocodeStatus: 'SUCCESS',
    geocodedAt: '2026-05-30T10:12:00+09:00',
  },
  {
    id: 'unit-a15-36',
    complexName: '오산세교2 21단지 36형',
    fullAddress: '경기도 오산시 초평중앙로 15',
    geocodeStatus: 'NOT_REQUESTED',
  },
  {
    id: 'unit-a16-26',
    complexName: '오산세교2 예비 공급단위',
    fullAddress: '경기도 오산시 세교동 일원',
    geocodeStatus: 'NO_RESULT',
  },
];

const normalizeUnit = (unit = {}) => ({
  ...unit,
  latitude: unit.latitude ?? unit.lat,
  longitude: unit.longitude ?? unit.lng,
  geocodeStatus: unit.geocodeStatus || 'NOT_REQUESTED',
  complexName: unit.complexName || unit.name || '공급 단위',
  fullAddress: unit.fullAddress || unit.address || '',
});

const getStatusMeta = (status) => GEOCODE_STATUS_META[status] || GEOCODE_STATUS_META.NOT_REQUESTED;
const FALLBACK_STATUS_ORDER = ['NOT_REQUESTED', 'NO_RESULT', 'FAILED', 'SKIPPED_NO_ADDRESS'];

const GeocodeBadge = ({ status }) => {
  const meta = getStatusMeta(status);
  return (
    <span style={{
      display:'inline-flex', alignItems:'center', gap:5, padding:'5px 10px', borderRadius:999,
      background:meta.tone.bg, color:meta.tone.color, border:`1px solid ${meta.tone.border}`,
      fontSize:12, fontWeight:700, lineHeight:1,
    }}>
      {status === 'SUCCESS' ? '●' : '○'} {status}
    </span>
  );
};

const MiniMap = ({ unit }) => {
  const ref = React.useRef(null);
  const mapInst = React.useRef(null);
  const [ready, setReady] = React.useState(false);
  const lat = Number(unit.latitude);
  const lng = Number(unit.longitude);
  const hasCoords = Number.isFinite(lat) && Number.isFinite(lng);

  React.useEffect(() => {
    if (!hasCoords || !ref.current || !window.L) {
      setReady(true);
      return undefined;
    }

    const L = window.L;
    setReady(false);

    const map = L.map(ref.current, {
      center:[lat, lng],
      zoom:15,
      zoomControl:false,
      scrollWheelZoom:false,
      dragging:false,
      doubleClickZoom:false,
      boxZoom:false,
      keyboard:false,
      attributionControl:false,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:'© OpenStreetMap',
      maxZoom:18,
    }).addTo(map);

    const icon = L.divIcon({
      className:'',
      html:`<div style="width:34px;height:34px;border-radius:50% 50% 50% 0;background:#ff385c;border:3px solid #fff;box-shadow:0 3px 12px rgba(0,0,0,0.28);transform:rotate(-45deg);display:flex;align-items:center;justify-content:center;">
        <span style="transform:rotate(45deg);font-size:14px">🏠</span>
      </div>`,
      iconSize:[34,34],
      iconAnchor:[17,34],
    });

    L.circle([lat, lng], { color:'#ff385c', fillColor:'#ff385c', fillOpacity:0.08, weight:1.5, radius:300 }).addTo(map);
    L.marker([lat, lng], { icon }).addTo(map);

    mapInst.current = map;
    const timer = window.setTimeout(() => {
      map.invalidateSize();
      setReady(true);
    }, 220);

    return () => {
      window.clearTimeout(timer);
      map.remove();
      mapInst.current = null;
    };
  }, [lat, lng, hasCoords]);

  if (!hasCoords) return null;

  const address = unit.fullAddress || unit.complexName;

  return (
    <div style={{borderRadius:20, overflow:'hidden', border:'1px solid rgba(0,0,0,0.08)', position:'relative', background:'#f2f2f2'}}>
      <div ref={ref} style={{width:'100%', height:270}} />
      {!ready && (
        <div style={{position:'absolute', inset:0, background:'linear-gradient(90deg,#f2f2f2 0%,#fafafa 50%,#f2f2f2 100%)', display:'flex', alignItems:'center', justifyContent:'center'}}>
          <div style={{fontSize:13, fontWeight:700, color:'#6a6a6a'}}>지도 불러오는 중</div>
        </div>
      )}
      <div style={{position:'absolute', left:14, bottom:14, right:14, background:'rgba(255,255,255,0.94)', borderRadius:14, padding:'13px 14px', boxShadow:'rgba(0,0,0,0.12) 0px 4px 14px', display:'flex', justifyContent:'space-between', gap:12, alignItems:'center'}}>
        <div style={{minWidth:0}}>
          <p style={{fontSize:14, fontWeight:800, color:'#222', marginBottom:3, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap'}}>{unit.complexName}</p>
          <p style={{fontSize:12, color:'#6a6a6a', display:'flex', alignItems:'center', gap:5, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap'}}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="13" height="13"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
            {address}
          </p>
        </div>
        <a href={`https://map.naver.com/v5/search/${encodeURIComponent(address)}`} target="_blank" rel="noreferrer" style={{flexShrink:0, padding:'8px 10px', borderRadius:10, background:'#222', color:'#fff', fontSize:12, fontWeight:700, textDecoration:'none'}}>지도 보기</a>
      </div>
    </div>
  );
};

const LocationFallback = ({ unit }) => {
  const meta = getStatusMeta(unit.geocodeStatus);
  const address = unit.fullAddress || unit.complexName || '';
  return (
    <div style={{border:'1px dashed #c1c1c1', borderRadius:20, minHeight:220, background:'#fafafa', display:'flex', alignItems:'center', justifyContent:'center', padding:24, textAlign:'center'}}>
      <div>
        <div style={{width:52, height:52, borderRadius:'50%', background:meta.tone.bg, color:meta.tone.color, display:'flex', alignItems:'center', justifyContent:'center', margin:'0 auto 14px'}}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="24" height="24"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
        </div>
        <p style={{fontSize:16, fontWeight:800, color:'#222', marginBottom:6}}>{meta.message}</p>
        {address ? <p style={{fontSize:13, color:'#6a6a6a', lineHeight:1.6, marginBottom:14}}>주소: {address}</p> : <p style={{fontSize:13, color:'#6a6a6a', marginBottom:14}}>상세 주소가 등록되면 위치를 다시 확인합니다.</p>}
        <GeocodeBadge status={unit.geocodeStatus} />
      </div>
    </div>
  );
};

const LocationSection = ({ units }) => {
  const normalizedUnits = (units && units.length ? units : SAMPLE_UNITS).map(normalizeUnit);
  const representative = normalizedUnits.find((unit) => {
    const lat = Number(unit.latitude);
    const lng = Number(unit.longitude);
    return unit.geocodeStatus === 'SUCCESS' && Number.isFinite(lat) && Number.isFinite(lng);
  });
  const fallbackUnit = representative || normalizedUnits[0] || normalizeUnit({ geocodeStatus:'SKIPPED_NO_ADDRESS' });
  const successCount = normalizedUnits.filter((unit) => unit.geocodeStatus === 'SUCCESS').length;

  return (
    <div style={{marginBottom:40}}>
      <div style={{display:'flex', justifyContent:'space-between', alignItems:'flex-end', gap:16, borderBottom:'1px solid rgba(0,0,0,0.08)', paddingBottom:12, marginBottom:20}}>
        <div>
          <h2 style={{fontSize:20, fontWeight:700, color:'#222', letterSpacing:'-0.3px', marginBottom:6}}>위치 안내</h2>
          <p style={{fontSize:13, color:'#6a6a6a'}}>좌표는 공고가 아닌 공급 단위(units[]) 기준으로 표시됩니다.</p>
        </div>
        <span style={{padding:'7px 12px', borderRadius:999, background:'#fff0f3', color:'#ff385c', fontSize:12, fontWeight:800, whiteSpace:'nowrap'}}>대표 위치 MVP</span>
      </div>

      <div style={{display:'grid', gridTemplateColumns:'minmax(0,1fr) 280px', gap:18, alignItems:'stretch'}}>
        <div>
          {representative ? <MiniMap unit={representative} /> : <LocationFallback unit={fallbackUnit} />}
        </div>
        <div style={{border:'1px solid rgba(0,0,0,0.08)', borderRadius:20, padding:18, background:'#fff', boxShadow:'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px'}}>
          <p style={{fontSize:13, color:'#6a6a6a', fontWeight:600, marginBottom:6}}>표시 정책</p>
          <p style={{fontSize:18, fontWeight:800, color:'#222', letterSpacing:'-0.3px', marginBottom:8}}>{representative ? representative.complexName : '주소 텍스트로 대체 표시'}</p>
          <p style={{fontSize:13, color:'#6a6a6a', lineHeight:1.6, marginBottom:14}}>
            {representative
              ? `SUCCESS 좌표가 있는 ${successCount}개 공급 단위 중 첫 번째 unit을 대표 위치로 표시합니다.`
              : 'SUCCESS 좌표가 없으면 지도 대신 상태별 placeholder와 주소 fallback을 보여줍니다.'}
          </p>
          <GeocodeBadge status={fallbackUnit.geocodeStatus} />
          {fallbackUnit.geocodedAt && <p style={{fontSize:12, color:'#6a6a6a', marginTop:10}}>최근 확인: {String(fallbackUnit.geocodedAt).replace('T', ' ').slice(0, 16)}</p>}
        </div>
      </div>

      <div style={{marginTop:14, display:'grid', gridTemplateColumns:'repeat(3, minmax(0,1fr))', gap:10}}>
        {normalizedUnits.slice(0, 3).map((unit, index) => (
          <div key={unit.id || index} style={{border:'1px solid rgba(0,0,0,0.08)', borderRadius:14, padding:12, background:index === 0 && representative === unit ? '#fff8f9' : '#fff'}}>
            <div style={{display:'flex', justifyContent:'space-between', gap:8, alignItems:'center', marginBottom:8}}>
              <p style={{fontSize:13, fontWeight:800, color:'#222', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap'}}>{unit.complexName}</p>
              {representative === unit && <span style={{fontSize:10, fontWeight:800, color:'#ff385c', flexShrink:0}}>대표</span>}
            </div>
            <p style={{fontSize:12, color:'#6a6a6a', lineHeight:1.45, minHeight:34, marginBottom:10}}>{unit.fullAddress || getStatusMeta(unit.geocodeStatus).message}</p>
            <GeocodeBadge status={unit.geocodeStatus} />
          </div>
        ))}
      </div>

      <div style={{marginTop:12, border:'1px solid rgba(0,0,0,0.08)', borderRadius:14, padding:14, background:'#fafafa'}}>
        <p style={{fontSize:12, fontWeight:800, color:'#222', marginBottom:10}}>좌표가 없을 때 fallback 문구</p>
        <div style={{display:'grid', gridTemplateColumns:'repeat(2, minmax(0,1fr))', gap:10}}>
          {FALLBACK_STATUS_ORDER.map((status) => (
            <div key={status} style={{display:'flex', alignItems:'center', justifyContent:'space-between', gap:10}}>
              <span style={{fontSize:12, color:'#6a6a6a', lineHeight:1.4}}>{getStatusMeta(status).message}</span>
              <GeocodeBadge status={status} />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

const DetailPage = ({ item, onBack }) => {
  const [saved, setSaved] = React.useState(false);
  const units = item?.units;

  const S = {
    hero: { width:'100%', height:380, background:'linear-gradient(135deg,#e8e8e8,#d0d0d0)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:90, position:'relative', overflow:'hidden' },
    heroOverlay: { position:'absolute', inset:0, background:'linear-gradient(to top, rgba(0,0,0,0.3) 0%, transparent 50%)' },
    heroBadges: { position:'absolute', bottom:20, left:24, display:'flex', gap:8 },
    badge: { padding:'6px 14px', borderRadius:20, fontSize:13, fontWeight:600, backdropFilter:'blur(8px)' },
    layout: { maxWidth:1280, margin:'0 auto', padding:'40px 24px 80px', display:'grid', gridTemplateColumns:'1fr 360px', gap:64, alignItems:'start' },
    sectionTitle: { fontSize:20, fontWeight:700, color:'#222', letterSpacing:'-0.3px', marginBottom:20, paddingBottom:12, borderBottom:'1px solid rgba(0,0,0,0.08)' },
    infoGrid: { display:'grid', gridTemplateColumns:'1fr 1fr', gap:0 },
    infoItem: { padding:'13px 0', borderBottom:'1px solid rgba(0,0,0,0.06)', display:'flex', alignItems:'flex-start', gap:12 },
    infoLabel: { fontSize:13, color:'#6a6a6a', fontWeight:400, minWidth:80, flexShrink:0, paddingTop:2 },
    infoValue: { fontSize:14, color:'#222', fontWeight:500, lineHeight:1.5 },
    stickyCard: { position:'sticky', top:90, background:'#fff', borderRadius:20, padding:28, boxShadow:'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px, rgba(0,0,0,0.1) 0px 4px 8px' },
    applyBtn: { width:'100%', height:56, borderRadius:12, background:'#ff385c', border:'none', cursor:'pointer', fontFamily:"'Noto Sans KR',sans-serif", fontSize:16, fontWeight:600, color:'#fff', marginBottom:12, transition:'background 0.2s' },
    saveBtn: { width:'100%', height:48, borderRadius:12, background:'transparent', border:'1px solid #c1c1c1', cursor:'pointer', fontFamily:"'Noto Sans KR',sans-serif", fontSize:15, fontWeight:500, color:'#222', display:'flex', alignItems:'center', justifyContent:'center', gap:8, marginBottom:24, transition:'border-color 0.15s' },
    quickRow: { display:'flex', justifyContent:'space-between', alignItems:'center', padding:'8px 0' },
  };

  const INFO = [
    ['공급기관','LH (한국토지주택공사)'], ['공급유형','국민임대'],
    ['주택유형','아파트'], ['단지명','오산세교2 21단지 (A-15블록)'],
    ['전용면적','46.01㎡'], ['세대수','총 694세대 (공급 155세대)'],
    ['난방방식','지역난방'], ['입주예정','2026년 10월'],
  ];

  return (
    <div>
      <div style={S.hero}>
        <span style={{fontSize:90}}>🏠</span>
        <div style={S.heroOverlay}></div>
        <div style={S.heroBadges}>
          <span style={{...S.badge, background:'#ff385c', color:'#fff'}}>모집중</span>
          <span style={{...S.badge, background:'rgba(255,255,255,0.9)', color:'#222'}}>국민임대</span>
          <span style={{...S.badge, background:'rgba(255,255,255,0.9)', color:'#222'}}>아파트</span>
        </div>
      </div>

      <div style={S.layout}>
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
                  <div style={{position:'absolute',left:-20,top:4,width:12,height:12,borderRadius:'50%',background:t.active?'#ff385c':'#d0d0d0',border:'2px solid #fff',boxShadow:`0 0 0 2px ${t.active?'#ff385c':'#d0d0d0'}`}}></div>
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

          <LocationSection units={units} />

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
                주변 시세 대비 월세 부담이 약 53% 낮음
              </div>
            </div>
          </div>
        </div>

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
