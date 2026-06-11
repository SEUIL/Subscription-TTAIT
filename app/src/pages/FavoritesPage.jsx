import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Pagination from '../components/common/Pagination';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import StatusBadge from '../components/common/StatusBadge';
import { useApi } from '../hooks/useApi';
import { useToast } from '../components/common/Toast';
import { formatPrice, calcDday, getDdayColor } from '../components/common/AnnouncementCard';

const SCHEDULE_STATUS_COLOR = {
  DUE_TODAY: '#EF4444',
  DUE_TOMORROW: '#F97316',
  DUE_SOON: '#F59E0B',
  OPEN: '#22C55E',
  UPCOMING: '#3B82F6',
  DATE_UNKNOWN: '#9ca3af',
  CLOSED: '#c1c1c1',
};

const SCHEDULE_STATUS_LABEL = {
  DUE_TODAY: '오늘 마감',
  DUE_TOMORROW: '내일 마감',
  DUE_SOON: '마감 임박',
  OPEN: '접수 중',
  UPCOMING: '접수 예정',
  DATE_UNKNOWN: '일정 확인 필요',
  CLOSED: '마감됨',
};

function ScheduleView({ schedule, loading }) {
  if (loading) {
    return <div style={{ padding: 40, textAlign: 'center', color: '#6a6a6a', fontSize: 14 }}>일정을 불러오는 중...</div>;
  }
  if (!schedule || !Array.isArray(schedule.groups) || schedule.groups.length === 0) {
    return (
      <div style={{ padding: 40, textAlign: 'center' }}>
        <p style={{ fontSize: 15, color: '#6a6a6a' }}>즐겨찾기한 공고가 없습니다.</p>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {schedule.groups.filter(g => g.items.length > 0).map(group => (
        <div key={group.key}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
            <span style={{
              width: 10, height: 10, borderRadius: '50%',
              background: SCHEDULE_STATUS_COLOR[group.key] || '#9ca3af', flexShrink: 0,
            }} />
            <h3 style={{ fontSize: 15, fontWeight: 700, color: '#222' }}>{group.label}</h3>
            <span style={{ fontSize: 13, color: '#6a6a6a' }}>{group.items.length}개</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {group.items.map(item => (
              <div key={item.announcementId} style={{
                background: '#fff', borderRadius: 16, padding: '16px 20px',
                boxShadow: 'var(--shadow-card)',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap',
              }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: 15, fontWeight: 700, color: '#222', marginBottom: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {item.noticeName}
                  </p>
                  <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 4 }}>{item.providerName}</p>
                  {item.applicationStartDate && (
                    <p style={{ fontSize: 12, color: '#6a6a6a' }}>
                      신청: {item.applicationStartDate} ~ {item.applicationEndDate || '미정'}
                    </p>
                  )}
                  {item.statusMessage && (
                    <p style={{ fontSize: 12, color: SCHEDULE_STATUS_COLOR[item.scheduleStatus] || '#6a6a6a', marginTop: 4, fontWeight: 600 }}>
                      {item.statusMessage}
                    </p>
                  )}
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 6, flexShrink: 0 }}>
                  {item.dDayLabel && (
                    <span style={{
                      fontSize: 16, fontWeight: 800,
                      color: SCHEDULE_STATUS_COLOR[item.scheduleStatus] || '#6a6a6a',
                    }}>
                      {item.dDayLabel}
                    </span>
                  )}
                  <span style={{
                    fontSize: 11, fontWeight: 700, padding: '3px 8px', borderRadius: 999,
                    background: (SCHEDULE_STATUS_COLOR[item.scheduleStatus] || '#9ca3af') + '20',
                    color: SCHEDULE_STATUS_COLOR[item.scheduleStatus] || '#9ca3af',
                  }}>
                    {SCHEDULE_STATUS_LABEL[item.scheduleStatus] || item.scheduleStatus}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
      {schedule.disclaimer && (
        <p style={{ fontSize: 12, color: '#9ca3af', textAlign: 'center', marginTop: 8, lineHeight: 1.6 }}>
          {schedule.disclaimer}
        </p>
      )}
    </div>
  );
}

function CalendarView({ schedule, loading }) {
  const [currentDate, setCurrentDate] = useState(new Date());

  if (loading) {
    return <div style={{ padding: 40, textAlign: 'center', color: '#6a6a6a', fontSize: 14 }}>캘린더를 불러오는 중...</div>;
  }
  if (!schedule) {
    return <div style={{ padding: 40, textAlign: 'center', color: '#6a6a6a', fontSize: 14 }}>즐겨찾기한 공고가 없습니다.</div>;
  }

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const eventsByDate = {};
  (schedule.calendarEvents || []).forEach(ev => {
    const d = ev.date;
    if (!eventsByDate[d]) eventsByDate[d] = [];
    eventsByDate[d].push(ev);
  });

  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startDow = firstDay.getDay();
  const daysInMonth = lastDay.getDate();

  const cells = [];
  for (let i = 0; i < startDow; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);
  while (cells.length % 7 !== 0) cells.push(null);

  const todayStr = new Date().toISOString().slice(0, 10);
  const pad = (n) => String(n).padStart(2, '0');

  const EVENT_TYPE_LABEL = {
    APPLICATION_START: '신청 시작',
    APPLICATION_END: '신청 마감',
    WINNER_ANNOUNCEMENT: '당첨 발표',
    ANNOUNCEMENT_DATE: '공고일',
  };
  const EVENT_TYPE_COLOR = {
    APPLICATION_START: '#3B82F6',
    APPLICATION_END: '#EF4444',
    WINNER_ANNOUNCEMENT: '#22C55E',
    ANNOUNCEMENT_DATE: '#F59E0B',
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <button onClick={() => setCurrentDate(new Date(year, month - 1, 1))}
          style={{ padding: '8px 14px', borderRadius: 8, border: '1px solid #c1c1c1', background: '#fff', cursor: 'pointer', fontSize: 14 }}>
          &lt;
        </button>
        <h3 style={{ fontSize: 18, fontWeight: 700, color: '#222' }}>{year}년 {month + 1}월</h3>
        <button onClick={() => setCurrentDate(new Date(year, month + 1, 1))}
          style={{ padding: '8px 14px', borderRadius: 8, border: '1px solid #c1c1c1', background: '#fff', cursor: 'pointer', fontSize: 14 }}>
          &gt;
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2, marginBottom: 4 }}>
        {['일','월','화','수','목','금','토'].map((d, i) => (
          <div key={d} style={{ textAlign: 'center', fontSize: 12, fontWeight: 700, color: i === 0 ? '#EF4444' : i === 6 ? '#3B82F6' : '#6a6a6a', padding: '6px 0' }}>
            {d}
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2 }}>
        {cells.map((day, idx) => {
          if (!day) return <div key={`empty-${idx}`} />;
          const dateStr = `${year}-${pad(month + 1)}-${pad(day)}`;
          const events = eventsByDate[dateStr] || [];
          const isToday = dateStr === todayStr;
          const dow = (startDow + day - 1) % 7;

          return (
            <div key={dateStr} style={{
              minHeight: 64, padding: '6px 4px', borderRadius: 10,
              background: isToday ? '#fff0f3' : '#fff',
              border: isToday ? '1px solid #ff385c' : '1px solid rgba(0,0,0,0.06)',
            }}>
              <p style={{
                fontSize: 13, fontWeight: isToday ? 800 : 400,
                color: isToday ? '#ff385c' : dow === 0 ? '#EF4444' : dow === 6 ? '#3B82F6' : '#222',
                marginBottom: 4, textAlign: 'right', paddingRight: 2,
              }}>
                {day}
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {events.slice(0, 2).map((ev, i) => (
                  <div key={i} style={{
                    fontSize: 10, fontWeight: 700, padding: '2px 4px', borderRadius: 4,
                    background: (EVENT_TYPE_COLOR[ev.eventType] || '#9ca3af') + '20',
                    color: EVENT_TYPE_COLOR[ev.eventType] || '#9ca3af',
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                  }}>
                    {EVENT_TYPE_LABEL[ev.eventType] || ev.eventType}
                  </div>
                ))}
                {events.length > 2 && (
                  <div style={{ fontSize: 10, color: '#6a6a6a', paddingLeft: 2 }}>+{events.length - 2}</div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 16, padding: '12px 16px', background: '#f9fafb', borderRadius: 12 }}>
        {Object.entries(EVENT_TYPE_LABEL).map(([type, label]) => (
          <div key={type} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ width: 8, height: 8, borderRadius: '50%', background: EVENT_TYPE_COLOR[type], flexShrink: 0 }} />
            <span style={{ fontSize: 12, color: '#6a6a6a' }}>{label}</span>
          </div>
        ))}
      </div>

      {schedule.disclaimer && (
        <p style={{ fontSize: 12, color: '#9ca3af', textAlign: 'center', marginTop: 12, lineHeight: 1.6 }}>
          {schedule.disclaimer}
        </p>
      )}
    </div>
  );
}

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [confirmId, setConfirmId] = useState(null);
  const [view, setView] = useState('list');
  const [schedule, setSchedule] = useState(null);
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [scheduleLoaded, setScheduleLoaded] = useState(false);

  const api = useApi();
  const navigate = useNavigate();
  const toast = useToast();

  const load = async () => {
    setLoading(true);
    try {
      const res = await api.get(`/api/me/favorites?page=${page - 1}&size=20`);
      if (res.ok) {
        const data = await res.json();
        setFavorites(data.content || []);
        setTotalPages(data.totalPages || 0);
        setTotalCount(data.totalElements || 0);
      }
    } catch { /* handled */ }
    setLoading(false);
  };

  useEffect(() => { load(); }, [page]);

  const removeFavorite = async (id) => {
    try {
      await api.del(`/api/me/favorites/${id}`);
      toast('즐겨찾기가 해제되었습니다');
      setConfirmId(null);
      setFavorites(prev => prev.filter(f => (f.announcement?.announcementId || f.announcementId) !== id));
      setTotalCount(prev => prev - 1);
    } catch { toast('오류가 발생했습니다', 'error'); }
  };

  const loadSchedule = async () => {
    if (scheduleLoaded) return;
    setScheduleLoading(true);
    try {
      const res = await api.get('/api/me/favorites/schedule');
      if (res.ok) setSchedule(await res.json());
    } catch { /* handled */ }
    setScheduleLoading(false);
    setScheduleLoaded(true);
  };

  useEffect(() => {
    if ((view === 'schedule' || view === 'calendar') && !scheduleLoaded) {
      loadSchedule();
    }
  }, [view, scheduleLoaded]);

  if (loading) return <LoadingSpinner />;

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '32px 24px 80px' }}>
      <h1 style={{ fontSize: 26, fontWeight: 700, color: '#222', marginBottom: 8 }}>즐겨찾기</h1>
      <p style={{ fontSize: 14, color: '#6a6a6a', marginBottom: 24 }}>
        총 <strong style={{ color: '#222' }}>{totalCount}</strong>건
      </p>

      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {[
          { key: 'list', label: '목록' },
          { key: 'schedule', label: '일정 관리' },
          { key: 'calendar', label: '캘린더' },
        ].map(({ key, label }) => (
          <button key={key} onClick={() => setView(key)}
            style={{
              padding: '8px 18px', borderRadius: 20, border: 'none', cursor: 'pointer',
              background: view === key ? '#ff385c' : '#f2f2f2',
              color: view === key ? '#fff' : '#222', fontWeight: 600, fontSize: 13,
              transition: 'background 0.15s',
            }}>
            {label}
          </button>
        ))}
      </div>

      {(view === 'schedule' || view === 'calendar') && schedule && (
        <div style={{
          padding: 16, borderRadius: 16, background: '#fff8f9', border: '1px solid #ffd0d9',
          marginBottom: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8,
        }}>
          <div>
            <p style={{ fontSize: 14, fontWeight: 700, color: '#222', marginBottom: 4 }}>내 관심 공고 일정</p>
            <p style={{ fontSize: 13, color: '#6a6a6a' }}>
              오늘 마감 <strong style={{ color: '#EF4444' }}>{schedule.summary?.dueTodayCount ?? 0}</strong>개 &middot;&nbsp;
              7일 이내 <strong style={{ color: '#F59E0B' }}>{schedule.summary?.dueSoonCount ?? 0}</strong>개 &middot;&nbsp;
              접수 중 <strong style={{ color: '#22C55E' }}>{schedule.summary?.openCount ?? 0}</strong>개
            </p>
          </div>
          <span style={{ fontSize: 12, color: '#6a6a6a' }}>총 {schedule.summary?.totalCount ?? 0}개</span>
        </div>
      )}

      {favorites.length === 0 && view === 'list' ? (
        <EmptyState icon="💛" title="즐겨찾기한 공고가 없습니다"
          description="관심 있는 공고를 즐겨찾기하면 마감일 알림을 받을 수 있습니다."
          actionLabel="공고 검색하기" onAction={() => navigate('/announcements')} />
      ) : view === 'list' ? (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {favorites.map(fav => {
              const a = fav.announcement || fav;
              const aid = a.announcementId;
              const dday = calcDday(a.applicationEndDate);
              const ddayColor = getDdayColor(a.applicationEndDate);

              return (
                <div key={aid} style={{
                  background: '#fff', borderRadius: 20, padding: 24, boxShadow: 'var(--shadow-card)',
                  transition: 'transform 0.2s',
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ flex: 1, cursor: 'pointer' }} onClick={() => navigate(`/announcements/${aid}`)}>
                      <h3 style={{ fontSize: 16, fontWeight: 700, color: '#222', marginBottom: 8, lineHeight: 1.4 }}>{a.noticeName}</h3>
                      <p style={{ fontSize: 13, color: '#6a6a6a', marginBottom: 8 }}>
                        {a.providerName}{a.regionLevel1 ? ` | ${a.regionLevel1}${a.regionLevel2 ? ' ' + a.regionLevel2 : ''}` : ''}
                      </p>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 8 }}>
                        {a.depositAmount != null && <span style={{ fontSize: 14, fontWeight: 500 }}>보증금: {formatPrice(a.depositAmount)}만원</span>}
                        {a.monthlyRentAmount != null && <span style={{ fontSize: 14, fontWeight: 500 }}>월세: {formatPrice(a.monthlyRentAmount)}만원</span>}
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
                        {a.applicationEndDate && <span style={{ fontSize: 12, color: '#6a6a6a' }}>마감: {a.applicationEndDate}</span>}
                        <StatusBadge status={a.noticeStatus} />
                        {dday && <span style={{ fontSize: 12, fontWeight: 600, color: ddayColor }}>{dday}</span>}
                      </div>
                      {fav.favoritedAt && (
                        <p style={{ fontSize: 12, color: '#6a6a6a' }}>즐겨찾기 추가일: {fav.favoritedAt.split('T')[0]}</p>
                      )}
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
                    <button onClick={() => navigate(`/announcements/${aid}`)}
                      style={{
                        padding: '10px 20px', borderRadius: 12, border: '1px solid #c1c1c1', background: '#fff',
                        fontSize: 14, fontWeight: 500, color: '#222', cursor: 'pointer',
                      }}>
                      공고 상세보기
                    </button>
                    {confirmId === aid ? (
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button onClick={() => removeFavorite(aid)}
                          style={{
                            padding: '10px 16px', borderRadius: 12, border: 'none', background: '#ff385c',
                            fontSize: 14, fontWeight: 600, color: '#fff', cursor: 'pointer',
                          }}>확인</button>
                        <button onClick={() => setConfirmId(null)}
                          style={{
                            padding: '10px 16px', borderRadius: 12, border: '1px solid #c1c1c1', background: '#fff',
                            fontSize: 14, fontWeight: 500, color: '#6a6a6a', cursor: 'pointer',
                          }}>취소</button>
                      </div>
                    ) : (
                      <button onClick={() => setConfirmId(aid)}
                        style={{
                          padding: '10px 20px', borderRadius: 12, border: '1px solid #ff385c', background: '#fff',
                          fontSize: 14, fontWeight: 500, color: '#ff385c', cursor: 'pointer',
                        }}>
                        즐겨찾기 해제
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      ) : view === 'schedule' ? (
        <ScheduleView schedule={schedule} loading={scheduleLoading} />
      ) : (
        <CalendarView schedule={schedule} loading={scheduleLoading} />
      )}
    </div>
  );
}
