import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getAdminReviewList, deleteAnnouncement } from '../../api/admin';
import type { AdminReviewItem } from '../../api/admin';

const STATUSES = ['ALL', 'PENDING', 'APPROVED', 'CORRECTED', 'REJECTED', 'RE_IMPORT'];

const STATUS_COLORS: Record<string, { color: string; bg: string }> = {
  PENDING:   { color: '#f59e0b', bg: '#fffbeb' },
  APPROVED:  { color: '#166534', bg: '#f0fdf4' },
  CORRECTED: { color: '#1d4ed8', bg: '#eff6ff' },
  REJECTED:  { color: '#ff385c', bg: '#fff0f3' },
  RE_IMPORT: { color: '#7e22ce', bg: '#fdf4ff' },
};

export default function AdminReviewListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const status = searchParams.get('status') ?? 'ALL';

  const [items, setItems] = useState<AdminReviewItem[]>([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    getAdminReviewList(status === 'ALL' ? undefined : status)
      .then((data: { content?: AdminReviewItem[] } | AdminReviewItem[]) => {
        setItems(Array.isArray(data) ? data : (data as { content: AdminReviewItem[] }).content ?? []);
      })
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [status]);

  const handleDelete = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!confirm('이 공고를 삭제할까요?')) return;
    await deleteAnnouncement(id);
    load();
  };

  return (
    <div>
      <h1 style={{ fontSize: 22, fontWeight: 700, color: '#222', marginBottom: 24 }}>공고 리뷰</h1>

      {/* 탭 */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
        {STATUSES.map((s) => (
          <button
            key={s}
            style={{
              padding: '8px 16px', borderRadius: 8, border: 'none', cursor: 'pointer', fontSize: 13, fontWeight: 600,
              background: status === s ? '#222' : '#fff',
              color: status === s ? '#fff' : '#6a6a6a',
              boxShadow: 'rgba(0,0,0,0.06) 0px 1px 4px',
            }}
            onClick={() => setSearchParams(s === 'ALL' ? {} : { status: s })}
          >
            {s}
          </button>
        ))}
      </div>

      {loading ? (
        <p style={{ color: '#6a6a6a', fontSize: 14 }}>불러오는 중...</p>
      ) : items.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#6a6a6a' }}>
          <div style={{ fontSize: 36, marginBottom: 12 }}>📭</div>
          <p>해당 상태의 공고가 없습니다.</p>
        </div>
      ) : (
        <div style={{ background: '#fff', borderRadius: 16, overflow: 'hidden', boxShadow: 'rgba(0,0,0,0.04) 0px 2px 8px' }}>
          {items.map((item, i) => {
            const sc = STATUS_COLORS[item.parseReviewStatus] ?? { color: '#6a6a6a', bg: '#f2f2f2' };
            return (
              <div
                key={item.announcementId}
                style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '16px 24px', borderBottom: i < items.length - 1 ? '1px solid rgba(0,0,0,0.06)' : 'none', cursor: 'pointer', transition: 'background 0.1s' }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#f9f9f9')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                onClick={() => navigate(`/admin/review/${item.announcementId}`)}
              >
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: 14, fontWeight: 600, color: '#222', marginBottom: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.title}</p>
                  <p style={{ fontSize: 12, color: '#6a6a6a' }}>{item.provider} · {item.createdAt?.slice(0, 10)}</p>
                </div>
                <span style={{ padding: '4px 10px', borderRadius: 6, fontSize: 11, fontWeight: 700, background: sc.bg, color: sc.color, flexShrink: 0 }}>
                  {item.parseReviewStatus}
                </span>
                <button
                  style={{ padding: '6px 12px', background: 'transparent', border: '1px solid #ff385c', color: '#ff385c', borderRadius: 6, fontSize: 12, fontWeight: 600, cursor: 'pointer', flexShrink: 0 }}
                  onClick={(e) => handleDelete(item.announcementId, e)}
                >
                  삭제
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
