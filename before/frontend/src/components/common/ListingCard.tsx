import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Announcement } from '../../types';

const TAG_STYLES: Record<string, React.CSSProperties> = {
  red:     { background: '#fff0f3', color: '#ff385c', border: '1px solid rgba(255,56,92,0.2)' },
  blue:    { background: '#eff6ff', color: '#1d4ed8', border: '1px solid rgba(29,78,216,0.2)' },
  purple:  { background: '#fdf4ff', color: '#7e22ce', border: '1px solid rgba(126,34,206,0.2)' },
  green:   { background: '#f0fdf4', color: '#166534', border: '1px solid rgba(22,101,52,0.2)' },
  orange:  { background: '#fff7ed', color: '#c2410c', border: '1px solid rgba(194,65,12,0.2)' },
  neutral: { background: '#f2f2f2', color: '#222',    border: '1px solid transparent' },
};

const STATUS_STYLES: Record<string, React.CSSProperties> = {
  ACTIVE:   { background: 'rgba(34,34,34,0.75)',   color: '#fff' },
  CLOSING:  { background: 'rgba(255,56,92,0.9)',   color: '#fff' },
  CLOSED:   { background: 'rgba(106,106,106,0.75)', color: '#fff' },
  UPCOMING: { background: 'rgba(29,78,216,0.75)',  color: '#fff' },
};

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: '모집중', CLOSING: '마감임박', CLOSED: '마감', UPCOMING: '예정',
};

function getDDay(endDate: string): { label: string; urgent: boolean } {
  if (!endDate) return { label: '', urgent: false };
  const diff = Math.ceil((new Date(endDate).getTime() - Date.now()) / 86400000);
  if (diff < 0) return { label: '모집 종료', urgent: false };
  if (diff === 0) return { label: '마감 D-0 · 오늘!', urgent: true };
  if (diff <= 3) return { label: `마감 D-${diff} · ${new Date(endDate).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })}`, urgent: true };
  return { label: `마감 D-${diff} · ${new Date(endDate).toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })}`, urgent: false };
}

function getCategoryColor(category: string): string {
  const map: Record<string, string> = {
    '청년': 'blue', '신혼부부': 'purple', '저소득': 'green', '저소득층': 'green',
    '고령자': 'orange', '다자녀': 'orange', '무주택자': 'neutral',
    '국민임대': 'red', '행복주택': 'red', '영구임대': 'red',
    '매입임대': 'red', '공공분양': 'red', '기타': 'neutral',
  };
  return map[category] ?? 'neutral';
}

const EMOJIS = ['🏢', '🏗️', '🏠', '🏘️', '🏙️', '🏛️'];

interface Props {
  item: Announcement;
}

export default function ListingCard({ item }: Props) {
  const navigate = useNavigate();
  const [saved, setSaved] = useState(false);
  const [hov, setHov] = useState(false);

  const { label: deadlineLabel, urgent } = getDDay(item.recruitmentEndDate);
  const emoji = EMOJIS[item.announcementId % EMOJIS.length];

  return (
    <div
      style={{
        cursor: 'pointer', borderRadius: 20, overflow: 'hidden', background: '#fff',
        transition: 'transform 0.2s, box-shadow 0.2s',
        transform: hov ? 'translateY(-2px)' : 'none',
        boxShadow: hov ? 'rgba(0,0,0,0.02) 0px 0px 0px 1px, rgba(0,0,0,0.04) 0px 2px 6px, rgba(0,0,0,0.1) 0px 4px 8px' : 'none',
      }}
      onMouseEnter={() => setHov(true)}
      onMouseLeave={() => setHov(false)}
      onClick={() => navigate(`/announcements/${item.announcementId}`)}
    >
      {/* Image */}
      <div style={{ position: 'relative', width: '100%', paddingTop: '66.67%', background: '#f2f2f2', borderRadius: 16, overflow: 'hidden' }}>
        {item.imageUrl ? (
          <img src={item.imageUrl} alt={item.title} style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', transform: hov ? 'scale(1.03)' : 'scale(1)', transition: 'transform 0.3s' }} />
        ) : (
          <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 44, transform: hov ? 'scale(1.03)' : 'scale(1)', transition: 'transform 0.3s', opacity: item.status === 'CLOSED' ? 0.5 : 1 }}>
            {emoji}
          </div>
        )}
        <span style={{ position: 'absolute', top: 12, left: 12, padding: '4px 10px', borderRadius: 6, fontSize: 11, fontWeight: 600, ...(STATUS_STYLES[item.status] ?? STATUS_STYLES.ACTIVE) }}>
          {STATUS_LABELS[item.status] ?? item.status}
        </span>
        <button
          style={{ position: 'absolute', top: 10, right: 10, width: 32, height: 32, borderRadius: '50%', background: 'transparent', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          onClick={(e) => { e.stopPropagation(); setSaved((s) => !s); }}
        >
          <svg viewBox="0 0 24 24" fill={saved ? 'rgba(255,56,92,0.9)' : 'none'} stroke="white" strokeWidth="2" width="22" height="22" style={{ filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.5))' }}>
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
          </svg>
        </button>
      </div>

      {/* Info */}
      <div style={{ padding: '10px 4px 0' }}>
        <p style={{ fontSize: 13, color: '#6a6a6a', fontWeight: 400, marginBottom: 3 }}>{item.provider} · {item.regionLevel1} {item.regionLevel2}</p>
        <h3 style={{ fontSize: 14, fontWeight: 600, color: '#222', lineHeight: 1.4, letterSpacing: '-0.2px', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden', marginBottom: 4, margin: '0 0 4px' }}>
          {item.title}
        </h3>
        {(item.deposit != null || item.monthlyRent != null) && (
          <p style={{ fontSize: 14, color: '#222', fontWeight: 500, marginBottom: 3 }}>
            {item.deposit != null ? `보증금 ${item.deposit.toLocaleString()}원` : ''}
            {item.deposit != null && item.monthlyRent != null ? ' / ' : ''}
            {item.monthlyRent != null ? `월세 ${item.monthlyRent.toLocaleString()}원` : ''}
          </p>
        )}
        <p style={{ fontSize: 12, color: urgent ? '#ff385c' : '#6a6a6a', fontWeight: urgent ? 600 : 400 }}>{deadlineLabel}</p>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4, marginTop: 6 }}>
          {item.categories.map((cat) => (
            <span key={cat} style={{ padding: '3px 8px', borderRadius: 4, fontSize: 11, fontWeight: 600, ...TAG_STYLES[getCategoryColor(cat)] }}>
              {cat}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}
