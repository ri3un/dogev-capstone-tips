import { useState } from 'react'
import Icon from '../components/common/Icon'
import { card, chip, primaryBtn } from '../components/common/styles'
import { DOG } from '../mocks/dog'
import { REPORT_SECTIONS, REPORT_SUMMARY } from '../mocks/report'

type Period = '2w' | '1m' | 'custom'
const PERIODS: { key: Period; label: string }[] = [
  { key: '2w', label: '최근 2주' },
  { key: '1m', label: '최근 1개월' },
  { key: 'custom', label: '직접 선택' },
]
const today = new Date().toISOString().slice(0, 10)
const daysAgo = (n: number) => new Date(Date.now() - n * 86400000).toISOString().slice(0, 10)

// 건강 리포트: 기간을 고르면 PDF와 같은 순서(9개 섹션)로 미리 보여 줌
export default function ReportPage() {
  const [period, setPeriod] = useState<Period>('1m')
  const [from, setFrom] = useState(daysAgo(30))
  const [to, setTo] = useState(today)

  const pick = (p: Period) => {
    setPeriod(p)
    if (p !== 'custom') {
      setFrom(daysAgo(p === '2w' ? 14 : 30))
      setTo(today)
    }
  }
  // TODO: GET /api/dogs/{dogId}/reports/pdf?from&to
  const download = () => alert(`${from} ~ ${to} 리포트 PDF를 내려받습니다. (API 연결 전)`)
  const alerts = REPORT_SECTIONS.filter((s) => s.alert).length

  return (
    <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-5 py-9 lg:px-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="mb-1 text-label-md text-secondary">병원 갈 때 보여 주세요</p>
          <h1 className="text-headline-lg font-bold tracking-tight">{DOG.name}의 건강 리포트</h1>
        </div>
        <button type="button" onClick={download} className={primaryBtn}>
          <Icon name="picture_as_pdf" className="text-[20px]" />
          PDF 내려받기
        </button>
      </div>

      <section className={`${card} flex flex-wrap items-center gap-3 p-5`}>
        <span className="mr-1 text-label-md text-outline">기간</span>
        {PERIODS.map((p) => (
          <button key={p.key} type="button" onClick={() => pick(p.key)} className={chip(period === p.key)}>
            {p.label}
          </button>
        ))}
        <div className="ml-auto flex items-center gap-2 text-label-md">
          <input type="date" value={from} max={to} disabled={period !== 'custom'} onChange={(e) => setFrom(e.target.value)} className="rounded-lg border border-beige bg-surface-container-low px-3 py-2 disabled:opacity-60" />
          <span className="text-outline">~</span>
          <input type="date" value={to} min={from} max={today} disabled={period !== 'custom'} onChange={(e) => setTo(e.target.value)} className="rounded-lg border border-beige bg-surface-container-low px-3 py-2 disabled:opacity-60" />
        </div>
      </section>

      {/* ② 기간 요약 (LLM 3줄) */}
      <section className={`${card} flex flex-col gap-3 bg-gradient-to-br from-surface-container-lowest to-cream p-6`}>
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-headline-sm font-bold">
            <Icon name="auto_awesome" className="text-[22px] text-primary" />
            기간 요약
          </h2>
          {alerts > 0 && (
            <span className="flex items-center gap-1.5 rounded-full bg-error-container px-3 py-1 text-label-sm font-bold text-on-error-container">
              <span className="h-2 w-2 rounded-full bg-error" />
              살펴볼 신호 {alerts}개
            </span>
          )}
        </div>
        <ul className="flex flex-col gap-2">
          {REPORT_SUMMARY.map((line) => (
            <li key={line} className="flex gap-2 leading-relaxed text-on-surface-variant">
              <span className="mt-2.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary-container" />
              {line}
            </li>
          ))}
        </ul>
      </section>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {REPORT_SECTIONS.map((s) => (
          <section key={s.key} className={`${card} flex flex-col gap-3 p-5 ${s.key === 'basic' || s.key === 'questions' ? 'md:col-span-2' : ''}`}>
            <h2 className="flex items-center gap-2 text-body-lg font-bold">
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-secondary-container/60">
                <Icon name={s.icon} className="text-[18px] text-primary" />
              </span>
              {s.title}
              {s.alert && <span className="h-2.5 w-2.5 rounded-full bg-error" aria-label="이상 신호" />}
            </h2>
            {s.alert && (
              <p className="flex items-center gap-2 rounded-xl bg-error-container/60 px-3 py-2 text-label-md text-on-error-container">
                <Icon name="warning" className="text-[18px]" />
                {s.alert}
              </p>
            )}
            {s.empty ? (
              <p className="rounded-xl bg-surface-container-low px-4 py-6 text-center text-label-md text-outline">이 기간에 기록이 없어요</p>
            ) : (
              <dl className="flex flex-col divide-y divide-outline-variant/30">
                {s.rows.map((r) => (
                  <div key={r.label} className="flex gap-4 py-2">
                    <dt className="w-24 shrink-0 text-label-md text-outline">{s.key === 'questions' ? `Q${r.label}` : r.label}</dt>
                    <dd className="flex-1 text-body-md">{r.value}</dd>
                  </div>
                ))}
              </dl>
            )}
          </section>
        ))}
      </div>

      <p className="flex items-center justify-center gap-1.5 text-label-sm text-outline">
        <Icon name="info" className="text-[16px]" />
        참고용 기록이며 진단이 아닙니다.
      </p>
    </div>
  )
}
