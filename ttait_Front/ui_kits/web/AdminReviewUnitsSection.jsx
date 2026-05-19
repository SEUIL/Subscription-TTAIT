// AdminReviewUnitsSection.jsx — 관리자 검수 상세의 공급 단위 검수 섹션 목업
// public 화면이 아닌 AdminReviewDetailPage 전용 패턴입니다.

const REVIEW_UNITS = [
  {
    unitOrder: 1,
    unitSource: 'MERGED',
    matchSource: 'LH_API_AND_PDF_AI',
    confidenceLevel: 'HIGH',
    complexName: '강동천호 행복주택',
    fullAddress: '서울특별시 강동구 천호동 458-3 일원',
    supplyTypeRaw: '행복주택',
    supplyTypeNormalized: '행복주택',
    houseTypeRaw: '청년형 26A',
    houseTypeNormalized: '청년',
    exclusiveAreaText: '26.43㎡',
    depositAmount: '42,000,000원',
    monthlyRentAmount: '178,000원',
    salePriceMin: '-',
    salePriceMax: '-',
    supplyHouseholdCount: '44세대',
    rawText: '26A 청년형 공급호수 44호, 임대보증금 42,000천원, 월임대료 178,000원. 자세한 자격요건은 공고문 본문을 확인해야 합니다.',
    sourceUnitKey: 'P12-TABLE-03-ROW-02',
  },
  {
    unitOrder: 2,
    unitSource: 'PDF_AI',
    matchSource: 'PDF_TABLE_ROW',
    confidenceLevel: 'LOW',
    complexName: '강동천호 행복주택',
    fullAddress: '서울특별시 강동구 천호동 458-3 일원',
    supplyTypeRaw: '고령자 계층',
    supplyTypeNormalized: '고령자',
    houseTypeRaw: '36B',
    houseTypeNormalized: '고령자',
    exclusiveAreaText: '36.82㎡',
    depositAmount: '61,000,000원',
    monthlyRentAmount: '236,000원',
    salePriceMin: '-',
    salePriceMax: '-',
    supplyHouseholdCount: '8세대',
    rawText: 'PDF 표에서 추출한 행입니다. 주석과 셀 병합으로 인해 공급대상 매칭 신뢰도가 낮습니다. 관리자가 원문 표와 대조해야 합니다.',
    sourceUnitKey: 'PDF-UNIT-36B-ELDERLY',
  },
  {
    unitOrder: 3,
    unitSource: 'LH_API',
    matchSource: 'API_ONLY',
    confidenceLevel: 'MEDIUM',
    complexName: '강동천호 행복주택',
    fullAddress: '서울특별시 강동구 천호동 458-3 일원',
    supplyTypeRaw: '신혼부부',
    supplyTypeNormalized: '신혼부부',
    houseTypeRaw: '44A',
    houseTypeNormalized: '신혼부부',
    exclusiveAreaText: '44.12㎡',
    depositAmount: '72,000,000원',
    monthlyRentAmount: '310,000원',
    salePriceMin: '-',
    salePriceMax: '-',
    supplyHouseholdCount: '20세대',
    rawText: 'LH API 공급형 정보만 존재합니다. PDF AI 근거가 없으므로 보증금/월세 최신성을 확인하는 것이 좋습니다.',
    sourceUnitKey: 'LHAPI-44A-NEWLYWED',
  },
];

const UNIT_STYLE = `
.review-units-page{min-height:100vh;background:#f2f2f2;color:#222;font-family:'Noto Sans KR',-apple-system,system-ui,sans-serif}
.review-shell{max-width:1180px;margin:0 auto;padding:32px 24px 80px}
.unit-panel{background:#fff;border-radius:20px;box-shadow:rgba(0,0,0,.02) 0 0 0 1px,rgba(0,0,0,.04) 0 2px 6px,rgba(0,0,0,.1) 0 4px 8px;overflow:hidden}
.unit-header{padding:24px;border-bottom:1px solid rgba(0,0,0,.06);display:flex;justify-content:space-between;gap:18px;align-items:flex-start}
.unit-eyebrow{color:#ff385c;font-size:12px;font-weight:900;margin-bottom:6px}.unit-title{margin:0 0 8px;font-size:24px;letter-spacing:-.4px}.unit-desc{margin:0;color:#6a6a6a;font-size:14px;line-height:1.6}
.unit-badge{display:inline-flex;border-radius:999px;padding:5px 10px;font-size:11px;font-weight:900;white-space:nowrap;border:1px solid transparent}.unit-badge-high,.unit-badge-merged{background:#f0fdf4;color:#166534;border-color:rgba(22,101,52,.16)}.unit-badge-medium,.unit-badge-lh{background:#eff6ff;color:#1d4ed8;border-color:rgba(29,78,216,.14)}.unit-badge-low,.unit-badge-pdf{background:#fff7ed;color:#c2410c;border-color:rgba(194,65,12,.16)}.unit-badge-api{background:#f2f2f2;color:#6a6a6a;border-color:rgba(0,0,0,.05)}
.unit-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;padding:18px 24px;border-bottom:1px solid rgba(0,0,0,.06)}.unit-summary-item{border-radius:14px;background:#fafafa;padding:14px}.unit-summary-value{font-size:22px;font-weight:900}.unit-summary-label{margin-top:4px;color:#6a6a6a;font-size:11px;font-weight:800}
.unit-warning{margin:18px 24px 0;border-radius:14px;background:#fff7ed;color:#c2410c;padding:14px 16px;font-size:13px;font-weight:800;line-height:1.55}
.unit-table-wrap{overflow-x:auto}.unit-table{width:100%;min-width:1060px;border-collapse:collapse}.unit-table th{text-align:left;padding:12px 14px;background:#fafafa;border-bottom:1px solid rgba(0,0,0,.08);color:#6a6a6a;font-size:12px;font-weight:900}.unit-table td{padding:15px 14px;border-bottom:1px solid rgba(0,0,0,.06);vertical-align:top;font-size:13px;line-height:1.45}.unit-main-name{font-size:14px;font-weight:900;margin-bottom:4px}.unit-sub{color:#6a6a6a;font-size:12px}.unit-money{font-weight:900;white-space:nowrap}.unit-raw{margin-top:10px;max-width:520px;border-radius:12px;background:#f8f8f8;color:#6a6a6a;padding:12px;font-size:12px;line-height:1.6}.unit-raw-key{margin-top:8px;color:#6a6a6a;font-size:11px;font-weight:800}.unit-toggle{border:none;border-radius:8px;background:#f2f2f2;color:#222;padding:7px 10px;font-family:inherit;font-size:12px;font-weight:900;cursor:pointer}
.unit-cards{display:none;padding:16px;gap:12px;flex-direction:column}.unit-card{border:1px solid rgba(0,0,0,.08);border-radius:16px;background:#fff;padding:16px}.unit-card-head{display:flex;justify-content:space-between;gap:12px;align-items:flex-start;margin-bottom:12px}.unit-card-badges{display:flex;gap:6px;flex-wrap:wrap;margin:10px 0}.unit-grid2{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:12px}.unit-kv{border-radius:12px;background:#fafafa;padding:11px}.unit-k{font-size:11px;color:#6a6a6a;font-weight:800;margin-bottom:4px}.unit-v{font-size:13px;font-weight:800}
@media(max-width:760px){.review-shell{padding:20px 16px 56px}.unit-header{flex-direction:column}.unit-title{font-size:21px}.unit-summary{grid-template-columns:1fr 1fr;padding:16px}.unit-table-wrap{display:none}.unit-cards{display:flex}}
`;

const UnitBadge = ({ children, tone }) => <span className={`unit-badge unit-badge-${tone}`}>{children}</span>;
const sourceTone = (value) => value === 'MERGED' ? 'merged' : value === 'PDF_AI' ? 'pdf' : 'lh';
const confidenceTone = (value) => value === 'HIGH' ? 'high' : value === 'LOW' ? 'low' : 'medium';

const AdminReviewUnitsSection = ({ units = REVIEW_UNITS }) => {
  const [expanded, setExpanded] = React.useState([1]);
  const toggle = (order) => setExpanded((prev) => prev.includes(order) ? prev.filter((v) => v !== order) : [...prev, order]);
  const lowCount = units.filter((unit) => unit.confidenceLevel === 'LOW').length;

  return (
    <>
      <style>{UNIT_STYLE}</style>
      <section className="unit-panel">
        <div className="unit-header">
          <div>
            <div className="unit-eyebrow">ADMIN REVIEW DETAIL</div>
            <h2 className="unit-title">공급 단위 검수</h2>
            <p className="unit-desc">units[]는 관리자 검수 화면에만 노출합니다. rawText와 sourceUnitKey는 public 상세 화면에 노출하지 않는 전제의 디자인입니다.</p>
          </div>
          <UnitBadge tone={lowCount ? 'low' : 'high'}>{lowCount ? `LOW 신뢰도 ${lowCount}건 확인 필요` : '모든 단위 확인 가능'}</UnitBadge>
        </div>

        <div className="unit-summary">
          <div className="unit-summary-item"><div className="unit-summary-value">{units.length}</div><div className="unit-summary-label">공급 단위</div></div>
          <div className="unit-summary-item"><div className="unit-summary-value">{units.filter((u) => u.unitSource === 'MERGED').length}</div><div className="unit-summary-label">MERGED</div></div>
          <div className="unit-summary-item"><div className="unit-summary-value">{units.filter((u) => u.unitSource === 'PDF_AI').length}</div><div className="unit-summary-label">PDF AI</div></div>
          <div className="unit-summary-item"><div className="unit-summary-value">{lowCount}</div><div className="unit-summary-label">LOW 신뢰도</div></div>
        </div>

        {lowCount > 0 && <div className="unit-warning">LOW 신뢰도 행은 원문 PDF 표와 대조한 뒤 승인/보정하세요. AI 추출값임을 관리자에게 분명히 보여주는 패턴입니다.</div>}

        <div className="unit-table-wrap">
          <table className="unit-table">
            <thead>
              <tr><th>순서</th><th>출처/신뢰도</th><th>단지·주소</th><th>공급/주택 유형</th><th>면적</th><th>금액</th><th>공급세대</th><th>근거 원문</th></tr>
            </thead>
            <tbody>
              {units.map((unit) => {
                const open = expanded.includes(unit.unitOrder);
                return (
                  <tr key={unit.unitOrder}>
                    <td style={{ fontWeight:900 }}>{unit.unitOrder}</td>
                    <td>
                      <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
                        <UnitBadge tone={sourceTone(unit.unitSource)}>{unit.unitSource}</UnitBadge>
                        <UnitBadge tone={confidenceTone(unit.confidenceLevel)}>{unit.confidenceLevel}</UnitBadge>
                      </div>
                      <div className="unit-sub" style={{ marginTop:6 }}>{unit.matchSource}</div>
                    </td>
                    <td><div className="unit-main-name">{unit.complexName}</div><div className="unit-sub">{unit.fullAddress}</div></td>
                    <td>
                      <div><strong>{unit.supplyTypeNormalized}</strong> <span className="unit-sub">({unit.supplyTypeRaw})</span></div>
                      <div><strong>{unit.houseTypeNormalized}</strong> <span className="unit-sub">({unit.houseTypeRaw})</span></div>
                    </td>
                    <td>{unit.exclusiveAreaText}</td>
                    <td>
                      <div className="unit-money">보증금 {unit.depositAmount}</div>
                      <div className="unit-money">월세 {unit.monthlyRentAmount}</div>
                      {unit.salePriceMin !== '-' && <div className="unit-money">분양가 {unit.salePriceMin}~{unit.salePriceMax}</div>}
                    </td>
                    <td>{unit.supplyHouseholdCount}</td>
                    <td>
                      <button className="unit-toggle" onClick={() => toggle(unit.unitOrder)}>{open ? '원문 접기' : '원문 보기'}</button>
                      {open && <><div className="unit-raw">{unit.rawText}</div><div className="unit-raw-key">sourceUnitKey · {unit.sourceUnitKey}</div></>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className="unit-cards">
          {units.map((unit) => {
            const open = expanded.includes(unit.unitOrder);
            return (
              <article className="unit-card" key={unit.unitOrder}>
                <div className="unit-card-head">
                  <div><div className="unit-main-name">#{unit.unitOrder} {unit.complexName}</div><div className="unit-sub">{unit.fullAddress}</div></div>
                  <UnitBadge tone={confidenceTone(unit.confidenceLevel)}>{unit.confidenceLevel}</UnitBadge>
                </div>
                <div className="unit-card-badges">
                  <UnitBadge tone={sourceTone(unit.unitSource)}>{unit.unitSource}</UnitBadge>
                  <UnitBadge tone="api">{unit.matchSource}</UnitBadge>
                </div>
                <div className="unit-grid2">
                  <div className="unit-kv"><div className="unit-k">공급유형</div><div className="unit-v">{unit.supplyTypeNormalized}</div></div>
                  <div className="unit-kv"><div className="unit-k">주택유형</div><div className="unit-v">{unit.houseTypeNormalized}</div></div>
                  <div className="unit-kv"><div className="unit-k">전용면적</div><div className="unit-v">{unit.exclusiveAreaText}</div></div>
                  <div className="unit-kv"><div className="unit-k">공급세대</div><div className="unit-v">{unit.supplyHouseholdCount}</div></div>
                  <div className="unit-kv"><div className="unit-k">보증금</div><div className="unit-v">{unit.depositAmount}</div></div>
                  <div className="unit-kv"><div className="unit-k">월세</div><div className="unit-v">{unit.monthlyRentAmount}</div></div>
                </div>
                <div style={{ marginTop:12 }}>
                  <button className="unit-toggle" onClick={() => toggle(unit.unitOrder)}>{open ? '원문 접기' : '원문 보기'}</button>
                  {open && <><div className="unit-raw">{unit.rawText}</div><div className="unit-raw-key">sourceUnitKey · {unit.sourceUnitKey}</div></>}
                </div>
              </article>
            );
          })}
        </div>
      </section>
    </>
  );
};

const AdminReviewUnitsPage = () => (
  <div className="review-units-page">
    <style>{UNIT_STYLE}</style>
    <div className="review-shell">
      <div style={{ marginBottom:20 }}>
        <div className="unit-eyebrow">HANDOFF MOCKUP</div>
        <h1 className="unit-title" style={{ fontSize:28 }}>관리자 검수 상세 · 공급 단위 섹션</h1>
        <p className="unit-desc">AdminReviewDetailPage에 추가할 units[] 검수 UI 패턴입니다.</p>
      </div>
      <AdminReviewUnitsSection />
    </div>
  </div>
);

Object.assign(window, { AdminReviewUnitsSection, AdminReviewUnitsPage });
