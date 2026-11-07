import { useState } from 'react'
import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import { DOG } from '../mocks/dog'
import { KIND_BADGE, KIND_DOT, KIND_LABEL, MONTH_STAT, RECORDS, WEIGHT, dayKinds, type RecordKind } from '../mocks/records'

const KINDS = Object.keys(KIND_LABEL) as RecordKind[]
const WEEK = ['일', '월', '화', '수', '목', '금', '토']
const card = 'rounded-xl bg-surface-container-lowest shadow-[0_4px_20px_-2px_rgba(74,52,38,0.05)]'
const TABS = [
  { to: '/records', icon: 'calendar_month', label: '캘린더' },
  { to: '/nutrition', icon: 'restaurant', label: '영양 가이드' },
  { to: '/report', icon: 'description', label: '병원용 리포트' },
]

const pad = (n: number) => String(n).padStart(2, '0')
const ymd = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

// 달력 칸: 앞달 끝 날짜 + 이번 달 날짜 (일요일 시작)
function monthCells(month: Date) {
  const y = month.getFullYear()
  const m = month.getMonth()
  const lead = new Date(y, m, 1).getDay()
  const days = new Date(y, m + 1, 0).getDate()
  return Array.from({ length: lead + days }, (_, i) => {
    const d = new Date(y, m, i - lead + 1)
    return { date: d, inMonth: i >= lead }
  })
}

// 건강 기록: 월 캘린더 + 일별 상세 (Stitch 06-records)
export default function RecordsPage() {
  const [month, setMonth] = useState(() => new Date(2026, 9)) // TODO: 오늘 날짜로
  const [selected, setSelected] = useState<string | null>('2026-10-18')

  const moveMonth = (d: number) => setMonth((m) => new Date(m.getFullYear(), m.getMonth() + d))
  const goToday = () => {
    const t = new Date()
    setMonth(new Date(t.getFullYear(), t.getMonth()))
    setSelected(ymd(t))
  }
  const dayRecords = RECORDS.filter((r) => r.date === selected)
  const selectedDate = selected ? new Date(selected) : null

  return (
    <div className="relative mx-auto w-full max-w-[1440px] px-10 py-6">
      <div className="pointer-events-none absolute -top-12 right-20 -z-10 h-96 w-96 rounded-full bg-primary-fixed/25 blur-3xl" />
      <div className="pointer-events-none absolute top-48 left-12 -z-10 h-80 w-80 rounded-full bg-secondary-container/20 blur-3xl" />

      {/* 제목 + 탭 */}
      <div className="mb-6 flex flex-col justify-between gap-4 md:flex-row md:items-end">
        <div className="flex flex-col gap-1">
          <div className="flex items-center gap-1 text-label-sm">
            <span className="inline-flex items-center rounded-full bg-surface-container-high px-2.5 py-0.5 tracking-wider text-primary uppercase">Health &amp; Wellness Log</span>
            <span className="h-1.5 w-1.5 rounded-full bg-primary/40" />
            <span className="text-outline">실시간 가족 동기화</span>
          </div>
          <div className="mt-1 flex items-baseline gap-3">
            <h1 className="text-headline-xl tracking-tight">{DOG.name}의 건강 기록</h1>
            <p className="hidden text-on-surface-variant sm:inline-block">가족과 함께 소중한 일상을 기록하고 건강 변화를 살펴봐요</p>
          </div>
        </div>
        <div className="inline-flex rounded-full bg-surface-container-low p-1 shadow-sm">
          {TABS.map((t, i) => (
            <Link
              key={t.to}
              to={t.to}
              aria-current={i === 0 ? 'page' : undefined}
              className={`flex items-center gap-1.5 rounded-full px-5 py-2 text-label-lg transition-all ${i === 0 ? 'bg-primary-container text-on-primary shadow-sm' : 'text-on-surface-variant hover:bg-surface-container-high/60 hover:text-on-surface'}`}
            >
              <Icon name={t.icon} className="text-[18px]" />
              {t.label}
            </Link>
          ))}
        </div>
      </div>

      {/* 체중 카드 */}
      <div className={`${card} mb-6 p-5 transition-all hover:shadow-[0_8px_28px_-4px_rgba(74,52,38,0.08)]`}>
        <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
          <div className="flex min-w-0 items-center gap-4">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-secondary-container/40">
              <Icon name="monitor_weight" className="text-[26px] text-primary" />
            </div>
            <div>
              <div className="flex items-center gap-2.5">
                <span className="text-label-md text-outline">최근 측정한 체중</span>
                <span className="inline-flex items-center gap-1 rounded-full bg-tertiary-fixed px-2 py-0.5 text-label-sm text-on-tertiary-fixed-variant">
                  <Icon name="trending_up" className="text-[13px]" />
                  지난달 대비 {WEIGHT.diff}
                </span>
              </div>
              <div className="mt-0.5 flex items-baseline gap-2">
                <span className="text-headline-lg font-bold">{WEIGHT.current}</span>
                <span className="text-headline-sm text-outline">kg</span>
                <span className="ml-2 text-body-sm text-outline">{WEIGHT.note}</span>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-6 lg:justify-end">
            <div className="flex flex-col items-end">
              <span className="mb-1 text-label-sm text-outline">최근 5개월 추이</span>
              {/* TODO: 체중 기록 값으로 좌표 계산 */}
              <svg className="h-11 w-64 overflow-visible" fill="none" viewBox="0 0 240 44">
                <line className="text-surface-container-high" stroke="currentColor" strokeDasharray="3 3" strokeWidth="1.5" x1="0" x2="240" y1="26" y2="26" />
                <path className="text-primary-container" d="M 10 32 C 40 32, 50 30, 70 29 C 95 28, 110 26, 130 26 C 150 26, 170 20, 190 19 C 205 18, 220 16, 230 15" stroke="currentColor" strokeLinecap="round" strokeWidth="2.5" />
                <path className="text-primary-container/10" d="M 10 32 C 40 32, 50 30, 70 29 C 95 28, 110 26, 130 26 C 150 26, 170 20, 190 19 C 205 18, 220 16, 230 15 L 230 42 L 10 42 Z" fill="currentColor" />
                {[
                  [10, 32],
                  [70, 29],
                  [130, 26],
                  [190, 19],
                ].map(([cx, cy]) => (
                  <circle key={cx} className="fill-surface-container-lowest stroke-primary-container stroke-2" cx={cx} cy={cy} r="3" />
                ))}
                <circle className="fill-primary-container" cx="230" cy="15" r="4.5" />
                <circle className="fill-none stroke-primary-container/40 stroke-[1.5]" cx="230" cy="15" r="8" />
              </svg>
              <div className="mt-1 flex w-64 justify-between px-1 text-[10px] text-outline">
                {WEIGHT.labels.map((l, i) => (
                  <span key={l} className={i === WEIGHT.labels.length - 1 ? 'font-semibold text-primary' : ''}>{l}</span>
                ))}
              </div>
            </div>
            <Link to="/records/new?kind=weight" className="hidden items-center gap-1 rounded-full bg-surface-container-low px-3 py-1.5 text-label-md text-primary transition-colors hover:bg-surface-container-high xl:flex">
              기록
              <Icon name="chevron_right" className="text-[16px]" />
            </Link>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* 월 캘린더 */}
        <div className={`${card} p-4 sm:p-6 lg:col-span-8`}>
          <div className="flex flex-col justify-between gap-2 pb-4 sm:flex-row sm:items-center">
            <div className="flex items-center gap-3">
              <h2 className="text-headline-md font-bold">
                {month.getFullYear()}년 {month.getMonth() + 1}월
              </h2>
              <div className="flex items-center gap-1">
                {[
                  ['chevron_left', '이전 달', -1],
                  ['chevron_right', '다음 달', 1],
                ].map(([icon, label, d]) => (
                  <button key={icon} type="button" aria-label={String(label)} onClick={() => moveMonth(Number(d))} className="flex h-8 w-8 items-center justify-center rounded-full text-outline transition-colors hover:bg-surface-container-low hover:text-on-surface">
                    <Icon name={String(icon)} className="text-[20px]" />
                  </button>
                ))}
              </div>
              <button type="button" onClick={goToday} className="rounded-full bg-surface-container-low px-2.5 py-1 text-label-sm text-primary transition-colors hover:bg-surface-container-high">
                오늘
              </button>
            </div>
            <div className="flex flex-wrap items-center gap-2.5">
              {KINDS.map((k) => (
                <div key={k} className="flex items-center gap-1.5 text-label-sm text-on-surface-variant">
                  <span className={`h-2.5 w-2.5 rounded-full ${KIND_DOT[k]}`} />
                  {KIND_LABEL[k]}
                </div>
              ))}
            </div>
          </div>

          <div className="mb-2 grid grid-cols-7 gap-2 text-center text-label-md">
            {WEEK.map((w, i) => (
              <div key={w} className={`py-1 ${i === 0 ? 'text-error/80' : i === 6 ? 'text-on-surface-variant' : 'text-outline'}`}>{w}</div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-2">
            {monthCells(month).map(({ date, inMonth }) => {
              const key = ymd(date)
              const kinds = inMonth ? dayKinds(key) : []
              const isSel = key === selected
              const base = 'flex min-h-[72px] flex-col justify-between rounded-lg p-2 sm:min-h-[82px]'
              if (!inMonth)
                return (
                  <div key={key} className={`${base} cursor-default bg-surface-container-low/40 opacity-40`}>
                    <span className="text-label-md text-outline">{date.getDate()}</span>
                  </div>
                )
              return (
                <button
                  key={key}
                  type="button"
                  onClick={() => setSelected(key)}
                  className={`${base} text-left transition-all ${isSel ? 'relative scale-[1.02] overflow-hidden bg-surface-container-high shadow-md shadow-primary/10' : 'bg-surface hover:bg-surface-container-low'}`}
                >
                  <div className="flex items-center justify-between">
                    <span className={isSel ? 'text-label-lg font-bold text-primary' : `text-label-md font-medium ${date.getDay() === 0 ? 'text-error' : ''}`}>{date.getDate()}</span>
                    {isSel && <span className="h-2 w-2 animate-pulse rounded-full bg-primary" />}
                  </div>
                  <div className="flex flex-wrap items-center gap-1">
                    {kinds.map((k) => (
                      <span key={k} title={KIND_LABEL[k]} className={`rounded-full ${KIND_DOT[k]} ${isSel ? 'h-2 w-2 shadow-sm' : 'h-1.5 w-1.5'}`} />
                    ))}
                  </div>
                </button>
              )
            })}
          </div>

          <div className="mt-4 flex items-center justify-between gap-4 rounded-xl bg-surface-container-low p-4">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-container/20">
                <Icon name="insights" className="text-[20px] text-primary" />
              </div>
              <div>
                <p className="text-label-md font-semibold">
                  {month.getMonth() + 1}월 기록 달성률 {MONTH_STAT.rate}%
                </p>
                <p className="text-body-sm text-outline">총 {MONTH_STAT.days}일간 꾸준히 기록되어 이상 징후 파악이 원활해요.</p>
              </div>
            </div>
            <span className="hidden text-label-sm font-semibold text-primary sm:inline-block">우수 안심 지수 ✨</span>
          </div>
        </div>

        {/* 선택한 날의 기록 */}
        {selectedDate && (
          <aside className={`${card} flex flex-col justify-between gap-4 p-4 sm:p-6 lg:col-span-4`}>
            <div className="flex flex-col gap-4">
              <div className="flex items-start justify-between">
                <div className="flex flex-col">
                  <div className="flex items-center gap-1.5 text-label-sm text-primary">
                    <Icon name="event" className="text-[15px]" />
                    선택된 날짜
                  </div>
                  <h3 className="mt-0.5 text-headline-md font-bold">
                    {selectedDate.getMonth() + 1}월 {selectedDate.getDate()}일 {WEEK[selectedDate.getDay()]}요일
                  </h3>
                </div>
                <button type="button" aria-label="닫기" onClick={() => setSelected(null)} className="flex h-8 w-8 items-center justify-center rounded-full text-outline transition-colors hover:bg-surface-container-low hover:text-on-surface">
                  <Icon name="close" className="text-[20px]" />
                </button>
              </div>

              <div className="flex items-center justify-between rounded-lg bg-surface-container-low px-3.5 py-2.5">
                <div className="flex items-center gap-2">
                  <span className="inline-flex h-2 w-2 rounded-full bg-tertiary" />
                  <span className="text-label-md font-semibold">총 기록 {dayRecords.length}건</span>
                </div>
                {dayRecords.length > 0 && <span className="rounded-full bg-tertiary-fixed px-2 py-0.5 text-label-sm font-medium text-tertiary">{DOG.name} 컨디션 좋음 🐶</span>}
              </div>

              <div className="flex flex-col gap-3">
                {dayRecords.map((r) => (
                  <div key={r.id} className="flex flex-col gap-2 rounded-xl bg-surface p-3.5 transition-all hover:bg-surface-container-low/70">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-label-sm font-semibold ${KIND_BADGE[r.kind]}`}>
                          <span className={`h-1.5 w-1.5 rounded-full ${KIND_DOT[r.kind]}`} />
                          {KIND_LABEL[r.kind]}
                        </span>
                        <span className="text-label-sm text-outline">{r.time}</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-label-sm">
                        <span className="flex h-5 w-5 items-center justify-center rounded-full bg-secondary-container text-[9px] font-bold text-on-secondary-container shadow-sm">{r.author[0]}</span>
                        <span className="font-medium text-on-surface-variant">{r.author}</span>
                      </div>
                    </div>
                    <p className="pl-0.5 font-semibold">{r.title}</p>
                    <p className="pl-0.5 text-body-sm text-outline">{r.memo}</p>
                  </div>
                ))}
                {dayRecords.length === 0 && <p className="py-6 text-center text-body-sm text-outline">이 날의 상세 기록이 없어요.</p>}
              </div>
            </div>
            <Link to={`/records/new?date=${selected}`} className="flex w-full items-center justify-center gap-2 rounded-xl bg-surface-container-low px-4 py-3 text-label-lg text-primary transition-colors hover:bg-surface-container-high">
              <Icon name="add_notes" className="text-[18px]" />+ 이 날짜에 메모 추가
            </Link>
          </aside>
        )}
      </div>

      <Link to="/records/new" className="fixed right-8 bottom-8 z-40 flex items-center gap-2.5 rounded-full bg-primary-container px-6 py-3.5 text-label-lg text-on-primary shadow-[0_12px_28px_-6px_rgba(200,134,75,0.45)] transition-all hover:scale-105 hover:bg-primary active:scale-95">
        <Icon name="add" filled className="text-[20px]" />
        <span className="tracking-tight">기록 추가</span>
        <Icon name="pets" className="text-[18px] opacity-80" />
      </Link>
    </div>
  )
}
