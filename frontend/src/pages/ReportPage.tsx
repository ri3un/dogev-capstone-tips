import { useState } from 'react'
import { Navigate } from 'react-router'
import { reportApi } from '../api/endpoints'
import { addDays, todayStr } from '../api/format'
import type { ReportData, ReportRow } from '../api/types'
import Icon from '../components/common/Icon'
import { card, chip, primaryBtn } from '../components/common/styles'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'

type Period = '2w' | '1m' | 'custom'
const PERIODS: { key: Period; label: string }[] = [
  { key: '2w', label: '최근 2주' },
  { key: '1m', label: '최근 1개월' },
  { key: 'custom', label: '직접 선택' },
]
const daysAgo = (n: number) => addDays(todayStr(), -n)
const md = (date: string) => date.slice(5).replace('-', '/') // "2026-10-05" → "10/05"
const MAX_ROWS = 6 // 화면에는 최근 6건만, 전체는 PDF에

interface Section {
  key: string
  title: string
  icon: string
  alert?: string
  rows: { label: string; value: string }[]
}

// 기록 줄: 최근 것부터 MAX_ROWS 개, 넘치면 "외 N건"
const recordRows = (rows: ReportRow[]) => {
  const shown = rows.slice(-MAX_ROWS).map((r) => ({ label: md(r.date), value: [r.category, r.memo].filter(Boolean).join(' · ') }))
  return rows.length > MAX_ROWS ? [...shown, { label: '외', value: `${rows.length - MAX_ROWS}건 더 (PDF에 전부 있어요)` }] : shown
}

// 이상 신호 문장을 섹션에 붙임 (서버 ReportSignals: 체중 변화·무른 변 연속·증상)
const STOOL_WORDS = ['변', '설사']
const signalFor = (signals: string[], words: string[]) => signals.filter((s) => words.some((w) => s.includes(w))).join(' / ') || undefined

// 서버 리포트 → PDF와 같은 순서의 섹션 8개 (요약은 위에 따로)
function sectionsOf(R: ReportData): Section[] {
  const b = R.basic
  const other = R.signals.filter((s) => ![...STOOL_WORDS, '체중'].some((w) => s.includes(w)))
  return [
    {
      key: 'basic', title: '기본 정보', icon: 'pets',
      rows: [
        { label: '이름', value: `${b.name} (${b.breed}, ${b.sex}${b.neutered ? ', 중성화' : ''})` },
        { label: '나이·체중', value: `${b.age} · ${b.weightKg}kg (${b.size}견)` },
        { label: '지병', value: b.diseases.join(', ') || '없음' },
        { label: '알레르기', value: b.allergies.join(', ') || '없음' },
      ],
    },
    { key: 'meal', title: '식사·간식', icon: 'restaurant', rows: recordRows(R.meals) },
    {
      key: 'poop', title: '배변', icon: 'eco', alert: signalFor(R.signals, STOOL_WORDS),
      rows: [...Object.entries(R.stoolCounts).map(([k, n]) => ({ label: k, value: `${n}회` })), ...recordRows(R.stools)],
    },
    { key: 'symptom', title: '증상·이상', icon: 'monitor_heart', alert: other.join(' / ') || undefined, rows: recordRows(R.symptoms) },
    {
      key: 'activity', title: '활동·산책', icon: 'directions_walk',
      rows: R.activities.length ? [{ label: '산책 합계', value: `${R.walkMinutesTotal}분` }, ...recordRows(R.activities)] : [],
    },
    { key: 'weight', title: '체중', icon: 'monitor_weight', alert: signalFor(R.signals, ['체중']), rows: R.weights.map((w) => ({ label: md(w.date), value: `${w.weightKg}kg` })) },
    { key: 'medical', title: '투약·접종·병원', icon: 'vaccines', rows: recordRows(R.medical) },
    { key: 'questions', title: '수의사에게 물어볼 것', icon: 'help', rows: R.questions.map((q, i) => ({ label: String(i + 1), value: q.content })) },
  ]
}

// 건강 리포트: 기간을 고르면 PDF와 같은 순서(9개 섹션)로 미리 보여 줌
export default function ReportPage() {
  const [period, setPeriod] = useState<Period>('1m')
  const [from, setFrom] = useState(() => daysAgo(30))
  const [to, setTo] = useState(todayStr)
  const [downloading, setDownloading] = useState(false)
  const today = todayStr()

  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const { data: R } = useApi(() => (dogId && from <= to ? reportApi.preview(dogId, { from, to }) : null), [dogId, from, to])

  if (loading) return null
  if (!dog) return <Navigate to="/onboarding" replace />

  const pick = (p: Period) => {
    setPeriod(p)
    if (p !== 'custom') {
      setFrom(daysAgo(p === '2w' ? 14 : 30))
      setTo(todayStr())
    }
  }

  const download = async () => {
    setDownloading(true)
    try {
      const blob = await reportApi.pdf(dog.id, { from, to })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${dog.name}_건강리포트_${from}_${to}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      alert((e as { message?: string }).message ?? 'PDF를 만들지 못했어요')
    } finally {
      setDownloading(false)
    }
  }
  const sections = R ? sectionsOf(R) : []
  const alerts = R?.signals.length ?? 0

  return (
    <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-5 py-9 lg:px-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="mb-1 text-label-md text-secondary">병원 갈 때 보여 주세요</p>
          <h1 className="text-headline-lg font-bold tracking-tight">{dog.name}의 건강 리포트</h1>
        </div>
        <button type="button" onClick={download} disabled={downloading || from > to} className={primaryBtn}>
          <Icon name="picture_as_pdf" className="text-[20px]" />
          {downloading ? 'PDF 만드는 중…' : 'PDF 내려받기'}
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
          {!R && <li className="text-label-md text-outline">기록을 모아 요약하는 중이에요… (최대 20초)</li>}
          {R?.summary.map((line) => (
            <li key={line} className="flex gap-2 leading-relaxed text-on-surface-variant">
              <span className="mt-2.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary-container" />
              {line}
            </li>
          ))}
        </ul>
      </section>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {sections.map((s) => (
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
            {s.rows.length === 0 ? (
              <p className="rounded-xl bg-surface-container-low px-4 py-6 text-center text-label-md text-outline">이 기간에 기록이 없어요</p>
            ) : (
              <dl className="flex flex-col divide-y divide-outline-variant/30">
                {s.rows.map((r, i) => (
                  <div key={i} className="flex gap-4 py-2">
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
