import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import AnnouncementCard from '../components/common/AnnouncementCard';
import Pagination from '../components/common/Pagination';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { useApi } from '../hooks/useApi';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../components/common/Toast';
import { formatPrice, calcDday, getDdayColor } from '../components/common/AnnouncementCard';
import StatusBadge from '../components/common/StatusBadge';

const FACTOR_STATUS_COLOR = {
  STRONG_MATCH: '#22C55E',
  PARTIAL_MATCH: '#F59E0B',
  NEEDS_VERIFICATION: '#F97316',
  NOT_MATCHED: '#EF4444',
  UNKNOWN: '#9ca3af',
};

const FACTOR_STATUS_LABEL = {
  STRONG_MATCH: '잘 맞음',
  PARTIAL_MATCH: '일부 일치',
  NEEDS_VERIFICATION: '확인 필요',
  NOT_MATCHED: '낮은 일치',
  UNKNOWN: '정보 부족',
};

const FACTOR_GROUP_ORDER = ['선호 조건', '비용', '일정', '신청 전 확인'];

const SUMMARY_STATUS_LABEL = {
  HIGHLY_RECOMMENDED: '내 조건과 잘 맞는 추천입니다',
  RECOMMENDED_WITH_CHECKS: '추천되지만 확인할 조건이 있습니다',
  LOW_EXPLANATION_CONFIDENCE: '추천 사유 설명이 제한적입니다',
  MIXED_MATCH: '일부 조건만 맞습니다',
};

const SCORE_COLOR = (score) => {
  if (score >= 80) return '#22C55E';
  if (score >= 50) return '#F59E0B';
  return '#EF4444';
};

function ReportPanel({ report }) {
  if (!report) return null;

  const summaryBgColor = {
    HIGHLY_RECOMMENDED: '#f0fdf4',
    RECOMMENDED_WITH_CHECKS: '#fff7ed',
    HAS_BLOCKERS: '#fff0f3',
    LOW_EXPLANATION_CONFIDENCE: '#fff0f3',
    MIXED_MATCH: '#eff6ff',
  }[report.summaryStatus] || '#f9fafb';

  const factors = Array.isArray(report.factors) ? report.factors : [];
  const hasFactor = factors.length > 0;

  const grouped = {};
  for (const g of FACTOR_GROUP_ORDER) grouped[g] = [];
  for (const f of factors) {
    const g = f.group || 'PREFERENCE';
    if (!grouped[g]) grouped[g] = [];
    grouped[g].push(f);
  }

  const visibleGroups = FACTOR_GROUP_ORDER.filter(g => grouped[g]?.length > 0);
  const [openGroups, setOpenGroups] = useState(() => new Set());

  const toggleGroup = (key) => {
    setOpenGroups(prev => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  };

  const counts = report.factorCounts || {};

  return (
    <div style={{ background: '#f9fafb', borderRadius: 16, padding: 16 }}>
      {/* 요약 배너 */}
      {report.summaryStatus && (
        <div style={{ background: summaryBgColor, borderRadius: 10, padding: '10px 14px', marginBottom: 12 }}>
          <p style={{ fontSize: 13, fontWeight: 700, color: '#222', margin: 0 }}>
            {SUMMARY_STATUS_LABEL[report.summaryStatus] || report.summaryStatus}
          </p>
          {report.summaryMessage && (
            <p style={{ fontSize: 12, color: '#6a6a6a', margin: '4px 0 0' }}>{report.summaryMessage}</p>
          )}
        </div>
      )}

      {/* factorCounts 요약 */}
      {(counts.strongMatch != null || counts.partialMatch != null || counts.needsVerification != null || counts.notMatched != null) && (
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 14 }}>
          {[
            { key: 'strongMatch', color: FACTOR_STATUS_COLOR.STRONG_MATCH, label: '잘 맞음' },
            { key: 'partialMatch', color: FACTOR_STATUS_COLOR.PARTIAL_MATCH, label: '일부 일치' },
            { key: 'needsVerification', color: FACTOR_STATUS_COLOR.NEEDS_VERIFICATION, label: '확인 필요' },
            { key: 'notMatched', color: FACTOR_STATUS_COLOR.NOT_MATCHED, label: '낮은 일치' },
          ].map(({ key, color, label }) =>
            counts[key] != null ? (
              <div key={key} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                <span style={{ width: 8, height: 8, borderRadius: '50%', background: color, display: 'inline-block' }} />
                <span style={{ fontSize: 12, color: '#6a6a6a' }}>{label} {counts[key]}</span>
              </div>
            ) : null
          )}
        </div>
      )}

      {/* factor 그룹별 아코디언 */}
      {hasFactor ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 8 }}>
          {visibleGroups.map(group => {
            const items = grouped[group];
            const isOpen = openGroups.has(group);
            const hasNotMatched = items.some(f => f.status === 'NOT_MATCHED');
            const hasVerification = !hasNotMatched && items.some(f => f.status === 'NEEDS_VERIFICATION');
            const dotColor = hasNotMatched ? FACTOR_STATUS_COLOR.NOT_MATCHED : hasVerification ? FACTOR_STATUS_COLOR.NEEDS_VERIFICATION : FACTOR_STATUS_COLOR.STRONG_MATCH;
            return (
              <div key={group} style={{ background: '#fff', borderRadius: 10, border: '1px solid rgba(0,0,0,0.07)', overflow: 'hidden' }}>
                <button
                  onClick={() => toggleGroup(group)}
                  style={{
                    width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    padding: '10px 14px', background: 'transparent', border: 'none', cursor: 'pointer',
                    borderBottom: isOpen ? '1px solid rgba(0,0,0,0.06)' : 'none',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ width: 6, height: 6, borderRadius: '50%', background: dotColor, flexShrink: 0 }} />
                    <span style={{ fontSize: 12, fontWeight: 700, color: '#222' }}>{group}</span>
                    <span style={{ fontSize: 11, color: '#9ca3af' }}>{items.length}개</span>
                  </div>
                  <span style={{ fontSize: 11, color: '#9ca3af', display: 'inline-block', transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }}>▼</span>
                </button>
                {isOpen && (
                  <div style={{ padding: '0 14px' }}>
                    {items.map((f, i) => (
                      <div key={i} style={{ borderBottom: i < items.length - 1 ? '1px solid rgba(0,0,0,0.05)' : 'none', padding: '10px 0', display: 'flex', flexDirection: 'column', gap: 4 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                          <span style={{ fontSize: 13, fontWeight: 600, color: '#222' }}>{f.label}</span>
                          <span style={{
                            display: 'inline-flex', alignItems: 'center',
                            borderRadius: 999, padding: '3px 8px',
                            fontSize: 11, fontWeight: 700, color: '#fff',
                            background: FACTOR_STATUS_COLOR[f.status] || FACTOR_STATUS_COLOR.UNKNOWN,
                          }}>
                            {FACTOR_STATUS_LABEL[f.status] || f.status}
                          </span>
                        </div>
                        {f.reason && <p style={{ fontSize: 12, color: '#6a6a6a', margin: 0 }}>{f.reason}</p>}
                        {f.actionLabel && (
                          f.actionTarget === 'OFFICIAL_NOTICE' && report.sourceNoticeUrl
                            ? <a href={report.sourceNoticeUrl} target="_blank" rel="noopener noreferrer"
                                style={{ fontSize: 12, color: '#ff385c', fontWeight: 600 }}
                                onClick={e => e.stopPropagation()}>
                                {f.actionLabel}
                              </a>
                            : <span style={{ fontSize: 12, color: '#ff385c', fontWeight: 600 }}>{f.actionLabel}</span>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : null}

      {/* disclaimer */}
      {report.disclaimer && (
        <p style={{ fontSize: 11, color: '#9ca3af', marginTop: 8, margin: '8px 0 0' }}>{report.disclaimer}</p>
      )}
    </div>
  );
}

export default function RecommendationsPage() {
  const [recommendations, setRecommendations] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [openReportId, setOpenReportId] = useState(null);
  const [openReasonsId, setOpenReasonsId] = useState(null);
  const [reportData, setReportData] = useState({});
  const [reportLoading, setReportLoading] = useState({});
  const [reportError, setReportError] = useState({});

  const api = useApi();
  const { profileCompleted } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const res = await api.get(`/api/recommendations?page=${page - 1}&size=20`);
        if (res.ok) {
          const data = await res.json();
          setRecommendations(data.content || []);
          setTotalPages(data.totalPages || 0);
          setTotalCount(data.totalElements || 0);
        }
      } catch { /* handled */ }
      setLoading(false);
    };
    load();
  }, [page]);

  if (!profileCompleted) {
    return (
      <EmptyState icon="📋" title="프로필을 먼저 작성해주세요"
        description="프로필을 작성하면 맞춤 공고를 추천받을 수 있습니다."
        actionLabel="프로필 설정하기" onAction={() => navigate('/profile/setup')} />
    );
  }

  if (loading) return <LoadingSpinner />;

  const handleToggleFavorite = async (id) => {
    try {
      const item = recommendations.find(r => (r.announcement?.announcementId || r.announcementId) === id);
      const a = item?.announcement || item;
      if (a?.favorited) {
        await api.del(`/api/me/favorites/${id}`);
        toast('즐겨찾기가 해제되었습니다');
      } else {
        await api.post('/api/me/favorites', { announcementId: id });
        toast('즐겨찾기에 추가되었습니다');
      }
      setRecommendations(prev => prev.map(r => {
        const aId = r.announcement?.announcementId || r.announcementId;
        if (aId !== id) return r;
        if (r.announcement) return { ...r, announcement: { ...r.announcement, favorited: !r.announcement.favorited } };
        return { ...r, favorited: !r.favorited };
      }));
    } catch { toast('오류가 발생했습니다', 'error'); }
  };

  const fetchReport = async (announcementId) => {
    if (reportData[announcementId]) {
      setOpenReportId(prev => prev === announcementId ? null : announcementId);
      return;
    }
    setReportLoading(prev => ({ ...prev, [announcementId]: true }));
    setOpenReportId(announcementId);
    setReportError(prev => { const n = { ...prev }; delete n[announcementId]; return n; });
    try {
      const res = await api.get(`/api/recommendations/${announcementId}/report`);
      if (res.ok) {
        const data = await res.json();
        setReportData(prev => ({ ...prev, [announcementId]: data }));
      } else {
        setReportError(prev => ({ ...prev, [announcementId]: true }));
      }
    } catch {
      setReportError(prev => ({ ...prev, [announcementId]: true }));
    }
    setReportLoading(prev => ({ ...prev, [announcementId]: false }));
  };

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '32px 24px 80px' }}>
      <h1 style={{ fontSize: 26, fontWeight: 700, color: '#222', marginBottom: 8 }}>맞춤공고 추천</h1>
      <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 24 }}>회원님의 프로필을 기반으로 추천된 공고입니다</p>
      <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 32 }}>
        총 <strong style={{ color: '#222' }}>{totalCount}</strong>건의 맞춤 공고
      </p>

      {recommendations.length === 0 ? (
        <EmptyState icon="🔍" title="현재 조건에 맞는 공고가 없습니다"
          description="프로필을 수정하면 더 많은 공고를 추천받을 수 있습니다."
          actionLabel="프로필 수정" onAction={() => navigate('/profile/setup')} />
      ) : (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {recommendations.map(rec => {
              const a = rec.announcement || rec;
              const score = rec.matchScore ?? 0;
              const maxScore = 135;
              const pct = Math.round((score / maxScore) * 100);
              const dday = calcDday(a.applicationEndDate);

              return (
                <div key={a.announcementId}
                  style={{
                    background: '#fff', borderRadius: 20, padding: 24,
                    boxShadow: 'var(--shadow-card)', cursor: 'pointer', transition: 'transform 0.2s',
                  }}
                  onClick={() => navigate(`/announcements/${a.announcementId}`)}
                  onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-2px)'}
                  onMouseLeave={e => e.currentTarget.style.transform = 'none'}
                >
                  {/* Score bar */}
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, flex: 1 }}>
                      <div>
                        <span style={{ fontSize: 14, fontWeight: 700, color: SCORE_COLOR(score) }}>추천 일치도 {score}점</span>
                        <span style={{ fontSize: 11, color: '#6a6a6a', display: 'block', marginTop: 2 }}>서비스 내부 기준의 프로필 일치도입니다</span>
                      </div>
                      <div style={{ flex: 1, height: 6, borderRadius: 3, background: 'rgba(0,0,0,0.08)', overflow: 'hidden' }}>
                        <div style={{ height: '100%', borderRadius: 3, background: SCORE_COLOR(score), width: `${pct}%`, transition: 'width 0.3s' }} />
                      </div>
                    </div>
                    <button
                      style={{ marginLeft: 12, background: 'transparent', border: 'none', cursor: 'pointer' }}
                      onClick={(e) => { e.stopPropagation(); handleToggleFavorite(a.announcementId); }}
                    >
                      <svg viewBox="0 0 24 24" fill={a.favorited ? '#ff385c' : 'none'} stroke={a.favorited ? '#ff385c' : '#c1c1c1'} strokeWidth="2" width="22" height="22">
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                      </svg>
                    </button>
                  </div>

                  <h3 style={{ fontSize: 16, fontWeight: 700, color: '#222', marginBottom: 8, lineHeight: 1.4 }}>{a.noticeName}</h3>
                  <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 8 }}>
                    {a.providerName}{a.regionLevel1 ? ` | ${a.regionLevel1}${a.regionLevel2 ? ' ' + a.regionLevel2 : ''}` : ''}
                  </p>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 8 }}>
                    {a.depositAmount != null && <span style={{ fontSize: 14, fontWeight: 500 }}>보증금: {formatPrice(a.depositAmount)}만원</span>}
                    {a.monthlyRentAmount != null && <span style={{ fontSize: 14, fontWeight: 500 }}>월세: {formatPrice(a.monthlyRentAmount)}만원</span>}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    {a.applicationStartDate && (
                      <span style={{ fontSize: 12, color: '#6a6a6a' }}>신청: {a.applicationStartDate} ~ {a.applicationEndDate}</span>
                    )}
                    <StatusBadge status={a.noticeStatus} />
                  </div>

                  {/* Match reasons */}
                  {rec.matchReasons?.length > 0 && (
                    <div style={{ marginTop: 16, borderRadius: 12, border: '1px solid rgba(0,0,0,0.07)', overflow: 'hidden' }}>
                      <button
                        onClick={(e) => { e.stopPropagation(); setOpenReasonsId(openReasonsId === a.announcementId ? null : a.announcementId); }}
                        style={{
                          width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                          padding: '10px 16px', background: '#f9fafb', border: 'none', cursor: 'pointer',
                        }}
                      >
                        <span style={{ fontSize: 12, fontWeight: 600, color: '#6a6a6a' }}>추천 이유 ({rec.matchReasons.length}개)</span>
                        <span style={{ fontSize: 11, color: '#9ca3af', display: 'inline-block', transform: openReasonsId === a.announcementId ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }}>▼</span>
                      </button>
                      {openReasonsId === a.announcementId && (
                        <div style={{ padding: '10px 16px', background: '#fff' }}>
                          {rec.matchReasons.map((r, i) => (
                            <p key={i} style={{ fontSize: 13, color: '#222', marginBottom: 4, display: 'flex', alignItems: 'center', gap: 6 }}>
                              <span style={{ color: '#22C55E' }}>&#10003;</span> {r}
                            </p>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* 추천 근거 자세히 보기 버튼 */}
                  <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end' }}>
                    <button
                      onClick={(e) => { e.stopPropagation(); fetchReport(a.announcementId); }}
                      style={{
                        fontSize: 13, color: '#ff385c', background: 'none', border: 'none',
                        cursor: 'pointer', fontWeight: 600, padding: '4px 0',
                      }}
                    >
                      {openReportId === a.announcementId ? '추천 근거 닫기 \u25b2' : '추천 근거 자세히 보기 \u25bc'}
                    </button>
                  </div>

                  {/* 리포트 확장 영역 */}
                  {openReportId === a.announcementId && (
                    <div style={{ marginTop: 12, borderTop: '1px solid rgba(0,0,0,0.06)', paddingTop: 16 }}
                      onClick={(e) => e.stopPropagation()}
                    >
                      {reportLoading[a.announcementId] ? (
                        <p style={{ fontSize: 13, color: '#6a6a6a', textAlign: 'center', padding: '12px 0' }}>추천 근거를 불러오는 중...</p>
                      ) : reportError[a.announcementId] ? (
                        <p style={{ fontSize: 13, color: '#EF4444', textAlign: 'center', padding: '12px 0' }}>추천 근거를 불러오지 못했습니다.</p>
                      ) : reportData[a.announcementId] ? (
                        <ReportPanel report={reportData[a.announcementId]} />
                      ) : null}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
