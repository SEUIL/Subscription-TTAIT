import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

interface Props {
  lat: number;
  lng: number;
  label: string;
  address: string;
}

export default function MiniMap({ lat, lng, label, address }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);

  useEffect(() => {
    if (!ref.current || mapRef.current) return;

    const map = L.map(ref.current, {
      center: [lat, lng], zoom: 15,
      zoomControl: false, scrollWheelZoom: false, dragging: false,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap', maxZoom: 18,
    }).addTo(map);

    const icon = L.divIcon({
      className: '',
      html: `<div style="width:32px;height:32px;border-radius:50% 50% 50% 0;background:#ff385c;border:2.5px solid #fff;box-shadow:0 2px 8px rgba(0,0,0,0.3);transform:rotate(-45deg);display:flex;align-items:center;justify-content:center;"><span style="transform:rotate(45deg);font-size:13px">🏢</span></div>`,
      iconSize: [32, 32], iconAnchor: [16, 32],
    });

    L.marker([lat, lng], { icon }).addTo(map);
    L.circle([lat, lng], { color: '#ff385c', fillColor: '#ff385c', fillOpacity: 0.08, weight: 1.5, radius: 300 }).addTo(map);

    mapRef.current = map;
    return () => { map.remove(); mapRef.current = null; };
  }, [lat, lng]);

  return (
    <div style={{ borderRadius: 16, overflow: 'hidden', border: '1px solid rgba(0,0,0,0.08)', position: 'relative' }}>
      <div ref={ref} style={{ width: '100%', height: 240 }} />
      <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, background: 'linear-gradient(to top,rgba(0,0,0,0.55) 0%,transparent 100%)', padding: '32px 16px 14px', pointerEvents: 'none' }}>
        <p style={{ fontSize: 13, fontWeight: 700, color: '#fff', marginBottom: 2 }}>{label}</p>
        <p style={{ fontSize: 12, color: 'rgba(255,255,255,0.85)', display: 'flex', alignItems: 'center', gap: 5 }}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="12" height="12"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
          {address}
        </p>
      </div>
      <a
        href={`https://map.naver.com/v5/search/${encodeURIComponent(address)}`}
        target="_blank" rel="noreferrer"
        style={{ position: 'absolute', top: 10, right: 10, background: '#fff', borderRadius: 8, padding: '6px 10px', fontSize: 11, fontWeight: 600, color: '#222', cursor: 'pointer', boxShadow: 'rgba(0,0,0,0.12) 0px 2px 8px', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: 4 }}
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="11" height="11"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
        네이버 지도에서 보기
      </a>
    </div>
  );
}
