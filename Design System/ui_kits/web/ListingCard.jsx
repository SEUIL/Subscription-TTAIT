// ListingCard.jsx — housing announcement card
const LISTING_DATA = [
  { id:1, emoji:'🏢', provider:'LH · 경기도 오산시', title:'오산세교2 21단지 국민임대주택 예비입주자 모집공고', price:'보증금 1,119만원 / 월세 149,570원', deadline:'마감 D-6 · 4월 14일', urgent:true, status:'open', statusLabel:'모집중', tags:[{l:'청년',c:'blue'},{l:'국민임대',c:'red'}], saved:false },
  { id:2, emoji:'🏗️', provider:'LH · 인천광역시', title:'26년 2차 신혼·신생아Ⅰ 예비입주자 모집공고 (인천,부천)', price:'보증금 8,500만원 / 월세 320,000원', deadline:'마감 D-1 · 오늘!', urgent:true, status:'closing', statusLabel:'마감임박', tags:[{l:'신혼부부',c:'purple'},{l:'행복주택',c:'red'}], saved:true },
  { id:3, emoji:'🏠', provider:'LH · 서울특별시', title:'서울 강서구 마곡지구 영구임대 입주자 모집공고', price:'보증금 2,340만원 / 월세 98,000원', deadline:'마감 D-15 · 4월 23일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'영구임대',c:'red'},{l:'저소득',c:'green'}], saved:false },
  { id:4, emoji:'🏘️', provider:'LH · 경기도 수원시', title:'수원 광교신도시 공공분양 (일반공급) 입주자 모집', price:'분양가 3억 2,000만원', deadline:'마감 D-2 · 4월 10일', urgent:true, status:'closing', statusLabel:'마감임박', tags:[{l:'공공분양',c:'red'}], saved:false },
  { id:5, emoji:'🏙️', provider:'LH · 인천광역시 부평구', title:'부평구 청천동 행복주택 입주자 모집공고', price:'보증금 5,200만원 / 월세 230,000원', deadline:'마감 D-20 · 4월 28일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'청년',c:'blue'},{l:'행복주택',c:'red'}], saved:false },
  { id:6, emoji:'🏛️', provider:'LH · 서울특별시 노원구', title:'노원구 상계동 국민임대주택 입주자 모집공고', price:'보증금 3,800만원 / 월세 175,000원', deadline:'모집 종료', urgent:false, status:'closed', statusLabel:'마감', tags:[{l:'국민임대',c:'neutral'}], saved:false },
  { id:7, emoji:'🏢', provider:'LH · 경기도 화성시', title:'화성 동탄2 매입임대주택 입주자 모집공고', price:'보증금 1,800만원 / 월세 210,000원', deadline:'마감 D-30 · 5월 8일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'매입임대',c:'red'},{l:'고령자',c:'orange'}], saved:false },
  { id:8, emoji:'🏗️', provider:'LH · 경기도 평택시', title:'평택 고덕신도시 국민임대주택 예비입주자 모집공고', price:'보증금 9,240만원 / 월세 188,000원', deadline:'마감 D-12 · 4월 20일', urgent:false, status:'open', statusLabel:'모집중', tags:[{l:'신혼부부',c:'purple'},{l:'청년',c:'blue'}], saved:false },
];

const TAG_STYLES = {
  red:    { background:'#fff0f3', color:'#ff385c', border:'1px solid rgba(255,56,92,0.2)' },
  blue:   { background:'#eff6ff', color:'#1d4ed8', border:'1px solid rgba(29,78,216,0.2)' },
  purple: { background:'#fdf4ff', color:'#7e22ce', border:'1px solid rgba(126,34,206,0.2)' },
  green:  { background:'#f0fdf4', color:'#166534', border:'1px solid rgba(22,101,52,0.2)' },
  orange: { background:'#fff7ed', color:'#c2410c', border:'1px solid rgba(194,65,12,0.2)' },
  neutral:{ background:'#f2f2f2', color:'#222', border:'1px solid transparent' },
};

const STATUS_STYLES = {
  open:    { background:'rgba(34,34,34,0.75)',  color:'#fff' },
  closing: { background:'rgba(255,56,92,0.9)',  color:'#fff' },
  closed:  { background:'rgba(106,106,106,0.75)', color:'#fff' },
};

const ListingCard = ({ item, onClick }) => {
  const [saved, setSaved] = React.useState(item.saved);
  const [hov, setHov] = React.useState(false);

  const cardStyle = {
    cursor:'pointer', borderRadius:20, overflow:'hidden', background:'#fff',
    transition:'transform 0.2s, box-shadow 0.2s', textDecoration:'none', display:'block',
    transform: hov ? 'translateY(-2px)' : 'none',
    boxShadow: hov ? 'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px, rgba(0,0,0,0.1) 0px 4px 8px' : 'none',
  };

  return (
    <div style={cardStyle} onMouseEnter={()=>setHov(true)} onMouseLeave={()=>setHov(false)} onClick={()=>onClick&&onClick(item)}>
      {/* Image */}
      <div style={{position:'relative',width:'100%',paddingTop:'66.67%',background:'#f2f2f2',borderRadius:16,overflow:'hidden'}}>
        <div style={{position:'absolute',inset:0,display:'flex',alignItems:'center',justifyContent:'center',fontSize:44,
          transform:hov?'scale(1.03)':'scale(1)',transition:'transform 0.3s', opacity: item.status==='closed'?0.5:1}}>
          {item.emoji}
        </div>
        <span style={{position:'absolute',top:12,left:12,padding:'4px 10px',borderRadius:6,fontSize:11,fontWeight:600,...STATUS_STYLES[item.status]}}>
          {item.statusLabel}
        </span>
        <button style={{position:'absolute',top:10,right:10,width:32,height:32,borderRadius:'50%',background:'transparent',border:'none',cursor:'pointer',display:'flex',alignItems:'center',justifyContent:'center'}}
          onClick={e=>{e.stopPropagation();setSaved(s=>!s)}}>
          <svg viewBox="0 0 24 24" fill={saved?'rgba(255,56,92,0.9)':'none'} stroke="white" strokeWidth="2" width="22" height="22"
            style={{filter:'drop-shadow(0 1px 2px rgba(0,0,0,0.5))'}}>
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
        </button>
      </div>

      {/* Info */}
      <div style={{padding:'10px 4px 0'}}>
        <p style={{fontSize:13,color:'#6a6a6a',fontWeight:400,marginBottom:3}}>{item.provider}</p>
        <h3 style={{fontSize:14,fontWeight:600,color:'#222',lineHeight:1.4,letterSpacing:'-0.2px',
          display:'-webkit-box',WebkitLineClamp:2,WebkitBoxOrient:'vertical',overflow:'hidden',marginBottom:4}}>
          {item.title}
        </h3>
        <p style={{fontSize:14,color:'#222',fontWeight:500,marginBottom:3}}>{item.price}</p>
        <p style={{fontSize:12,color:item.urgent?'#ff385c':'#6a6a6a',fontWeight:item.urgent?600:400}}>{item.deadline}</p>
        <div style={{display:'flex',flexWrap:'wrap',gap:4,marginTop:6}}>
          {item.tags.map(t=>(
            <span key={t.l} style={{padding:'3px 8px',borderRadius:4,fontSize:11,fontWeight:600,...TAG_STYLES[t.c]}}>{t.l}</span>
          ))}
        </div>
      </div>
    </div>
  );
};

Object.assign(window, { ListingCard, LISTING_DATA, TAG_STYLES, STATUS_STYLES });
