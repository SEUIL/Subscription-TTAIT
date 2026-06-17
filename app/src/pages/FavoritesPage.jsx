import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Pagination from '../components/common/Pagination';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import StatusBadge from '../components/common/StatusBadge';
import { useApi } from '../hooks/useApi';
import { useToast } from '../components/common/Toast';
import { formatPrice, calcDday, getDdayColor } from '../components/common/AnnouncementCard';

const STATUS_META = {
  DUE_TODAY: { label: '오늘 마감', color: '#EF4444', bg: '#FEF2F2' },
  DUE_TOMORROW: { label: '내일 마감', color: '#F97316', bg: '#FFF7ED' },
  DUE_SOON: { label: '마감 임박', color: '#F59E0B', bg: '#FFFBEB' },
  OPEN: { label: '접수 중', color: '#16A34A', bg: '#F0FDF4' },
  UPCOMING: { label: '접수 예정', color: '#2563EB', bg: '#EFF6FF' },
  DATE_UNKNOWN: { label: '일정 확인 필요', color: '#6B7280', bg: '#F3F4F6' },
  CLOSED: { label: '마감됨', color: '#9CA3AF', bg: '#F3F4F6' },
};

const EVENT_META = {
  ANNOUNCEMENT_DATE: { label: '공고일', color: '#F59E0B', bg: '#FFFBEB' },
  APPLICATION_START: { label: '신청 시작', color: '#2563EB', bg: '#EFF6FF' },
  APPLICATION_END: { label: '신청 마감', color: '#EF4444', bg: '#FEF2F2' },
  WINNER_ANNOUNCEMENT: { label: '당첨 발표', color: '#16A34A', bg: '#F0FDF4' },
};

const SUMMARY_ITEMS = [
  { key: 'totalCount', label: '전체 즐겨찾기', color: '#222', statuses: null },
  { key: 'dueTodayCount', label: '오늘 마감', color: '#EF4444', statuses: ['DUE_TODAY'] },
  { key: 'dueTomorrowCount', label: '내일 마감', color: '#F97316', statuses: ['DUE_TOMORROW'] },
  { key: 'dueSoonCount', label: '7일 이내', color: '#F59E0B', statuses: ['DUE_TODAY', 'DUE_TOMORROW', 'DUE_SOON'] },
  { key: 'openCount', label: '접수 중', color: '#16A34A', statuses: ['DUE_TODAY', 'DUE_TOMORROW', 'DUE_SOON', 'OPEN'] },
  { key: 'upcomingCount', label: '접수 예정', color: '#2563EB', statuses: ['UPCOMING'] },
];

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

function fmtDate(value) {
  if (!value) return '미정';
  return String(value).slice(0, 10).replaceAll('-', '.');
}

function toDateKey(value) {
  return String(value || '').slice(0, 10);
}

function monthLabel(date) {
  return `${date.getFullYear()}년 ${date.getMonth() + 1}월`;
}

function getStatusMeta(status) {
  return STATUS_META[status] || { label: status || '상태 미확인', color: '#6B7280', bg: '#F3F4F6' };
}

function getEventMeta(type) {
  return EVENT_META[type] || { label: type || '일정', color: '#6B7280', bg: '#F3F4F6' };
}

function ChevronLeftIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
      <path d="M15 18l-6-6 6-6" />
    </svg>
  );
}

function ChevronRightIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
      <path d="M9 18l6-6-6-6" />
    </svg>
  );
}

function SummaryBadges({ summary, activeFilter, onFilterChange }) {
  if (!summary) return null;

  return (
    <section style={S.section}>
      <div style={S.sectionHead}>
        <div>
          <h2 style={S.sectionTitle}>일정 요약</h2>
          <p style={S.sectionDesc}>항목을 클릭하면 해당 상태만 필터링됩니다.</p>
        </div>
        {summary.truncated && <span style={S.truncatedBadge}>200개 초과, 일부만 표시</span>}
      </div>
      <div style={S.summaryGrid}>
        {SUMMARY_ITEMS.map(item => {
          const isActive = activeFilter === item.key;
          return (
            <button
              key={item.key}
              type="button"
              style={S.summaryCard(item.key === 'dueTodayCount', isActive, item.color)}
              onClick={() => onFilterChange && onFilterChange(isActive ? null : item.key)}
            >
              <span style={{ ...S.summaryLabel, color: isActive ? 'rgba(255,255,255,0.85)' : '#6a6a6a' }}>{item.label}</span>
              <strong style={{ ...S.summaryValue, color: isActive ? '#fff' : item.color }}>{summary[item.key] ?? 0}</strong>
            </button>
          );
        })}
      </div>
    </section>
  );
}

function ScheduleView({ schedule, loading, onOpenAnnouncement, activeFilter, onClearFilter }) {
  const [hoveredId, setHoveredId] = useState(null);

  if (loading) return <LoadingSpinner />;

  const groups = Array.isArray(schedule?.groups) ? schedule.groups : [];
  const filterStatuses = activeFilter
    ? (SUMMARY_ITEMS.find(i => i.key === activeFilter)?.statuses ?? null)
    : null;
  const visibleGroups = groups
    .filter(group => Array.isArray(group.items) && group.items.length > 0)
    .filter(group => !filterStatuses || filterStatuses.includes(group.key));

  if (visibleGroups.length === 0) {
    return (
      <section style={S.section}>
        <EmptyState
          icon={activeFilter ? '🔍' : '♡'}
          title={activeFilter ? '해당 상태의 공고가 없습니다' : '즐겨찾기한 공고가 없습니다'}
          description={activeFilter ? '다른 항목을 선택하거나 전체 보기를 클릭해보세요.' : '관심 있는 공고를 즐겨찾기하면 마감 일정이 여기에 표시됩니다.'}
        />
      </section>
    );
  }

  return (
    <section style={S.section}>
      <div style={S.sectionHead}>
        <div>
          <h2 style={S.sectionTitle}>상태별 일정</h2>
          <p style={S.sectionDesc}>
            {activeFilter
              ? `'${SUMMARY_ITEMS.find(i => i.key === activeFilter)?.label}' 필터 적용 중`
              : '마감이 임박한 순서로 정렬됩니다.'}
          </p>
        </div>
        {activeFilter && (
          <button
            type="button"
            style={{ padding: '6px 12px', borderRadius: 999, border: '1px solid #c1c1c1', background: '#fff', fontSize: 12, fontWeight: 700, color: '#6a6a6a', cursor: 'pointer' }}
            onClick={() => onClearFilter && onClearFilter()}
          >
            필터 해제
          </button>
        )}
      </div>

      <div style={S.groupStack}>
        {visibleGroups.map(group => {
          const items = group.items;
          const meta = getStatusMeta(group.key);

          return (
            <div key={group.key || group.label} style={S.groupBlock}>
              <div style={S.groupTitleRow}>
                <span style={{ ...S.groupDot, background: meta.color }} />
                <h3 style={S.groupTitle}>{group.label || meta.label}</h3>
                <span style={S.groupCount}>{items.length}개</span>
              </div>

              <div style={S.scheduleCards}>
                {items.map(item => {
                  const itemMeta = getStatusMeta(item.scheduleStatus || group.key);
                  const isClosed = item.scheduleStatus === 'CLOSED';
                  const isHovered = hoveredId === item.announcementId;

                  return (
                    <article
                      key={item.announcementId}
                      style={{
                        ...S.scheduleCard(itemMeta, isClosed),
                        boxShadow: isHovered ? 'var(--shadow-hover)' : 'none',
                        transform: isHovered ? 'translateY(-1px)' : 'none',
                      }}
                      onMouseEnter={() => setHoveredId(item.announcementId)}
                      onMouseLeave={() => setHoveredId(null)}
                    >
                      <div style={S.scheduleMain}>
                        <div style={S.badgeRow}>
                          {item.dDayLabel && (
                            <span style={S.ddayBadge(itemMeta)}>{item.dDayLabel}</span>
                          )}
                          <span style={S.statusBadge(itemMeta)}>{itemMeta.label}</span>
                        </div>
                        <h4 style={S.noticeTitle}>{item.noticeName}</h4>
                        <p style={S.noticeProvider}>{item.providerName}</p>
                        {item.statusMessage && <p style={{ ...S.statusMessage, color: itemMeta.color }}>{item.statusMessage}</p>}
                        <div style={S.dateRows}>
                          <span>신청 {fmtDate(item.applicationStartDate)} ~ {fmtDate(item.applicationEndDate)}</span>
                          {item.winnerAnnouncementDate && <span>당첨 발표 {fmtDate(item.winnerAnnouncementDate)}</span>}
                        </div>
                      </div>
                      <button style={S.outlineBtn} onClick={() => onOpenAnnouncement(item.announcementId)}>
                        {item.actionLabel || '공고 확인'}
                      </button>
                    </article>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function CalendarView({ schedule, loading }) {
  const [currentDate, setCurrentDate] = useState(() => new Date());
  const [openDates, setOpenDates] = useState({});

  const toggleDate = (date) => {
    setOpenDates(prev => ({ ...prev, [date]: !prev[date] }));
  };

  // announcementId → scheduleStatus 조인 맵
  const announcementStatusMap = useMemo(() => {
    const map = {};
    (schedule?.groups || []).forEach(group => {
      (group.items || []).forEach(item => {
        map[item.announcementId] = item.scheduleStatus;
      });
    });
    return map;
  }, [schedule]);

  // eventType + scheduleStatus 복합 색상 결정
  const getEffectiveEventMeta = (event) => {
    const status = announcementStatusMap[event.announcementId];
    if (event.eventType === 'APPLICATION_END') {
      const urgentStatuses = ['DUE_TODAY', 'DUE_TOMORROW', 'DUE_SOON'];
      if (urgentStatuses.includes(status)) {
        const sm = STATUS_META[status];
        return { label: '신청 마감', color: sm.color, bg: sm.bg };
      }
      return { label: '신청 마감', color: '#EF4444', bg: '#FEF2F2' };
    }
    if (event.eventType === 'APPLICATION_START') return { label: '신청 시작', color: '#16A34A', bg: '#F0FDF4' };
    if (event.eventType === 'WINNER_ANNOUNCEMENT') return { label: '당첨 발표', color: '#7C3AED', bg: '#F5F3FF' };
    return getEventMeta(event.eventType);
  };

  // 해당 날짜 이벤트들 중 가장 높은 긴급도의 셀 배경색
  const getCellUrgencyBg = (dayEvents) => {
    for (const urgency of ['DUE_TODAY', 'DUE_TOMORROW', 'DUE_SOON']) {
      if (dayEvents.some(ev => announcementStatusMap[ev.announcementId] === urgency && ev.eventType === 'APPLICATION_END')) {
        return STATUS_META[urgency].bg;
      }
    }
    return null;
  };

  // 셀 클릭: 1개면 새탭, 2개 이상이면 아코디언 포커스
  const handleCellClick = (dateKey, dayEvents) => {
    if (dayEvents.length === 0) return;
    if (dayEvents.length === 1) {
      window.open(`/announcements/${dayEvents[0].announcementId}`, '_blank');
    } else {
      setOpenDates(prev => ({ ...prev, [dateKey]: true }));
      setTimeout(() => {
        const el = document.getElementById(`accordion-${dateKey}`);
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 50);
    }
  };

  const events = (Array.isArray(schedule?.calendarEvents) ? schedule.calendarEvents : [])
    .filter(ev => ev.eventType !== 'ANNOUNCEMENT_DATE');

  const eventsByDate = useMemo(() => {
    return events.reduce((acc, event) => {
      const key = toDateKey(event.date);
      if (!key) return acc;
      acc[key] = acc[key] || [];
      acc[key].push(event);
      acc[key].sort((a, b) => (a.priority === b.priority ? 0 : a.priority === 'HIGH' ? -1 : 1));
      return acc;
    }, {});
  }, [events]);

  const sortedDateGroups = useMemo(() => Object.entries(eventsByDate).sort(([a], [b]) => a.localeCompare(b)), [eventsByDate]);

  if (loading) return <LoadingSpinner />;

  if (!schedule || events.length === 0) {
    return (
      <section style={S.section}>
        <div style={S.sectionHead}>
          <div>
            <h2 style={S.sectionTitle}>캘린더</h2>
            <p style={S.sectionDesc}>표시할 일정 이벤트가 없습니다.</p>
          </div>
        </div>
      </section>
    );
  }

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const cells = [
    ...Array(firstDay.getDay()).fill(null),
    ...Array.from({ length: lastDay.getDate() }, (_, i) => i + 1),
  ];
  while (cells.length % 7 !== 0) cells.push(null);

  const todayStr = new Date().toISOString().slice(0, 10);
  const pad = (n) => String(n).padStart(2, '0');

  return (
    <section style={S.section}>
      <div style={S.sectionHead}>
        <div>
          <h2 style={S.sectionTitle}>캘린더</h2>
          <p style={S.sectionDesc}>신청 시작, 신청 마감, 당첨 발표를 날짜별로 묶었습니다.</p>
        </div>
        <div style={S.calendarNav}>
          <button style={S.iconBtn} aria-label="이전 달" onClick={() => setCurrentDate(new Date(year, month - 1, 1))}>
            <ChevronLeftIcon />
          </button>
          <strong style={S.monthText}>{monthLabel(currentDate)}</strong>
          <button style={S.iconBtn} aria-label="다음 달" onClick={() => setCurrentDate(new Date(year, month + 1, 1))}>
            <ChevronRightIcon />
          </button>
        </div>
      </div>

      <div style={S.calendarShell}>
        <div style={S.weekHeader}>
          {WEEKDAYS.map((day, index) => (
            <div key={day} style={{ ...S.weekDay, color: index === 0 ? '#EF4444' : index === 6 ? '#2563EB' : '#6a6a6a' }}>
              {day}
            </div>
          ))}
        </div>
        <div style={S.calendarGrid}>
          {cells.map((day, index) => {
            if (!day) return <div key={`blank-${index}`} style={S.calendarCellBlank} />;

            const dateKey = `${year}-${pad(month + 1)}-${pad(day)}`;
            const dayEvents = eventsByDate[dateKey] || [];
            const isToday = dateKey === todayStr;
            const dayOfWeek = (firstDay.getDay() + day - 1) % 7;
            const urgencyBg = getCellUrgencyBg(dayEvents);

            return (
              <div
                key={dateKey}
                style={{
                  ...S.calendarCell(isToday),
                  background: urgencyBg || (isToday ? '#fff0f3' : '#fff'),
                  cursor: dayEvents.length > 0 ? 'pointer' : 'default',
                }}
                onClick={() => handleCellClick(dateKey, dayEvents)}
              >
                <div style={{
                  ...S.dayNumber,
                  color: isToday ? '#ff385c' : dayOfWeek === 0 ? '#EF4444' : dayOfWeek === 6 ? '#2563EB' : '#222',
                  fontWeight: isToday ? 800 : 600,
                }}>
                  {day}
                </div>
                <div style={S.eventPillStack}>
                  {dayEvents.slice(0, 3).map((event, eventIndex) => {
                    const meta = getEffectiveEventMeta(event);
                    return (
                      <div
                        key={`${event.eventType}-${eventIndex}`}
                        style={{ ...S.eventPill(meta, event.priority === 'HIGH'), cursor: 'pointer' }}
                        title={event.title}
                        onClick={(e) => {
                          e.stopPropagation();
                          window.open(`/announcements/${event.announcementId}`, '_blank');
                        }}
                      >
                        <span>{meta.label}</span>
                        <strong>{event.title}</strong>
                      </div>
                    );
                  })}
                  {dayEvents.length > 3 && <span style={S.moreEvents}>+{dayEvents.length - 3}</span>}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div style={S.eventList}>
        {sortedDateGroups.map(([date, dateEvents]) => {
          const isOpen = !!openDates[date];
          const hasHigh = dateEvents.some(e => e.priority === 'HIGH');
          return (
            <div key={date} id={`accordion-${date}`} style={S.accordionItem}>
              <button
                type="button"
                style={S.accordionHeader(isOpen)}
                onClick={() => toggleDate(date)}
              >
                <div style={S.accordionLeft}>
                  {hasHigh && <span style={S.accordionDot} />}
                  <span style={S.eventDate}>{fmtDate(date)}</span>
                  <span style={S.accordionCount}>{dateEvents.length}개</span>
                </div>
                <svg
                  viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                  width="16" height="16"
                  style={{ transition: 'transform 0.2s', transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)', color: '#6a6a6a', flexShrink: 0 }}
                >
                  <path d="M6 9l6 6 6-6" />
                </svg>
              </button>
              {isOpen && (
                <div style={S.accordionBody}>
                  {dateEvents.map((event, index) => {
                    const meta = getEffectiveEventMeta(event);
                    return (
                      <div
                        key={`${date}-${event.eventType}-${index}`}
                        style={{ ...S.eventItem(event.priority === 'HIGH'), cursor: 'pointer' }}
                        onClick={() => window.open(`/announcements/${event.announcementId}`, '_blank')}
                      >
                        <span style={S.eventType(meta)}>{meta.label}</span>
                        <span style={S.eventTitle}>{event.title}</span>
                        {event.priority === 'HIGH' && <span style={S.highBadge}>중요</span>}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}

function Disclaimer({ text }) {
  if (!text) return null;

  return (
    <p style={S.disclaimer}>
      {text}
    </p>
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
  const [activeFilter, setActiveFilter] = useState(null);

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
    } catch {
      /* useApi handles 401 */
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [page]);

  const loadSchedule = async () => {
    if (scheduleLoaded || scheduleLoading) return;

    setScheduleLoading(true);
    try {
      const res = await api.get('/api/me/favorites/schedule');
      if (res.ok) {
        setSchedule(await res.json());
      }
    } catch {
      /* useApi handles 401 */
    } finally {
      setScheduleLoading(false);
      setScheduleLoaded(true);
    }
  };

  useEffect(() => {
    if ((view === 'schedule' || view === 'calendar') && !scheduleLoaded) {
      loadSchedule();
    }
  }, [view, scheduleLoaded]);

  const removeFavorite = async (id) => {
    try {
      await api.del(`/api/me/favorites/${id}`);
      toast('즐겨찾기가 해제되었습니다');
      setConfirmId(null);
      setFavorites(prev => prev.filter(f => (f.announcement?.announcementId || f.announcementId) !== id));
      setTotalCount(prev => Math.max(0, prev - 1));
      setScheduleLoaded(false);
      setSchedule(null);
    } catch {
      toast('오류가 발생했습니다', 'error');
    }
  };

  const openAnnouncement = (id) => {
    if (id) navigate(`/announcements/${id}`);
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div style={S.page}>
      <div style={S.titleRow}>
        <div>
          <h1 style={S.pageTitle}>즐겨찾기</h1>
          <p style={S.pageDesc}>총 <strong style={{ color: '#222' }}>{totalCount}</strong>건</p>
        </div>
      </div>

      <div style={S.tabs}>
        {[
          { key: 'list', label: '목록' },
          { key: 'schedule', label: '일정 관리' },
          { key: 'calendar', label: '캘린더' },
        ].map(({ key, label }) => (
          <button key={key} onClick={() => setView(key)} style={S.tab(view === key)}>
            {label}
          </button>
        ))}
      </div>

      {view === 'list' && (
        favorites.length === 0 ? (
          <EmptyState
            icon="♡"
            title="즐겨찾기한 공고가 없습니다"
            description="관심 있는 공고를 즐겨찾기하면 마감일 알림을 받을 수 있습니다."
            actionLabel="공고 검색하기"
            onAction={() => navigate('/announcements')}
          />
        ) : (
          <>
            <div style={S.favoriteList}>
              {favorites.map(fav => {
                const announcement = fav.announcement || fav;
                const id = announcement.announcementId;
                const dday = calcDday(announcement.applicationEndDate);
                const ddayColor = getDdayColor(announcement.applicationEndDate);

                return (
                  <article key={id} style={S.favoriteCard}>
                    <div style={S.favoriteCardTop}>
                      <button style={S.favoriteMain} onClick={() => navigate(`/announcements/${id}`)}>
                        <h3 style={S.favoriteTitle}>{announcement.noticeName}</h3>
                        <p style={S.favoriteMeta}>
                          {announcement.providerName}
                          {announcement.regionLevel1 ? ` | ${announcement.regionLevel1}${announcement.regionLevel2 ? ` ${announcement.regionLevel2}` : ''}` : ''}
                        </p>
                        <div style={S.priceRow}>
                          {announcement.depositAmount != null && <span>보증금 {formatPrice(announcement.depositAmount)}만원</span>}
                          {announcement.monthlyRentAmount != null && <span>월세 {formatPrice(announcement.monthlyRentAmount)}만원</span>}
                        </div>
                        <div style={S.listMetaRow}>
                          {announcement.applicationEndDate && <span>마감: {fmtDate(announcement.applicationEndDate)}</span>}
                          <StatusBadge status={announcement.noticeStatus} />
                          {dday && <span style={{ color: ddayColor, fontWeight: 700 }}>{dday}</span>}
                        </div>
                        {fav.favoritedAt && <p style={S.favoritedAt}>즐겨찾기 추가일: {fmtDate(fav.favoritedAt)}</p>}
                      </button>
                    </div>

                    <div style={S.cardActions}>
                      <button onClick={() => navigate(`/announcements/${id}`)} style={S.secondaryBtn}>공고 상세보기</button>
                      {confirmId === id ? (
                        <div style={S.confirmActions}>
                          <button onClick={() => removeFavorite(id)} style={S.primaryBtn}>확인</button>
                          <button onClick={() => setConfirmId(null)} style={S.secondaryBtn}>취소</button>
                        </div>
                      ) : (
                        <button onClick={() => setConfirmId(id)} style={S.dangerBtn}>즐겨찾기 해제</button>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
            <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
          </>
        )
      )}

      {view === 'schedule' && (
        <>
          <SummaryBadges summary={schedule?.summary} activeFilter={activeFilter} onFilterChange={setActiveFilter} />
          <ScheduleView schedule={schedule} loading={scheduleLoading} onOpenAnnouncement={openAnnouncement} activeFilter={activeFilter} onClearFilter={() => setActiveFilter(null)} />
          <Disclaimer text={schedule?.disclaimer} />
        </>
      )}

      {view === 'calendar' && (
        <>
          <CalendarView schedule={schedule} loading={scheduleLoading} />
          <Disclaimer text={schedule?.disclaimer} />
        </>
      )}
    </div>
  );
}

const S = {
  page: { maxWidth: 980, margin: '0 auto', padding: '32px 24px 80px' },
  titleRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 16, marginBottom: 20 },
  pageTitle: { fontSize: 26, fontWeight: 700, color: '#222', marginBottom: 8, letterSpacing: 0 },
  pageDesc: { fontSize: 14, color: '#6a6a6a' },
  tabs: { display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 24 },
  tab: (active) => ({
    padding: '8px 18px',
    borderRadius: 20,
    border: 'none',
    cursor: 'pointer',
    background: active ? '#ff385c' : '#f2f2f2',
    color: active ? '#fff' : '#222',
    fontWeight: 700,
    fontSize: 13,
    transition: 'background 0.15s, color 0.15s',
  }),
  section: {
    background: '#fff',
    borderRadius: 20,
    boxShadow: 'var(--shadow-card)',
    padding: 24,
    marginBottom: 20,
  },
  sectionHead: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 16,
    marginBottom: 18,
    flexWrap: 'wrap',
  },
  sectionTitle: { fontSize: 18, fontWeight: 700, color: '#222', marginBottom: 4, letterSpacing: 0 },
  sectionDesc: { fontSize: 13, color: '#6a6a6a', lineHeight: 1.6 },
  truncatedBadge: {
    padding: '6px 10px',
    borderRadius: 999,
    background: '#fff0f3',
    color: '#ff385c',
    fontSize: 12,
    fontWeight: 800,
    whiteSpace: 'nowrap',
  },
  summaryGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: 10 },
  summaryCard: (urgent, active, activeColor) => ({
    padding: '14px 16px',
    borderRadius: 14,
    background: active ? (activeColor || '#222') : urgent ? '#fff0f3' : '#f7f7f7',
    border: active ? `2px solid ${activeColor || '#222'}` : urgent ? '1px solid #ffd0d9' : '1px solid rgba(0,0,0,0.04)',
    outline: 'none',
    minHeight: 80,
    display: 'flex',
    flexDirection: 'column',
    justifyContent: 'space-between',
    cursor: 'pointer',
    textAlign: 'left',
    transition: 'background 0.15s, transform 0.1s, box-shadow 0.15s',
    transform: active ? 'translateY(-2px)' : 'none',
    boxShadow: active ? `0 4px 12px ${activeColor}44` : 'none',
  }),
  summaryLabel: { fontSize: 12, fontWeight: 700, color: '#6a6a6a' },
  summaryValue: { fontSize: 24, fontWeight: 800, lineHeight: 1 },
  loadingBox: {
    padding: 40,
    textAlign: 'center',
    color: '#6a6a6a',
    fontSize: 14,
    background: '#fff',
    borderRadius: 20,
    boxShadow: 'var(--shadow-card)',
  },
  groupStack: { display: 'flex', flexDirection: 'column', gap: 20 },
  groupBlock: { display: 'flex', flexDirection: 'column', gap: 12 },
  groupTitleRow: { display: 'flex', alignItems: 'center', gap: 8 },
  groupDot: { width: 10, height: 10, borderRadius: '50%', flexShrink: 0 },
  groupTitle: { fontSize: 15, fontWeight: 800, color: '#222', letterSpacing: 0 },
  groupCount: { fontSize: 13, color: '#6a6a6a', fontWeight: 600 },
  emptyGroup: {
    padding: '14px 16px',
    borderRadius: 14,
    background: '#f7f7f7',
    color: '#9CA3AF',
    fontSize: 13,
  },
  scheduleCards: { display: 'flex', flexDirection: 'column', gap: 10 },
  scheduleCard: (meta, closed) => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 16,
    padding: '18px 20px',
    borderRadius: 16,
    border: '1px solid rgba(0,0,0,0.06)',
    borderLeft: `4px solid ${meta.color}`,
    background: closed ? '#fafafa' : '#fff',
    opacity: closed ? 0.72 : 1,
    transition: 'box-shadow 0.15s, transform 0.15s',
  }),
  scheduleMain: { minWidth: 0, flex: 1 },
  badgeRow: { display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10, flexWrap: 'wrap' },
  ddayBadge: (meta) => ({
    padding: '5px 9px',
    borderRadius: 999,
    background: meta.color,
    color: '#fff',
    fontSize: 12,
    fontWeight: 800,
  }),
  statusBadge: (meta) => ({
    padding: '5px 9px',
    borderRadius: 999,
    background: meta.bg,
    color: meta.color,
    fontSize: 12,
    fontWeight: 800,
  }),
  noticeTitle: {
    fontSize: 16,
    fontWeight: 800,
    color: '#222',
    lineHeight: 1.45,
    marginBottom: 5,
    wordBreak: 'keep-all',
  },
  noticeProvider: { fontSize: 13, color: '#6a6a6a', marginBottom: 6 },
  statusMessage: { fontSize: 13, fontWeight: 700, marginBottom: 8, lineHeight: 1.5 },
  dateRows: { display: 'flex', gap: 12, flexWrap: 'wrap', fontSize: 12, color: '#6a6a6a', lineHeight: 1.6 },
  outlineBtn: {
    padding: '10px 16px',
    borderRadius: 12,
    border: '1px solid #222',
    background: '#fff',
    color: '#222',
    fontSize: 13,
    fontWeight: 800,
    cursor: 'pointer',
    whiteSpace: 'nowrap',
    flexShrink: 0,
  },
  calendarNav: { display: 'flex', alignItems: 'center', gap: 8 },
  iconBtn: {
    width: 36,
    height: 36,
    borderRadius: '50%',
    border: '1px solid #c1c1c1',
    background: '#fff',
    color: '#222',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: 'pointer',
  },
  monthText: { minWidth: 96, textAlign: 'center', fontSize: 15, color: '#222' },
  calendarShell: { border: '1px solid rgba(0,0,0,0.06)', borderRadius: 16, overflow: 'auto' },
  weekHeader: { display: 'grid', gridTemplateColumns: 'repeat(7, minmax(0, 1fr))', background: '#f7f7f7', minWidth: 560 },
  weekDay: { textAlign: 'center', padding: '10px 0', fontSize: 12, fontWeight: 800 },
  calendarGrid: { display: 'grid', gridTemplateColumns: 'repeat(7, minmax(0, 1fr))', minWidth: 560 },
  calendarCellBlank: { minHeight: 110, borderTop: '1px solid rgba(0,0,0,0.05)', borderRight: '1px solid rgba(0,0,0,0.05)' },
  calendarCell: (today) => ({
    minHeight: 110,
    padding: 8,
    borderTop: '1px solid rgba(0,0,0,0.05)',
    borderRight: '1px solid rgba(0,0,0,0.05)',
    background: today ? '#fff0f3' : '#fff',
    minWidth: 0,
  }),
  dayNumber: { textAlign: 'right', fontSize: 12, marginBottom: 6 },
  eventPillStack: { display: 'flex', flexDirection: 'column', gap: 4, minWidth: 0 },
  eventPill: (meta, high) => ({
    display: 'flex',
    flexDirection: 'column',
    gap: 1,
    padding: '4px 5px',
    borderRadius: 6,
    background: meta.bg,
    border: high ? `1px solid ${meta.color}` : '1px solid transparent',
    color: meta.color,
    overflow: 'hidden',
    minWidth: 0,
    fontSize: 10,
    lineHeight: 1.25,
  }),
  moreEvents: { fontSize: 10, fontWeight: 800, color: '#6a6a6a', paddingLeft: 2 },
  eventList: { marginTop: 18, display: 'flex', flexDirection: 'column', gap: 6 },
  accordionItem: { borderRadius: 12, border: '1px solid rgba(0,0,0,0.07)', overflow: 'hidden' },
  accordionHeader: (open) => ({
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '12px 16px',
    background: open ? '#f7f7f7' : '#fff',
    border: 'none',
    cursor: 'pointer',
    textAlign: 'left',
    transition: 'background 0.15s',
  }),
  accordionLeft: { display: 'flex', alignItems: 'center', gap: 8 },
  accordionDot: { width: 7, height: 7, borderRadius: '50%', background: '#ff385c', flexShrink: 0 },
  accordionCount: { fontSize: 12, color: '#6a6a6a', fontWeight: 600 },
  accordionBody: { padding: '8px 16px 12px', display: 'flex', flexDirection: 'column', gap: 6, borderTop: '1px solid rgba(0,0,0,0.06)' },
  eventDate: { fontSize: 13, color: '#222', fontWeight: 800 },
  eventItems: { display: 'flex', flexDirection: 'column', gap: 6 },
  eventItem: (high) => ({
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    minWidth: 0,
    padding: high ? '8px 10px' : '6px 0',
    borderRadius: 10,
    background: high ? '#fff0f3' : 'transparent',
  }),
  eventType: (meta) => ({
    padding: '4px 8px',
    borderRadius: 999,
    background: meta.bg,
    color: meta.color,
    fontSize: 11,
    fontWeight: 800,
    whiteSpace: 'nowrap',
  }),
  eventTitle: { fontSize: 13, color: '#222', fontWeight: 600, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  highBadge: { marginLeft: 'auto', fontSize: 11, fontWeight: 800, color: '#ff385c', whiteSpace: 'nowrap' },
  disclaimer: {
    padding: '12px 16px',
    borderRadius: 14,
    background: '#f7f7f7',
    color: '#6a6a6a',
    fontSize: 12,
    lineHeight: 1.6,
    textAlign: 'center',
  },
  favoriteList: { display: 'flex', flexDirection: 'column', gap: 16 },
  favoriteCard: { background: '#fff', borderRadius: 20, padding: 24, boxShadow: 'var(--shadow-card)' },
  favoriteCardTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' },
  favoriteMain: { flex: 1, cursor: 'pointer', border: 'none', background: 'transparent', textAlign: 'left', padding: 0, minWidth: 0 },
  favoriteTitle: { fontSize: 16, fontWeight: 800, color: '#222', marginBottom: 8, lineHeight: 1.45, wordBreak: 'keep-all' },
  favoriteMeta: { fontSize: 13, color: '#6a6a6a', marginBottom: 8 },
  priceRow: { display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap', marginBottom: 8, fontSize: 14, fontWeight: 600, color: '#222' },
  listMetaRow: { display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 8, fontSize: 12, color: '#6a6a6a' },
  favoritedAt: { fontSize: 12, color: '#6a6a6a' },
  cardActions: { display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end', flexWrap: 'wrap' },
  confirmActions: { display: 'flex', gap: 4 },
  secondaryBtn: {
    padding: '10px 20px',
    borderRadius: 12,
    border: '1px solid #c1c1c1',
    background: '#fff',
    fontSize: 14,
    fontWeight: 600,
    color: '#222',
    cursor: 'pointer',
  },
  primaryBtn: {
    padding: '10px 16px',
    borderRadius: 12,
    border: 'none',
    background: '#ff385c',
    fontSize: 14,
    fontWeight: 800,
    color: '#fff',
    cursor: 'pointer',
  },
  dangerBtn: {
    padding: '10px 20px',
    borderRadius: 12,
    border: '1px solid #ff385c',
    background: '#fff',
    fontSize: 14,
    fontWeight: 700,
    color: '#ff385c',
    cursor: 'pointer',
  },
};
