import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getAdminReviewDetail, submitReview } from '../../api/admin';
import type { AdminReviewDetail } from '../../api/admin';

const ACTIONS = [
  { value: 'APPROVE',  label: '승인',     color: '#166534', bg: '#f0fdf4' },
  { value: 'CORRECT',  label: '수정 승인', color: '#1d4ed8', bg: '#eff6ff' },
  { value: 'REJECT',   label: '거부',     color: '#ff385c', bg: '#fff0f3' },
  { value: 'REIMPORT', label: '재임포트',  color: '#7e22ce', bg: '#fdf4ff' },
];

export default function AdminReviewDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [detail, setDetail] = useState<AdminReviewDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [msg, setMsg] = useState('');

  useEffect(() => {
    if (!id) return;
    getAdminReviewDetail(Number(id))
      .then(setDetail)
      .catch(() => navigate('/admin/review'))
      .finally(() => setLoading(false));
  }, [id, navigate]);

  const handleAction = async (action: string) => {
    if (!id) return;
    setSubmitting(true);
    setMsg('');
    try {
      await submitReview(Number(id), action);
      setMsg(`${action} 처리 완료`);
      setTimeout(() => navigate('/admin/review'), 1200);
    } catch {
      setMsg('처리에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <p style={{ color: '#6a6a6a', fontSize: 14 }}>불러오는 중...</p>;
  if (!detail) return null;

  return (
    <div>
      <button
        style={{ display: 'flex', alignItems: 'center', gap: 6, background: 'none', border: 'none', cursor: 'pointer', color: '#6a6a6a', fontSize: 14, marginBottom: 20, padding: 0 }}
        onClick={() => navigate('/admin/review')}
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="14" height="14"><polyline points="15 18 9 12 15 6"/></svg>
        목록으로
      </button>

      <h1 style={{ fontSize: 20, fontWeight: 700, color: '#222', marginBottom: 4 }}>{detail.title}</h1>
      <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 24 }}>{detail.provider} · 상태: <strong>{detail.parseReviewStatus}</strong></p>

      {/* 액션 버튼 */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 32 }}>
        {ACTIONS.map((a) => (
          <button
            key={a.value}
            disabled={submitting}
            style={{ padding: '10px 20px', background: a.bg, border: `1px solid ${a.color}40`, color: a.color, borderRadius: 10, fontSize: 14, fontWeight: 700, cursor: 'pointer', opacity: submitting ? 0.6 : 1 }}
            onClick={() => handleAction(a.value)}
          >
            {a.label}
          </button>
        ))}
      </div>
      {msg && <p style={{ fontSize: 13, color: msg.includes('실패') ? '#ff385c' : '#166534', marginBottom: 16, fontWeight: 600 }}>{msg}</p>}

      {/* 2열 비교 */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {/* AI 파싱 결과 */}
        <div style={{ background: '#fff', borderRadius: 14, padding: '20px', boxShadow: 'rgba(0,0,0,0.04) 0px 2px 8px' }}>
          <p style={{ fontSize: 13, fontWeight: 700, color: '#222', marginBottom: 12, paddingBottom: 8, borderBottom: '1px solid rgba(0,0,0,0.06)' }}>AI 파싱 결과</p>
          {detail.parsedResult ? (
            <pre style={{ fontSize: 12, color: '#444', whiteSpace: 'pre-wrap', wordBreak: 'break-all', lineHeight: 1.7, margin: 0 }}>
              {JSON.stringify(detail.parsedResult, null, 2)}
            </pre>
          ) : (
            <p style={{ fontSize: 13, color: '#6a6a6a' }}>파싱 결과 없음</p>
          )}
        </div>

        {/* 원본 PDF 텍스트 */}
        <div style={{ background: '#fff', borderRadius: 14, padding: '20px', boxShadow: 'rgba(0,0,0,0.04) 0px 2px 8px' }}>
          <p style={{ fontSize: 13, fontWeight: 700, color: '#222', marginBottom: 12, paddingBottom: 8, borderBottom: '1px solid rgba(0,0,0,0.06)' }}>원본 PDF 텍스트</p>
          {detail.rawPdfText ? (
            <pre style={{ fontSize: 12, color: '#444', whiteSpace: 'pre-wrap', wordBreak: 'break-all', lineHeight: 1.7, margin: 0, maxHeight: 500, overflow: 'auto' }}>
              {detail.rawPdfText}
            </pre>
          ) : (
            <p style={{ fontSize: 13, color: '#6a6a6a' }}>원본 텍스트 없음</p>
          )}
        </div>
      </div>
    </div>
  );
}
