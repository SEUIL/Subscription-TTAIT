import { useState } from 'react';
import { importLh, createAnnouncement } from '../../api/admin';

export default function AdminImportPage() {
  const [lhPage, setLhPage] = useState('1');
  const [lhSize, setLhSize] = useState('10');
  const [importing, setImporting] = useState(false);
  const [importMsg, setImportMsg] = useState('');

  const [form, setForm] = useState({ title: '', provider: '', supplyType: '', houseType: '', regionLevel1: '', regionLevel2: '', recruitmentStartDate: '', recruitmentEndDate: '', deposit: '', monthlyRent: '' });
  const [creating, setCreating] = useState(false);
  const [createMsg, setCreateMsg] = useState('');

  const handleImport = async () => {
    setImporting(true);
    setImportMsg('');
    try {
      await importLh(Number(lhPage), Number(lhSize));
      setImportMsg('임포트 완료!');
    } catch {
      setImportMsg('임포트 실패. 다시 시도해주세요.');
    } finally {
      setImporting(false);
    }
  };

  const handleCreate = async () => {
    if (!form.title || !form.provider || !form.supplyType || !form.regionLevel1) {
      setCreateMsg('필수 항목을 모두 입력해주세요.');
      return;
    }
    setCreating(true);
    setCreateMsg('');
    try {
      await createAnnouncement({
        ...form,
        deposit: form.deposit ? Number(form.deposit) : undefined,
        monthlyRent: form.monthlyRent ? Number(form.monthlyRent) : undefined,
      });
      setCreateMsg('공고가 등록됐습니다.');
      setForm({ title: '', provider: '', supplyType: '', houseType: '', regionLevel1: '', regionLevel2: '', recruitmentStartDate: '', recruitmentEndDate: '', deposit: '', monthlyRent: '' });
    } catch {
      setCreateMsg('등록에 실패했습니다.');
    } finally {
      setCreating(false);
    }
  };

  const inputStyle = { width: '100%', height: 44, padding: '0 14px', border: '1.5px solid rgba(0,0,0,0.12)', borderRadius: 10, fontFamily: "'Noto Sans KR',sans-serif", fontSize: 14, color: '#222', background: '#fff', outline: 'none', boxSizing: 'border-box' } as React.CSSProperties;
  const labelStyle = { display: 'block', fontSize: 12, fontWeight: 600, color: '#6a6a6a', marginBottom: 6 } as React.CSSProperties;
  const cardStyle = { background: '#fff', borderRadius: 16, padding: '24px', marginBottom: 24, boxShadow: 'rgba(0,0,0,0.04) 0px 2px 8px' } as React.CSSProperties;

  return (
    <div>
      <h1 style={{ fontSize: 22, fontWeight: 700, color: '#222', marginBottom: 24 }}>공고 관리</h1>

      {/* LH 임포트 */}
      <div style={cardStyle}>
        <h2 style={{ fontSize: 16, fontWeight: 700, color: '#222', marginBottom: 16 }}>LH 공고 임포트</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
          <div>
            <label style={labelStyle}>페이지 번호</label>
            <input style={inputStyle} type="number" min="1" value={lhPage} onChange={(e) => setLhPage(e.target.value)} />
          </div>
          <div>
            <label style={labelStyle}>건수 (size)</label>
            <input style={inputStyle} type="number" min="1" max="50" value={lhSize} onChange={(e) => setLhSize(e.target.value)} />
          </div>
        </div>
        {importMsg && <p style={{ fontSize: 13, color: importMsg.includes('실패') ? '#ff385c' : '#166534', marginBottom: 12, fontWeight: 600 }}>{importMsg}</p>}
        <button
          style={{ padding: '12px 24px', background: '#222', color: '#fff', border: 'none', borderRadius: 10, fontSize: 14, fontWeight: 600, cursor: 'pointer', opacity: importing ? 0.7 : 1 }}
          disabled={importing}
          onClick={handleImport}
        >
          {importing ? '임포트 중...' : 'LH 공고 가져오기'}
        </button>
      </div>

      {/* 수동 등록 */}
      <div style={cardStyle}>
        <h2 style={{ fontSize: 16, fontWeight: 700, color: '#222', marginBottom: 16 }}>공고 수동 등록</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
          {([
            ['제목 *', 'title', 'text'],
            ['공급기관 *', 'provider', 'text'],
            ['공급유형 *', 'supplyType', 'text'],
            ['주택유형', 'houseType', 'text'],
            ['시/도 *', 'regionLevel1', 'text'],
            ['시/군/구', 'regionLevel2', 'text'],
            ['모집 시작일 *', 'recruitmentStartDate', 'date'],
            ['모집 마감일 *', 'recruitmentEndDate', 'date'],
            ['보증금 (원)', 'deposit', 'number'],
            ['월세 (원)', 'monthlyRent', 'number'],
          ] as const).map(([l, k, t]) => (
            <div key={k}>
              <label style={labelStyle}>{l}</label>
              <input
                style={inputStyle}
                type={t}
                placeholder={l.replace(' *', '')}
                value={form[k]}
                onChange={(e) => setForm((f) => ({ ...f, [k]: e.target.value }))}
              />
            </div>
          ))}
        </div>
        {createMsg && <p style={{ fontSize: 13, color: createMsg.includes('실패') || createMsg.includes('입력') ? '#ff385c' : '#166534', marginBottom: 12, fontWeight: 600 }}>{createMsg}</p>}
        <button
          style={{ padding: '12px 24px', background: '#ff385c', color: '#fff', border: 'none', borderRadius: 10, fontSize: 14, fontWeight: 600, cursor: 'pointer', opacity: creating ? 0.7 : 1 }}
          disabled={creating}
          onMouseEnter={(e) => (e.currentTarget.style.background = '#e00b41')}
          onMouseLeave={(e) => (e.currentTarget.style.background = '#ff385c')}
          onClick={handleCreate}
        >
          {creating ? '등록 중...' : '공고 등록'}
        </button>
      </div>
    </div>
  );
}
