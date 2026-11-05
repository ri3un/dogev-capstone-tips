import { useState } from 'react'
import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import Photo from '../components/common/Photo'
import { DOG, MONTHLY_SUMMARY, type Trend } from '../mocks/dog'
import { PLACES, formatDistance } from '../mocks/places'

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
  const [month, setMonth] = useState(() => new Date(2026, 9)) // TODO: 오늘 날짜로
  const [memo, setMemo] = useState('')
  const moveMonth = (d: number) => setMonth((m) => new Date(m.getFullYear(), m.getMonth() + d))

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
                {DOG.age}살 · {DOG.weightKg}kg
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
            <Link to="/dogs/1/edit" className="flex items-center gap-0.5 text-label-md text-on-surface-variant transition-colors hover:text-primary">
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
          {/* TODO: POST /api/records/parse 호출 후 /records/new 확인 화면으로 이동 */}
          <button type="button" disabled={!memo.trim()} className={`${primaryBtn} w-full shrink-0 px-6 disabled:opacity-50 sm:w-auto`}>
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
          {PLACES.filter((p) => p.status === 'OK').slice(0, 3).map((p) => (
            <Link key={p.id} to={`/map?placeId=${p.id}`} className={`group flex flex-col overflow-hidden ${card} transition-all hover:shadow-md`}>
              <div className="relative h-44 overflow-hidden">
                <Photo src={p.imageUrl} alt={p.name} className="h-full w-full transition-transform duration-500 group-hover:scale-105" />
                <span className="absolute top-3 left-3 rounded-full bg-tertiary-fixed px-2.5 py-0.5 text-label-sm font-semibold text-on-tertiary-fixed shadow-sm">
                  {DOG.size} OK
                </span>
              </div>
              <div className="flex flex-1 flex-col justify-between gap-2 p-5">
                <div>
                  <h3 className="text-headline-sm font-bold transition-colors group-hover:text-primary">{p.name}</h3>
                  <p className="mt-1 text-body-sm text-on-surface-variant">{p.summary[0]}</p>
                </div>
                <div className="flex items-center gap-1 pt-1 text-label-md font-semibold text-primary">
                  <Icon name="near_me" className="text-[16px]" />
                  {formatDistance(p.distanceM)}
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}
