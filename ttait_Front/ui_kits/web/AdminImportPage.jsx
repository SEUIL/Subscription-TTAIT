// AdminImportPage.jsx — 관리자 LH 후보 수집/import 콘솔 목업
// 실제 API 연동 없이 다음 프론트 작업자가 이식할 수 있도록 UI 상태와 패턴만 제공합니다.

const LH_CANDIDATES = [
  { id: 12041, panId: 'LN-2026-001284', title: '서울 강동천호 행복주택 예비입주자 모집공고', region: '서울 강동구', status: 'COLLECTED', dedupeStatus: 'NEW', canParse: true, alreadyImported: false, pdfUrl: '#', sourceNoticeUrl: '#' },
  { id: 12040, panId: 'LN-2026-001276', title: '인천검단 AA35블록 국민임대주택 입주자 추가모집', region: '인천 서구', status: 'IMPORTED', dedupeStatus: 'UNCHANGED_SKIP_GEMINI', canParse: true, alreadyImported: true, pdfUrl: '#', sourceNoticeUrl: '#' },
  { id: 12039, panId: 'LN-2026-001258', title: '고양장항 A-4 신혼희망타운 공공분양 정정공고', region: '경기 고양시', status: 'COLLECTED', dedupeStatus: 'CHANGED_REPARSE', canParse: true, alreadyImported: true, pdfUrl: '#', sourceNoticeUrl: '#' },
  { id: 12038, panId: 'LN-2026-001244', title: '부산명지 토지 공급 공고', region: '부산 강서구', status: 'SKIPPED', dedupeStatus: 'LAND_SKIP', canParse: false, alreadyImported: false, pdfUrl: '', sourceNoticeUrl: '#' },
  { id: 12037, panId: 'LN-2026-001229', title: '대전도안 영구임대주택 입주자 모집공고', region: '대전 유성구', status: 'FAILED', dedupeStatus: 'FAILED_RETRY', canParse: true, alreadyImported: false, pdfUrl: '#', sourceNoticeUrl: '#' },
  { id: 12036, panId: 'LN-2026-001211', title: '수원당수 A-1 통합공공임대주택 예비입주자 모집', region: '경기 수원시', status: 'COLLECTED', dedupeStatus: 'NO_PDF', canParse: false, alreadyImported: false, pdfUrl: '', sourceNoticeUrl: '#' },
];

const STATUS_BADGE = {
  COLLECTED: ['수집됨', 'info'],
  IMPORTED: ['import 완료', 'success'],
  FAILED: ['실패', 'danger'],
  SKIPPED: ['제외', 'neutral'],
};

const DEDUPE_BADGE = {
  NEW: ['신규', 'brand'],
  UNCHANGED_SKIP_GEMINI: ['변경 없음', 'neutral'],
  CHANGED_REPARSE: ['변경 감지', 'warning'],
  FAILED_RETRY: ['실패 재시도', 'danger'],
  FORCE_REPARSE: ['강제 재파싱', 'warning'],
  NO_PDF: ['PDF 없음', 'mutedWarning'],
  LAND_SKIP: ['토지 제외', 'neutral'],
};

const IMPORT_RESULT = {
  fetched: 6,
  scanned: 6,
  skippedLand: 1,
  unchanged: 1,
  geminiSkipped: 1,
  imported: 2,
  reparsed: 1,
  failed: 1,
};

const ADMIN_IMPORT_STYLE = `
.admin-import-page{min-height:100vh;background:#f2f2f2;color:#222;font-family:'Noto Sans KR',-apple-system,system-ui,sans-serif}
.admin-shell{max-width:1280px;margin:0 auto;padding:32px 24px 120px}
.admin-top{height:72px;background:#fff;border-bottom:1px solid rgba(0,0,0,.08);position:sticky;top:0;z-index:80}
.admin-top-in{max-width:1280px;margin:0 auto;height:100%;padding:0 24px;display:flex;align-items:center;gap:14px}
.admin-logo{width:32px;height:32px;border-radius:8px;background:#ff385c;color:#fff;display:flex;align-items:center;justify-content:center;font-weight:900}
.admin-hero,.admin-card{background:#fff;border-radius:20px;box-shadow:rgba(0,0,0,.02) 0 0 0 1px,rgba(0,0,0,.04) 0 2px 6px,rgba(0,0,0,.1) 0 4px 8px;overflow:hidden}
.admin-hero{margin-bottom:24px}.admin-head{padding:24px;border-bottom:1px solid rgba(0,0,0,.06);display:flex;justify-content:space-between;gap:18px;align-items:flex-start}
.admin-eyebrow{color:#ff385c;font-size:12px;font-weight:900;letter-spacing:.2px;margin-bottom:6px}.admin-title{margin:0 0 10px;font-size:28px;line-height:1.28;letter-spacing:-.5px}.admin-desc{margin:0;color:#6a6a6a;font-size:14px;line-height:1.65}
.admin-grid{display:grid;grid-template-columns:minmax(0,1fr) 340px;gap:24px;align-items:start}.admin-main{display:flex;flex-direction:column;gap:20px;min-width:0}.admin-side{position:sticky;top:94px;display:flex;flex-direction:column;gap:16px}
.admin-pad{padding:24px}.admin-actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}.admin-btn{height:42px;padding:0 16px;border-radius:10px;border:none;font-family:inherit;font-size:13px;font-weight:800;cursor:pointer;display:inline-flex;align-items:center;gap:7px;text-decoration:none;transition:all .15s}.admin-btn:hover{transform:translateY(-1px);box-shadow:rgba(0,0,0,.08) 0 4px 12px}.admin-btn.primary{background:#ff385c;color:#fff}.admin-btn.dark{background:#222;color:#fff}.admin-btn.secondary{background:#f2f2f2;color:#222}.admin-btn.ghost{background:#fff;color:#222;border:1px solid rgba(0,0,0,.1)}.admin-btn.danger{background:#fff0f3;color:#e00b41}
.admin-form{display:grid;grid-template-columns:repeat(2,180px) auto;gap:12px;align-items:end}.admin-field label{display:block;margin-bottom:6px;color:#6a6a6a;font-size:12px;font-weight:800}.admin-field input,.admin-field select{width:100%;height:44px;border:1.5px solid rgba(0,0,0,.12);border-radius:10px;padding:0 13px;font-family:inherit;font-size:14px;background:#fff;color:#222;outline:none}.admin-field input:focus,.admin-field select:focus{border-color:#222;box-shadow:0 0 0 3px rgba(34,34,34,.08)}
.admin-note{margin-top:16px;border-radius:14px;background:#eff6ff;color:#1d4ed8;padding:14px 16px;font-size:13px;font-weight:700;line-height:1.55}.summary{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:18px}.summary-card{border-radius:16px;padding:18px;border:1px solid rgba(0,0,0,.06);background:#fff}.summary-card.info{background:#eff6ff}.summary-card.success{background:#f0fdf4}.summary-card.warning{background:#fff7ed}.summary-label{font-size:12px;font-weight:800;color:#6a6a6a}.summary-value{font-size:28px;font-weight:900;letter-spacing:-.4px;margin-top:5px}.summary-sub{font-size:12px;color:#6a6a6a;margin-top:4px}
.filterbar{display:flex;gap:10px;align-items:end;flex-wrap:wrap;padding:20px 24px;border-bottom:1px solid rgba(0,0,0,.06)}.filterbar .admin-field{min-width:150px}.table-wrap{overflow-x:auto}.candidate-table{width:100%;min-width:980px;border-collapse:collapse}.candidate-table th{text-align:left;padding:12px 14px;background:#fafafa;border-bottom:1px solid rgba(0,0,0,.08);color:#6a6a6a;font-size:12px;font-weight:900;white-space:nowrap}.candidate-table td{padding:15px 14px;border-bottom:1px solid rgba(0,0,0,.06);font-size:13px;vertical-align:middle}.candidate-title{max-width:300px;font-size:14px;font-weight:800;line-height:1.4;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.candidate-meta{margin-top:4px;font-size:12px;color:#6a6a6a}.disabled-row{opacity:.58}
.badge{display:inline-flex;align-items:center;border-radius:999px;padding:5px 10px;font-size:11px;font-weight:900;white-space:nowrap;border:1px solid transparent}.badge.brand{background:#fff0f3;color:#ff385c;border-color:rgba(255,56,92,.18)}.badge.info{background:#eff6ff;color:#1d4ed8;border-color:rgba(29,78,216,.14)}.badge.success{background:#f0fdf4;color:#166534;border-color:rgba(22,101,52,.16)}.badge.warning,.badge.mutedWarning{background:#fff7ed;color:#c2410c;border-color:rgba(194,65,12,.16)}.badge.danger{background:#fff0f3;color:#e00b41;border-color:rgba(224,11,65,.16)}.badge.neutral{background:#f2f2f2;color:#6a6a6a;border-color:rgba(0,0,0,.05)}
.link-row{display:flex;gap:6px}.link-chip{border-radius:8px;background:#f2f2f2;color:#222;padding:7px 9px;text-decoration:none;font-size:12px;font-weight:800}.link-chip.disabled{color:rgba(0,0,0,.24);cursor:not-allowed}.mobile-cards{display:none;padding:16px;gap:12px;flex-direction:column}.candidate-card{border:1px solid rgba(0,0,0,.08);border-radius:16px;background:#fff;padding:16px}.card-top{display:flex;justify-content:space-between;gap:12px}.card-badges{display:flex;gap:6px;flex-wrap:wrap;margin:12px 0}
.workflow{list-style:none;padding:0;margin:0;display:flex;flex-direction:column;gap:14px}.workflow li{display:grid;grid-template-columns:32px 1fr;gap:12px}.step-dot{width:32px;height:32px;border-radius:50%;background:#fff0f3;color:#ff385c;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:900}.side-title{margin:0 0 10px;font-size:16px;font-weight:900}.side-text{margin:3px 0 0;color:#6a6a6a;font-size:13px;line-height:1.55}
.result-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}.result-item{border-radius:14px;background:#f8f8f8;padding:14px}.result-item.info{background:#eff6ff}.result-item.success{background:#f0fdf4}.result-item.warning{background:#fff7ed}.result-item.danger{background:#fff0f3}.result-value{font-size:22px;font-weight:900}.result-label{margin-top:4px;color:#6a6a6a;font-size:11px;font-weight:800}.error-callout{margin-bottom:16px;border-radius:14px;background:#fff0f3;color:#e00b41;padding:14px 16px;font-size:13px;font-weight:700;line-height:1.55}
.sticky-bar{position:fixed;left:50%;bottom:20px;transform:translateX(-50%);z-index:100;width:min(920px,calc(100vw - 32px));border-radius:20px;background:#222;color:#fff;box-shadow:rgba(0,0,0,.2) 0 12px 32px;padding:14px 16px;display:flex;align-items:center;justify-content:space-between;gap:16px}.sticky-title{font-size:14px;font-weight:900}.sticky-desc{font-size:12px;color:rgba(255,255,255,.72)}.force{display:flex;align-items:center;gap:8px;font-size:13px;font-weight:800;white-space:nowrap}.force input{accent-color:#ff385c}.sticky-actions{display:flex;gap:8px;align-items:center}
.modal-backdrop{position:fixed;inset:0;z-index:120;background:rgba(0,0,0,.42);display:flex;align-items:center;justify-content:center;padding:24px}.modal{width:min(480px,100%);border-radius:22px;background:#fff;box-shadow:rgba(0,0,0,.2) 0 20px 60px;padding:24px}.modal-icon{width:48px;height:48px;border-radius:50%;background:#fff7ed;color:#c2410c;display:flex;align-items:center;justify-content:center;font-size:22px;margin-bottom:16px}.modal h3{margin:0 0 10px;font-size:20px;letter-spacing:-.3px}.modal p{margin:0;color:#6a6a6a;font-size:14px;line-height:1.65}.modal-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:22px}
@media(max-width:1040px){.admin-grid{grid-template-columns:1fr}.admin-side{position:static}}@media(max-width:760px){.admin-shell{padding:20px 16px 120px}.admin-head{flex-direction:column}.admin-title{font-size:23px}.admin-actions{justify-content:flex-start}.admin-form{grid-template-columns:1fr}.summary,.result-grid{grid-template-columns:1fr 1fr}.table-wrap{display:none}.mobile-cards{display:flex}.sticky-bar{flex-direction:column;align-items:stretch;border-radius:18px}.sticky-actions{justify-content:space-between}.admin-btn{justify-content:center}}
`;

const Badge = ({ map, value }) => {
  const [label, tone] = map[value] || [value, 'neutral'];
  return <span className={`badge ${tone}`}>{label}</span>;
};

const ToneBadge = ({ label, tone }) => <span className={`badge ${tone}`}>{label}</span>;

const Button = ({ children, tone = 'secondary', onClick, disabled }) => (
  <button className={`admin-btn ${tone}`} onClick={onClick} disabled={disabled} style={disabled ? { opacity:.45, cursor:'not-allowed' } : null}>{children}</button>
);

const SummaryCard = ({ label, value, sub, tone }) => (
  <div className={`summary-card ${tone || ''}`}>
    <div className="summary-label">{label}</div>
    <div className="summary-value">{value}</div>
    <div className="summary-sub">{sub}</div>
  </div>
);

const ResultItem = ({ label, value, tone }) => (
  <div className={`result-item ${tone || ''}`}>
    <div className="result-value">{value}</div>
    <div className="result-label">{label}</div>
  </div>
);

const AdminImportPage = ({ onReviewClick, onDashboardClick }) => {
  const [selected, setSelected] = React.useState([12041, 12039]);
  const [force, setForce] = React.useState(false);
  const [statusFilter, setStatusFilter] = React.useState('전체');
  const [showModal, setShowModal] = React.useState(false);
  const [showResult, setShowResult] = React.useState(true);

  const visibleCandidates = LH_CANDIDATES.filter((item) => statusFilter === '전체' || item.status === statusFilter);
  const toggleSelected = (id) => setSelected((prev) => prev.includes(id) ? prev.filter((v) => v !== id) : [...prev, id]);
  const runImport = () => force ? setShowModal(true) : setShowResult(true);
  const confirmForce = () => { setShowModal(false); setShowResult(true); };

  const CandidateCheck = ({ item }) => (
    <input
      type="checkbox"
      checked={selected.includes(item.id)}
      disabled={!item.canParse}
      onChange={() => toggleSelected(item.id)}
      aria-label={`${item.title} 선택`}
      style={{ width:18, height:18, accentColor:'#ff385c', cursor:item.canParse ? 'pointer' : 'not-allowed' }}
    />
  );

  const Links = ({ item }) => (
    <div className="link-row">
      <a className={`link-chip ${!item.pdfUrl ? 'disabled' : ''}`} href={item.pdfUrl || undefined}>PDF</a>
      <a className="link-chip" href={item.sourceNoticeUrl}>원문</a>
    </div>
  );

  return (
    <div className="admin-import-page">
      <style>{ADMIN_IMPORT_STYLE}</style>
      <header className="admin-top">
        <div className="admin-top-in">
          <div className="admin-logo">집</div>
          <div>
            <div style={{ fontSize:15, fontWeight:900 }}>집구해 관리자</div>
            <div style={{ fontSize:12, color:'#6a6a6a' }}>LH 후보 기반 import 운영 콘솔 목업</div>
          </div>
          <div style={{ marginLeft:'auto', display:'flex', gap:8 }}>
            <Button tone="ghost" onClick={onReviewClick}>검수 목록 보기</Button>
            <Button onClick={onDashboardClick}>관리자 대시보드</Button>
          </div>
        </div>
      </header>

      <main className="admin-shell">
        <section className="admin-hero">
          <div className="admin-head">
            <div>
              <div className="admin-eyebrow">ADMIN · LH IMPORT</div>
              <h1 className="admin-title">Save LH candidates first,<br />then import only selected rows.</h1>
              <p className="admin-desc">Candidate collect/list only saves or reads lh_import_candidate. During selected import, dedupeStatus and fingerprint decide whether Gemini is called.</p>
            </div>
            <div className="admin-actions">
              <Button tone="dark" onClick={onReviewClick}>검수 대기 목록 →</Button>
              <Button onClick={onDashboardClick}>대시보드</Button>
            </div>
          </div>
        </section>

        <div className="admin-grid">
          <div className="admin-main">
            <section className="admin-card">
              <div className="admin-head">
                <div>
                  <div className="admin-eyebrow">STEP 1</div>
                  <h2 style={{ margin:0, fontSize:20 }}>후보 수집</h2>
                  <p className="admin-desc">Save LH list/detail JSON and PDF URL into the lh_import_candidate table.</p>
                </div>
                <ToneBadge label="Gemini 호출 없음" tone="info" />
              </div>
              <div className="admin-pad">
                <div className="admin-form">
                  <div className="admin-field"><label>LH 목록 페이지</label><input type="number" defaultValue="1" min="1" /></div>
                  <div className="admin-field"><label>수집 크기</label><input type="number" defaultValue="10" min="1" /></div>
                  <Button tone="primary">후보 수집 실행</Button>
                </div>
                <div className="admin-note">ℹ️ 이 단계는 후보 판별과 저장만 수행합니다. 실제 연동 시 POST /api/admin/import/lh/candidates/collect로 연결하세요.</div>
                <div className="summary">
                  <SummaryCard label="fetched" value="6" sub="LH 목록에서 받은 항목" tone="info" />
                  <SummaryCard label="scanned" value="6" sub="후보 판별 완료" tone="success" />
                  <SummaryCard label="skippedLand" value="1" sub="토지 공고 제외" tone="warning" />
                </div>
              </div>
            </section>

            <section className="admin-card">
              <div className="admin-head">
                <div>
                  <div className="admin-eyebrow">STEP 2</div>
                  <h2 style={{ margin:0, fontSize:20 }}>후보 목록 및 선택 import</h2>
                  <p className="admin-desc">canParse=false 후보는 선택할 수 없고, 중복/변경 상태를 배지로 먼저 판단합니다.</p>
                </div>
                <ToneBadge label={`${selected.length}개 선택됨`} tone="brand" />
              </div>

              <div className="filterbar">
                <div className="admin-field">
                  <label>상태</label>
                  <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
                    {['전체','COLLECTED','IMPORTED','FAILED','SKIPPED'].map((v) => <option key={v} value={v}>{v}</option>)}
                  </select>
                </div>
                <div className="admin-field"><label>페이지</label><input type="number" defaultValue="0" /></div>
                <div className="admin-field"><label>페이지 크기</label><input type="number" defaultValue="20" /></div>
                <Button>후보 목록 새로고침</Button>
              </div>

              <div className="table-wrap">
                <table className="candidate-table">
                  <thead>
                    <tr>
                      <th>선택</th><th>후보 ID</th><th>공고명</th><th>지역</th><th>후보 상태</th><th>중복 판단</th><th>import 가능</th><th>기존 저장</th><th>링크</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleCandidates.map((item) => (
                      <tr key={item.id} className={!item.canParse ? 'disabled-row' : ''}>
                        <td><CandidateCheck item={item} /></td>
                        <td style={{ fontWeight:900 }}>{item.id}</td>
                        <td><div className="candidate-title">{item.title}</div><div className="candidate-meta">{item.panId}</div></td>
                        <td>{item.region}</td>
                        <td><Badge map={STATUS_BADGE} value={item.status} /></td>
                        <td><Badge map={DEDUPE_BADGE} value={item.dedupeStatus} /></td>
                        <td>{item.canParse ? <ToneBadge label="가능" tone="success" /> : <ToneBadge label="불가" tone="neutral" />}</td>
                        <td>{item.alreadyImported ? <ToneBadge label="있음" tone="info" /> : <ToneBadge label="없음" tone="neutral" />}</td>
                        <td><Links item={item} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mobile-cards">
                {visibleCandidates.map((item) => (
                  <article className="candidate-card" key={item.id}>
                    <div className="card-top"><CandidateCheck item={item} /><Badge map={STATUS_BADGE} value={item.status} /></div>
                    <div className="candidate-title" style={{ marginTop:12 }}>{item.title}</div>
                    <div className="candidate-meta">#{item.id} · {item.panId} · {item.region}</div>
                    <div className="card-badges">
                      <Badge map={DEDUPE_BADGE} value={item.dedupeStatus} />
                      {item.canParse ? <ToneBadge label="import 가능" tone="success" /> : <ToneBadge label="import 불가" tone="neutral" />}
                      {item.alreadyImported && <ToneBadge label="기존 저장 있음" tone="info" />}
                    </div>
                    <Links item={item} />
                  </article>
                ))}
              </div>
            </section>

            {showResult && (
              <section className="admin-card">
                <div className="admin-head">
                  <div>
                    <div className="admin-eyebrow">STEP 3</div>
                    <h2 style={{ margin:0, fontSize:20 }}>실행 결과</h2>
                    <p className="admin-desc">Import means announcement save/update and review queue handoff, not public exposure.</p>
                  </div>
                  <Button tone="dark" onClick={onReviewClick}>검수 대기 목록 보기 →</Button>
                </div>
                <div className="admin-pad">
                  {IMPORT_RESULT.failed > 0 && <div className="error-callout">일부 후보 처리가 실패했습니다. 서버 로그에서 실패 panId와 Gemini quota 또는 원문 API 오류를 확인해주세요.</div>}
                  <div className="result-grid">
                    <ResultItem label="fetched" value={IMPORT_RESULT.fetched} tone="info" />
                    <ResultItem label="scanned" value={IMPORT_RESULT.scanned} tone="info" />
                    <ResultItem label="skippedLand" value={IMPORT_RESULT.skippedLand} tone="warning" />
                    <ResultItem label="unchanged" value={IMPORT_RESULT.unchanged} tone="info" />
                    <ResultItem label="geminiSkipped" value={IMPORT_RESULT.geminiSkipped} tone="success" />
                    <ResultItem label="imported" value={IMPORT_RESULT.imported} tone="success" />
                    <ResultItem label="reparsed" value={IMPORT_RESULT.reparsed} tone="warning" />
                    <ResultItem label="failed" value={IMPORT_RESULT.failed} tone="danger" />
                  </div>
                </div>
              </section>
            )}
          </div>

          <aside className="admin-side">
            <section className="admin-card"><div className="admin-pad">
              <h3 className="side-title">운영 흐름</h3>
              <ol className="workflow">
                <li><span className="step-dot">1</span><div><strong>후보 수집</strong><p className="side-text">LH 원문과 PDF URL만 저장합니다. Gemini 호출 없음.</p></div></li>
                <li><span className="step-dot">2</span><div><strong>후보 검토</strong><p className="side-text">canParse, 중복, PDF 유무를 보고 선택합니다.</p></div></li>
                <li><span className="step-dot">3</span><div><strong>Selected import</strong><p className="side-text">Only candidateIds selected by the admin are saved/updated as announcements; Gemini is called or skipped by dedupeStatus/fingerprint.</p></div></li>
              </ol>
            </div></section>
            <section className="admin-card"><div className="admin-pad">
              <h3 className="side-title">배지 가이드</h3>
              <div style={{ display:'flex', flexWrap:'wrap', gap:8 }}>
                {Object.keys(STATUS_BADGE).map((v) => <Badge key={v} map={STATUS_BADGE} value={v} />)}
                {Object.keys(DEDUPE_BADGE).map((v) => <Badge key={v} map={DEDUPE_BADGE} value={v} />)}
              </div>
            </div></section>
            <section className="admin-card"><div className="admin-pad">
              <h3 className="side-title">비용 보호 원칙</h3>
              <p className="side-text">force=true는 기본값으로 보이지 않게 두고, 선택 시 확인 모달을 반드시 노출합니다. 변경 없음/skip은 실패가 아니라 정상 비용 절감 결과로 표시합니다.</p>
            </div></section>
          </aside>
        </div>
      </main>

      {selected.length > 0 && (
        <div className="sticky-bar">
          <div><div className="sticky-title">{selected.length} candidates selected</div><div className="sticky-desc">Selected import decides Gemini calls by dedupeStatus/fingerprint.</div></div>
          <label className="force"><input type="checkbox" checked={force} onChange={(e) => setForce(e.target.checked)} /> 강제 재파싱</label>
          <div className="sticky-actions"><Button tone="ghost" onClick={() => setSelected([])}>선택 해제</Button><Button tone="primary" onClick={runImport}>선택 후보 import</Button></div>
        </div>
      )}

      {showModal && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <div className="modal">
            <div className="modal-icon">⚠️</div>
            <h3>강제 재파싱을 실행할까요?</h3>
            <p>강제 재파싱은 변경 없는 공고도 Gemini를 다시 호출할 수 있습니다. 정말 선택한 후보를 강제 재파싱할까요?</p>
            <div className="modal-actions"><Button onClick={() => setShowModal(false)}>취소</Button><Button tone="danger" onClick={confirmForce}>강제 재파싱 실행</Button></div>
          </div>
        </div>
      )}
    </div>
  );
};

Object.assign(window, { AdminImportPage });
