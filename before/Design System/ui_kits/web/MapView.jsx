// MapView.jsx — 지도 뷰 (Leaflet, 카테고리/상태 필터 지원)

const MAP_LISTINGS = [
  { id:1, emoji:'🏢', lat:37.152, lng:127.077, category:'국민임대', provider:'LH · 경기도 오산시', title:'오산세교2 21단지 국민임대주택 예비입주자 모집공고', price:'보증금 1,119만원 / 월세 149,570원', deadline:'마감 D-6 · 4월 14일', urgent:true, status:'open', statusLabel:'모집중', tags:[{l:'청년',c:'blue'},{l:'국민임대',c:'red'}] },
  { id:2, emoji:'🏗️', lat:37.456, lng:126.705, category:'행복주택', provider:'LH · 인천광역시', title:'26년 2차 신혼·신생아Ⅰ 예비입주자 모집공고 (인천,부천)', price:'보증금 8,500만원 / 월세 320,000원', deadline:'마감 D-1 · 오늘!', urgent:true, status:'closing', statusLabel:'마감임박', tags:[{l:'신혼부부',c:'purple'},{l:'행복주택',c:'red'}] },
  { id:3, emoji:'🏠', lat:37.559, lng:126.829, category:'영구임대', provider:'LH · 서울특별시', title:'서울 강서구 마곡지구 영구임대 입주자 모집공고', price:'보증금 2,340만원 / 월세 98,000원', deadline:'마감 D-15 · 4월 23일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'영구임대',c:'red'},{l:'저소득',c:'green'}] },
  { id:4, emoji:'🏘️', lat:37.277, lng:127.045, category:'공공분양', provider:'LH · 경기도 수원시', title:'수원 광교신도시 공공분양 (일반공급) 입주자 모집', price:'분양가 3억 2,000만원', deadline:'마감 D-2 · 4월 10일', urgent:true, status:'closing', statusLabel:'마감임박', tags:[{l:'공공분양',c:'red'}] },
  { id:5, emoji:'🏙️', lat:37.507, lng:126.722, category:'행복주택', provider:'LH · 인천광역시 부평구', title:'부평구 청천동 행복주택 입주자 모집공고', price:'보증금 5,200만원 / 월세 230,000원', deadline:'마감 D-20 · 4월 28일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'청년',c:'blue'},{l:'행복주택',c:'red'}] },
  { id:6, emoji:'🏛️', lat:37.656, lng:127.063, category:'국민임대', provider:'LH · 서울특별시 노원구', title:'노원구 상계동 국민임대주택 입주자 모집공고', price:'보증금 3,800만원 / 월세 175,000원', deadline:'모집 종료', urgent:false, status:'closed', statusLabel:'마감', tags:[{l:'국민임대',c:'neutral'}] },
  { id:7, emoji:'🏢', lat:37.200, lng:127.075, category:'매입임대', provider:'LH · 경기도 화성시', title:'화성 동탄2 매입임대주택 입주자 모집공고', price:'보증금 1,800만원 / 월세 210,000원', deadline:'마감 D-30 · 5월 8일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'매입임대',c:'red'},{l:'고령자',c:'orange'}] },
  { id:8, emoji:'🏗️', lat:36.992, lng:127.113, category:'국민임대', provider:'LH · 경기도 평택시', title:'평택 고덕신도시 국민임대주택 예비입주자 모집공고', price:'보증금 9,240만원 / 월세 188,000원', deadline:'마감 D-12 · 4월 20일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'신혼부부',c:'purple'},{l:'청년',c:'blue'}] },
];

const MAP_TAG_STYLES = {
  red:    { background:'#fff0f3', color:'#ff385c', border:'1px solid rgba(255,56,92,0.2)' },
  blue:   { background:'#eff6ff', color:'#1d4ed8', border:'1px solid rgba(29,78,216,0.2)' },
  purple: { background:'#fdf4ff', color:'#7e22ce', border:'1px solid rgba(126,34,206,0.2)' },
  green:  { background:'#f0fdf4', color:'#166534', border:'1px solid rgba(22,101,52,0.2)' },
  orange: { background:'#fff7ed', color:'#c2410c', border:'1px solid rgba(194,65,12,0.2)' },
  neutral:{ background:'#f2f2f2', color:'#222', border:'1px solid transparent' },
};

const STATUS_COLORS_MAP = { open:'#222', closing:'#ff385c', closed:'#aaa' };
const STATUS_FILTERS = ['전체', '모집중', '마감임박', '마감'];
const STATUS_MAP = { '모집중':'open', '마감임박':'closing', '마감':'closed' };

const MapView = ({ activeCategory = 'all', onDetailClick }) => {
  const mapRef = React.useRef(null);
  const leafletMap = React.useRef(null);
  const markersRef = React.useRef({});
  const [selected, setSelected] = React.useState(null);
  const [cardPos, setCardPos] = React.useState({ x: 0, y: 0 });
  const [statusFilter, setStatusFilter] = React.useState('전체');

  // Derived filtered list
  const filtered = React.useMemo(() => {
    return MAP_LISTINGS.filter(item => {
      const catOk = activeCategory === 'all' || item.category === activeCategory;
      const stOk = statusFilter === '전체' || item.status === STATUS_MAP[statusFilter];
      return catOk && stOk;
    });
  }, [activeCategory, statusFilter]);

  // Init map once
  React.useEffect(() => {
    if (leafletMap.current) return;
    if (!mapRef.current || !window.L) return;
    const L = window.L;
    const map = L.map(mapRef.current, { center:[37.35,127.0], zoom:10, zoomControl:true });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution:'© OpenStreetMap', maxZoom:18,
    }).addTo(map);
    map.on('click', () => setSelected(null));
    leafletMap.current = map;
    return () => { map.remove(); leafletMap.current = null; };
  }, []);

  // Sync markers when filter changes
  React.useEffect(() => {
    const L = window.L;
    const map = leafletMap.current;
    if (!map || !L) return;

    // Remove all existing markers
    Object.values(markersRef.current).forEach(m => m.remove());
    markersRef.current = {};

    filtered.forEach(item => {
      const color = STATUS_COLORS_MAP[item.status];
      const icon = L.divIcon({
        className: '',
        html: `<div style="width:36px;height:36px;border-radius:50% 50% 50% 0;background:${color};border:2.5px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.25);transform:rotate(-45deg);display:flex;align-items:center;justify-content:center;cursor:pointer;transition:transform 0.15s;">
          <span style="transform:rotate(45deg);font-size:14px;line-height:1">${item.emoji}</span>
        </div>`,
        iconSize:[36,36], iconAnchor:[18,36],
      });

      const marker = L.marker([item.lat, item.lng], { icon }).addTo(map);
      marker.on('click', e => {
        const pt = map.latLngToContainerPoint(e.latlng);
        setCardPos({ x: pt.x, y: pt.y });
        setSelected(item);
        map.panTo([item.lat, item.lng], { animate:true, duration:0.4 });
      });
      markersRef.current[item.id] = marker;
    });

    // Deselect if selected item no longer in filtered
    setSelected(prev => prev && filtered.find(i => i.id === prev.id) ? prev : null);
  }, [filtered]);

  const popupStyle = () => {
    const cW = 300, cH = 220;
    const mW = mapRef.current ? mapRef.current.offsetWidth : 800;
    let x = cardPos.x - cW / 2;
    let y = cardPos.y - cH - 52;
    x = Math.max(8, Math.min(x, mW - cW - 8));
    if (y < 8) y = cardPos.y + 20;
    return {
      position:'absolute', left:x, top:y, width:cW, zIndex:1000,
      background:'#fff', borderRadius:16, padding:16,
      boxShadow:'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.12) 0px 8px 24px',
      animation:'mapFadeUp 0.16s ease',
    };
  };

  return (
    <div style={{position:'relative', width:'100%', height:'calc(100vh - 168px)', minHeight:480}}>
      <style>{`
        @keyframes mapFadeUp { from{opacity:0;transform:translateY(6px)} to{opacity:1;transform:translateY(0)} }
        .leaflet-container { font-family:'Noto Sans KR',sans-serif !important; }
        .leaflet-control-attribution { font-size:10px !important; }
      `}</style>

      <div ref={mapRef} style={{width:'100%', height:'100%'}} />

      {/* 상태 필터 pills — 지도 위 */}
      <div style={{position:'absolute',top:12,left:'50%',transform:'translateX(-50%)',zIndex:900,
        display:'flex',gap:6,background:'#fff',padding:'6px 8px',borderRadius:40,
        boxShadow:'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.1) 0px 4px 12px'}}>
        {STATUS_FILTERS.map(s => (
          <button key={s} onClick={() => setStatusFilter(s)} style={{
            padding:'6px 14px', borderRadius:20, border:'none', cursor:'pointer',
            fontFamily:"'Noto Sans KR',sans-serif", fontSize:12, fontWeight:600,
            background: statusFilter===s ? '#222' : 'transparent',
            color: statusFilter===s ? '#fff' : '#6a6a6a',
            transition:'all 0.15s',
          }}>{s}</button>
        ))}
      </div>

      {/* 공고 수 표시 */}
      <div style={{position:'absolute',bottom:24,right:12,zIndex:900,
        background:'#fff',borderRadius:20,padding:'6px 14px',
        boxShadow:'rgba(0,0,0,0.08) 0px 2px 8px',
        fontSize:12,fontWeight:600,color:'#222',display:'flex',alignItems:'center',gap:6}}>
        <span style={{width:7,height:7,borderRadius:'50%',background:'#ff385c',display:'inline-block'}}></span>
        {filtered.length}개 공고
      </div>

      {/* 범례 */}
      <div style={{position:'absolute',bottom:24,left:12,zIndex:900,
        background:'#fff',borderRadius:12,padding:'10px 14px',
        boxShadow:'rgba(0,0,0,0.08) 0px 2px 8px',
        display:'flex',flexDirection:'column',gap:6}}>
        {[['#222','모집중'],['#ff385c','마감임박'],['#aaa','마감']].map(([c,l])=>(
          <div key={l} style={{display:'flex',alignItems:'center',gap:8,fontSize:11,fontWeight:600,color:'#222'}}>
            <div style={{width:11,height:11,borderRadius:'50%',background:c}}></div>{l}
          </div>
        ))}
      </div>

      {/* 팝업 요약 카드 */}
      {selected && (
        <div style={popupStyle()}>
          <button onClick={e=>{e.stopPropagation();setSelected(null);}}
            style={{position:'absolute',top:10,right:10,width:24,height:24,borderRadius:'50%',
              background:'#f2f2f2',border:'none',cursor:'pointer',display:'flex',alignItems:'center',justifyContent:'center'}}>
            <svg viewBox="0 0 24 24" fill="none" stroke="#222" strokeWidth="2.5" width="11" height="11"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>

          <div style={{display:'flex',alignItems:'center',gap:10,marginBottom:10}}>
            <div style={{width:44,height:44,borderRadius:10,background:'#f2f2f2',display:'flex',alignItems:'center',justifyContent:'center',fontSize:22,flexShrink:0}}>
              {selected.emoji}
            </div>
            <span style={{fontSize:11,fontWeight:700,padding:'3px 8px',borderRadius:6,
              background:selected.status==='closing'?'#ff385c':selected.status==='closed'?'rgba(0,0,0,0.06)':'rgba(34,34,34,0.08)',
              color:selected.status==='closing'?'#fff':selected.status==='closed'?'#999':'#222'}}>
              {selected.statusLabel}
            </span>
          </div>

          <p style={{fontSize:12,color:'#6a6a6a',marginBottom:3}}>{selected.provider}</p>
          <p style={{fontSize:13,fontWeight:600,color:'#222',lineHeight:1.4,marginBottom:5,
            display:'-webkit-box',WebkitLineClamp:2,WebkitBoxOrient:'vertical',overflow:'hidden'}}>
            {selected.title}
          </p>
          <p style={{fontSize:13,color:'#222',fontWeight:500,marginBottom:3}}>{selected.price}</p>
          <p style={{fontSize:12,color:selected.urgent?'#ff385c':'#6a6a6a',fontWeight:selected.urgent?600:400,marginBottom:8}}>
            {selected.deadline}
          </p>
          <div style={{display:'flex',gap:4,flexWrap:'wrap',marginBottom:12}}>
            {selected.tags.map(t=>(
              <span key={t.l} style={{padding:'2px 8px',borderRadius:4,fontSize:11,fontWeight:600,...MAP_TAG_STYLES[t.c]}}>{t.l}</span>
            ))}
          </div>
          <button onClick={()=>onDetailClick&&onDetailClick(selected)}
            style={{width:'100%',height:40,borderRadius:10,background:'#ff385c',border:'none',cursor:'pointer',
              fontFamily:"'Noto Sans KR',sans-serif",fontSize:13,fontWeight:600,color:'#fff',transition:'background 0.2s'}}
            onMouseEnter={e=>e.currentTarget.style.background='#e00b41'}
            onMouseLeave={e=>e.currentTarget.style.background='#ff385c'}>
            상세보기 →
          </button>
        </div>
      )}
    </div>
  );
};

Object.assign(window, { MapView, MAP_LISTINGS });
