import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { consultApi, dogApi, familyApi, placeApi, recordApi } from '../api/endpoints'
import { addDays, ageText, formatDistance, localDate, SIZE_LABEL, todayStr } from '../api/format'
import type { Dog, RecordSummary } from '../api/types'
import Icon from '../components/common/Icon'
import Photo from '../components/common/Photo'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'

type Trend = 'good' | 'watch' | 'keep'

// 오늘의 맞춤 안내: 코스 추천과 같은 기준(노령견·관절이면 2km 이내)
function tipOf(dog: Dog) {
  if (dog.ageMonths >= 96) return { before: '노령견이라 오늘은 ', strong: '2km 이내', after: ' 산책을 추천해요 🐾' }
  if (dog.diseases.includes('JOINT')) return { before: '관절 지병이 있어 오늘은 ', strong: '2km 이내', after: ' 산책을 추천해요 🐾' }
  if (dog.activityLevel === 'HIGH') return { before: '활동량이 많아 오늘은 ', strong: '뛰어놀 수 있는 곳', after: '을 넣어 볼까요 🐾' }
  return { before: '오늘도 ', strong: '가볍게 산책', after: '하고 기록을 남겨 보세요 🐾' }
}

// 이번 달 요약: 숫자는 서버 집계, 문장은 규칙으로 만듭니다(LLM 없음)
function monthSummary(s: RecordSummary | undefined) {
  if (!s) return { text: '', items: [] as { label: string; status: string; trend: Trend }[] }
  const total = Object.values(s.byCategory).reduce((a, b) => a + (b ?? 0), 0)
  if (total === 0) return { text: '이번 달 기록이 아직 없어요. 오늘 있었던 일을 아래에 적어 보세요.', items: [] }
  const text = `이번 달 기록 ${total}개, 산책·활동 총 ${s.walkMinutesTotal}분이에요.` + (s.looseStoolCount ? ` 무른 변·설사가 ${s.looseStoolCount}번 있었어요.` : ' 배변은 안정적이에요.')
  return {
    text,
    items: [
      { label: '배변', status: s.looseStoolCount ? '지켜보기' : '좋아요', trend: (s.looseStoolCount ? 'watch' : 'good') as Trend },
      { label: '활동', status: s.walkMinutesTotal ? `${s.walkMinutesTotal}분` : '기록 없음', trend: (s.walkMinutesTotal ? 'good' : 'watch') as Trend },
      { label: '증상', status: s.byCategory.SYMPTOM ? `${s.byCategory.SYMPTOM}회` : '없음', trend: (s.byCategory.SYMPTOM ? 'watch' : 'keep') as Trend },
    ],
  }
}

const card = 'rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-sm'
const primaryBtn =
  'inline-flex h-[52px] items-center justify-center gap-2 rounded-full bg-primary-container text-label-lg text-on-primary shadow-sm transition-all hover:scale-[1.02] hover:bg-primary active:scale-[0.98]'
const linkArrow = 'inline-flex items-center gap-0.5 text-label-md font-semibold text-primary transition-colors hover:text-on-primary-fixed-variant'

// 건강 상태 칩 색상·아이콘
const TREND_STYLE: Record<Trend, { icon: string; className: string }> = {
  good: { icon: 'check_circle', className: 'bg-tertiary-fixed text-on-tertiary-fixed' },
  watch: { icon: 'visibility', className: 'bg-secondary-fixed text-on-secondary-fixed-variant' },
  keep: { icon: 'horizontal_rule', className: 'bg-tertiary-fixed text-on-tertiary-fixed' },
}

// 홈 대시보드 (Stitch 01-home)
export default function HomePage() {
  const navigate = useNavigate()
  const [month, setMonth] = useState(() => new Date())
  const [memo, setMemo] = useState('')
  const moveMonth = (d: number) => setMonth((m) => new Date(m.getFullYear(), m.getMonth() + d))

  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const from = localDate(new Date(month.getFullYear(), month.getMonth(), 1))
  const to = localDate(new Date(month.getFullYear(), month.getMonth() + 1, 0))
  const { data: nutrition } = useApi(() => (dogId ? dogApi.nutrition(dogId) : null), [dogId])
  const { data: family } = useApi(() => familyApi.get(), [])
  const { data: recent } = useApi(() => (dogId ? recordApi.list(dogId, { from: addDays(todayStr(), -6), to: todayStr() }) : null), [dogId])
  const { data: summary } = useApi(() => (dogId ? recordApi.summary(dogId, { from, to }) : null), [dogId, from])
  const { data: consults } = useApi(() => (dogId ? consultApi.list(dogId) : null), [dogId])
  const { data: places } = useApi(() => (dogId ? placeApi.list({ dogId }) : null), [dogId])

  if (loading) return null
  if (!dog) return <Navigate to="/onboarding" replace />

  const last = recent?.at(-1)
  const DOG = {
    name: dog.name, breed: dog.breed, size: SIZE_LABEL[dog.size], age: ageText(dog.ageMonths), weightKg: dog.weightKg,
    photoUrl: dog.photoUrl ?? undefined,
    stats: [
      { icon: 'restaurant', label: '오늘 사료 권장량', value: nutrition?.foodGrams ? `${nutrition.foodGrams}g` : nutrition ? `${nutrition.derKcal}kcal` : '-' },
      { icon: 'water_drop', label: '물 권장량', value: nutrition ? `${nutrition.waterMinMl}~${nutrition.waterMaxMl}ml` : '-' },
      { icon: 'directions_walk', label: '최근 기록', value: last ? `${last.recordDate.slice(5)} ${last.memo ?? last.categoryLabel}` : '기록 없음' },
    ],
    family: (family?.members ?? []).map((m) => m.nickname.slice(0, 2)),
    tip: tipOf(dog),
  }
  const MONTHLY_SUMMARY = {
    ...monthSummary(summary),
    consultCount: (consults ?? []).filter((c) => c.createdAt.slice(0, 10) >= from && c.createdAt.slice(0, 10) <= to).length,
    recordCount: Object.values(summary?.byCategory ?? {}).reduce((a, b) => a + (b ?? 0), 0),
  }
  const nearby = (places ?? []).filter((p) => p.verdict?.level === 'OK').slice(0, 3)

  return (
    <div className="mx-auto flex w-full max-w-[1180px] flex-col gap-9 px-10 py-9">
      {/* 1. 반려견 카드 + 오늘의 맞춤 안내 (2:1) */}
      <section className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-12">
        <div className={`${card} flex flex-col justify-between p-8 lg:col-span-8`}>
          <div className="flex flex-col gap-6 sm:flex-row sm:items-center">
            <Photo src={DOG.photoUrl} alt={`${DOG.name} 프로필 사진`} className="h-24 w-24 shrink-0 rounded-full shadow-md ring-4 ring-secondary-fixed/50 sm:h-28 sm:w-28" />
            <div className="flex flex-col gap-2">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-headline-lg font-bold tracking-tight">{DOG.name}</h1>
                <span className="rounded-full bg-secondary-fixed px-2.5 py-0.5 text-label-md font-medium text-on-secondary-fixed-variant">{DOG.breed}</span>
                <span className="rounded-full bg-tertiary-fixed px-2.5 py-0.5 text-label-md text-on-tertiary-fixed">{DOG.size}</span>
              </div>
              <p className="font-medium text-on-surface-variant">
                {DOG.age} · {DOG.weightKg}kg
              </p>
            </div>
          </div>

          <div className="my-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
            {DOG.stats.map((s) => (
              <div key={s.label} className="flex items-center gap-2.5 rounded-lg bg-surface-container-low p-3">
                <Icon name={s.icon} className="text-[20px] text-primary" />
                <div className="flex flex-col">
                  <span className="text-label-sm text-on-surface-variant">{s.label}</span>
                  <span className="text-label-md font-bold">{s.value}</span>
                </div>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-between border-t border-outline-variant/30 pt-2">
            <div className="flex items-center gap-2.5">
              <div className="flex -space-x-2">
                {DOG.family.map((name) => (
                  <div key={name} className="flex h-7 w-7 items-center justify-center rounded-full bg-secondary-container text-[11px] font-bold text-on-secondary-container ring-2 ring-surface-container-lowest">
                    {name}
                  </div>
                ))}
              </div>
              <span className="text-body-sm text-on-surface-variant">가족 {DOG.family.length}명이 함께 돌봐요</span>
            </div>
            <Link to={`/dogs/${dog.id}/edit`} className="flex items-center gap-0.5 text-label-md text-on-surface-variant transition-colors hover:text-primary">
              프로필 자세히 보기
              <Icon name="chevron_right" className="text-[16px]" />
            </Link>
          </div>
        </div>

        <div className="flex flex-col justify-between rounded-xl border border-outline-variant/30 bg-surface-container-low p-8 shadow-sm lg:col-span-4">
          <div className="flex flex-col gap-4">
            <div className="inline-flex items-center gap-1.5 self-start rounded-full bg-surface-container-high px-3 py-1 text-label-md font-bold text-on-secondary-container">
              <Icon name="lightbulb" className="text-[18px] text-primary" />
              오늘의 맞춤 안내
            </div>
            <p className="mt-2 text-body-lg leading-relaxed">
              {DOG.tip.before}
              <span className="font-bold text-primary">{DOG.tip.strong}</span>
              {DOG.tip.after}
            </p>
          </div>
          <Link to="/course" className={`${primaryBtn} mt-6 w-full`}>
            <Icon name="near_me" className="text-[20px]" />
            오늘 코스 추천받기
          </Link>
        </div>
      </section>

      {/* 2. 이번 달 건강 요약 */}
      <section className={`${card} relative flex flex-col gap-5 overflow-hidden p-6 sm:p-8`}>
        <div className="absolute top-0 bottom-0 left-0 w-1.5 bg-primary-container" />
        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
          <div className="flex flex-wrap items-center gap-3">
            <div className="inline-flex items-center gap-1.5 rounded-full bg-surface-container px-3 py-1 text-label-md text-on-secondary-container">
              <button type="button" aria-label="이전 달" onClick={() => moveMonth(-1)} className="text-[14px] hover:text-primary">‹</button>
              {month.getFullYear()}년 {month.getMonth() + 1}월
              <button type="button" aria-label="다음 달" onClick={() => moveMonth(1)} className="text-[14px] hover:text-primary">›</button>
            </div>
            <h2 className="text-headline-md font-bold tracking-tight">이번 달 {DOG.name} 건강 요약</h2>
          </div>
          <div className="inline-flex items-center gap-1.5 self-start rounded-full border border-outline-variant/30 bg-surface-container-low px-3 py-1 text-label-sm text-on-surface-variant sm:self-auto">
            <Icon name="psychology" className="text-[15px] text-primary" />
            AI 상담 {MONTHLY_SUMMARY.consultCount}회 · 기록 {MONTHLY_SUMMARY.recordCount}개 기반
          </div>
        </div>

        <p className="leading-relaxed">{MONTHLY_SUMMARY.text}</p>

        <div className="flex flex-wrap items-center gap-2.5">
          {MONTHLY_SUMMARY.items.map(({ label, status, trend }) => (
            <span key={label} className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-label-md ${TREND_STYLE[trend].className}`}>
              <Icon name={TREND_STYLE[trend].icon} className="text-[16px]" /> {label} · {status}
            </span>
          ))}
        </div>

        <div className="flex flex-col justify-between gap-3 border-t border-outline-variant/30 pt-3 sm:flex-row sm:items-center">
          <div className="flex items-center gap-4">
            <Link to="/consult" className={linkArrow}>
              상담 기록 보기 <Icon name="arrow_forward" className="text-[16px]" />
            </Link>
            <span className="text-outline-variant/50">·</span>
            <Link to="/report" className={linkArrow}>
              병원용 리포트 만들기 <Icon name="arrow_forward" className="text-[16px]" />
            </Link>
          </div>
          <span className="text-label-sm text-outline">참고 정보이며 진단이 아니에요.</span>
        </div>
      </section>

      {/* 3. 말하듯 기록하기 */}
      <section className={`${card} flex flex-col gap-4 p-8`}>
        <div>
          <h2 className="text-headline-md font-bold tracking-tight">말하듯 기록하기</h2>
          <p className="mt-0.5 text-body-sm text-on-surface-variant">산책, 식사, 특별한 행동까지 자연어로 편하게 남겨보세요</p>
        </div>
        <div className="flex flex-col items-stretch gap-3 sm:flex-row sm:items-end">
          <textarea
            rows={3}
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
            placeholder={`오늘 ${DOG.name}에게 있었던 일을 편하게 적어주세요 (예: 아침에 사료 반만 먹고 산책 20분 했어)`}
            className="w-full resize-none rounded-xl border border-outline-variant/30 bg-surface-container-low p-4 transition-all placeholder:text-outline focus:bg-surface-container-lowest focus:ring-2 focus:ring-primary-container focus:outline-none"
          />
          {/* 기록 입력 화면이 글을 받아 바로 기록 후보로 바꿉니다 */}
          <button type="button" disabled={!memo.trim()} onClick={() => navigate('/records/new', { state: { text: memo.trim() } })} className={`${primaryBtn} w-full shrink-0 px-6 disabled:opacity-50 sm:w-auto`}>
            <Icon name="auto_awesome" className="text-[20px]" />
            기록으로 바꾸기
          </button>
        </div>
      </section>

      {/* 4. 근처 함께 갈 수 있는 곳 */}
      <section className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="text-headline-md font-bold tracking-tight">근처 함께 갈 수 있는 곳</h2>
          <Link to="/map" className="text-label-lg font-semibold text-primary transition-colors hover:text-on-primary-fixed-variant">
            지도에서 더 보기 →
          </Link>
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          {nearby.map((p) => (
            <Link key={p.id} to={`/map?placeId=${p.id}`} className={`group flex flex-col overflow-hidden ${card} transition-all hover:shadow-md`}>
              <div className="relative h-44 overflow-hidden">
                <Photo src={undefined} alt={p.name} className="h-full w-full transition-transform duration-500 group-hover:scale-105" />
                <span className="absolute top-3 left-3 rounded-full bg-tertiary-fixed px-2.5 py-0.5 text-label-sm font-semibold text-on-tertiary-fixed shadow-sm">
                  {DOG.size} OK
                </span>
              </div>
              <div className="flex flex-1 flex-col justify-between gap-2 p-5">
                <div>
                  <h3 className="text-headline-sm font-bold transition-colors group-hover:text-primary">{p.name}</h3>
                  <p className="mt-1 text-body-sm text-on-surface-variant">{p.summaryFirstLine ?? p.keywords.join(' ')}</p>
                </div>
                <div className="flex items-center gap-1 pt-1 text-label-md font-semibold text-primary">
                  <Icon name="near_me" className="text-[16px]" />
                  {p.distanceM != null ? formatDistance(p.distanceM) : p.categoryLabel}
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}
