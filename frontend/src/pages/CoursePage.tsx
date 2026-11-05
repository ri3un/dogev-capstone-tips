import { useState } from 'react'
import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import Photo from '../components/common/Photo'
import { COURSE_CARDS, THEME_LABEL, WALK_TIP, type CourseTheme, type Tone } from '../mocks/course'
import { DOG } from '../mocks/dog'

const THEMES = Object.keys(THEME_LABEL) as CourseTheme[]
const TONE: Record<Tone, string> = {
  green: 'bg-tertiary-fixed/60 text-on-tertiary-fixed-variant',
  apricot: 'bg-secondary-fixed/70 text-on-secondary-fixed-variant',
  neutral: 'bg-surface-container-high text-on-surface-variant',
}
// 카드 순서별 강조 색 (Stitch 원본과 동일)
const AREA_COLOR = ['text-primary', 'text-secondary', 'text-on-surface-variant']
const LEVEL_COLOR = ['text-tertiary', 'text-secondary', 'text-on-surface-variant']
const filled = { fontVariationSettings: "'FILL' 1" }

// 코스 추천 카드 목록 (Stitch 03-course). 카드를 누르면 2단계 플로우(/course/plan)로 이동
export default function CoursePage() {
  const [theme, setTheme] = useState<CourseTheme>('light') // TODO: 테마별 추천 API 파라미터로 전달

  return (
    <div className="mx-auto flex w-full max-w-[1240px] flex-col gap-9 px-10 py-9">
      <header className="flex flex-col gap-6">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div className="flex flex-col gap-1">
            <div className="inline-flex w-fit items-center gap-2 rounded-full bg-secondary-container/50 px-3 py-1 text-label-md text-on-secondary-container">
              <span className="material-symbols-outlined text-[16px] text-primary" style={filled}>favorite</span>
              오늘의 컨디션 매칭 지수 98%
            </div>
            <h1 className="text-headline-xl tracking-tight">{DOG.name} 맞춤형 산책 코스 🐕</h1>
            <p className="max-w-2xl text-body-lg text-on-surface-variant">
              {DOG.size}({DOG.weightKg}kg), 다리 관절 주의 알림을 고려한 완만한 평지 위주의 코스입니다.
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-full bg-surface-container-lowest px-4 py-2.5 shadow-[0_4px_20px_-2px_rgba(74,52,38,0.05)]">
            <div className="h-2.5 w-2.5 animate-pulse rounded-full bg-tertiary" />
            <span className="text-label-md">안양 평촌동 기준 실시간 반영</span>
            <span className="text-label-sm text-outline">오후 2:30 갱신</span>
          </div>
        </div>

        <nav aria-label="산책 코스 테마 선택" className="flex flex-wrap items-center gap-2">
          {THEMES.map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setTheme(t)}
              className={`rounded-full px-5 py-2.5 text-label-lg transition-all ${
                theme === t
                  ? 'bg-primary-container font-bold text-on-primary shadow-[0_4px_16px_-2px_rgba(200,134,75,0.25)]'
                  : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-low hover:text-primary'
              }`}
            >
              {THEME_LABEL[t]}
            </button>
          ))}
        </nav>
      </header>

      <div className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-3">
        {COURSE_CARDS.map((c, i) => {
          const top = i === 0 // 1순위 카드만 강조
          return (
            <article key={c.id} className="group relative flex flex-col justify-between overflow-hidden rounded-2xl bg-surface-container-lowest p-6 shadow-[0_4px_20px_-2px_rgba(74,52,38,0.06)] transition-all duration-300 hover:shadow-[0_8px_28px_-4px_rgba(74,52,38,0.12)]">
              {top && (
                <div className="absolute top-0 right-0 flex items-center gap-1 rounded-bl-xl bg-primary-container px-4 py-1.5 text-label-sm font-bold text-on-primary shadow-sm">
                  <Icon name="auto_awesome" className="text-[14px]" />
                  {DOG.name} 추천 1순위
                </div>
              )}
              <div className="flex flex-col gap-4">
                <div className="relative h-44 w-full overflow-hidden rounded-xl bg-surface-container">
                  <Photo src={c.imageUrl} alt={c.name} className="h-full w-full transition-transform duration-500 group-hover:scale-105" />
                  <div className="absolute bottom-2.5 left-2.5 flex items-center gap-1.5 rounded-full bg-surface-container-lowest/90 px-3 py-1 shadow-sm backdrop-blur-md">
                    <Icon name={c.imageTag.icon} className="text-[16px] text-tertiary" />
                    <span className="text-label-sm font-bold">{c.imageTag.label}</span>
                  </div>
                </div>

                <div className="flex flex-col gap-1">
                  <div className="flex items-center gap-2 text-label-sm font-bold">
                    <span className={AREA_COLOR[i]}>{c.area}</span>
                    <span className="text-outline-variant">·</span>
                    <span className={i === 0 ? 'text-tertiary' : 'font-semibold text-on-surface-variant'}>{c.feature}</span>
                  </div>
                  <h2 className="text-headline-md font-bold tracking-tight">{c.name}</h2>
                  <p className="mt-0.5 flex items-center gap-2 text-body-sm text-on-surface-variant">
                    총 {c.distanceKm}km <span className="text-outline-variant">•</span> 예상 {c.minutes}분 <span className="text-outline-variant">•</span>
                    <span className={`font-bold ${LEVEL_COLOR[i]}`}>난이도: {c.difficulty}</span>
                  </p>
                </div>

                <div className="flex flex-col gap-2 rounded-xl bg-surface-container-low/70 p-3.5">
                  <span className="flex items-center gap-1 text-label-sm font-bold text-on-surface-variant">
                    <Icon name="route" className="text-[16px] text-primary" />
                    추천 이동 경로
                  </span>
                  <div className="flex items-center justify-between px-1 text-label-md">
                    {c.route.map((stop, j) => (
                      <span key={stop} className="contents">
                        {j > 0 && <Icon name="arrow_forward" className="text-[16px] text-outline" />}
                        <span className={j === c.route.length - 1 ? 'font-bold text-primary' : ''}>{stop}</span>
                      </span>
                    ))}
                  </div>
                </div>

                <div className="flex flex-wrap gap-1.5 pt-1">
                  {c.tags.map((t) => (
                    <span key={t.label} className={`rounded-full px-2.5 py-1 text-label-sm font-semibold ${TONE[t.tone]}`}>
                      {t.label}
                    </span>
                  ))}
                </div>
              </div>

              <Link
                to={`/course/plan?courseId=${c.id}`}
                className={`mt-6 flex h-[52px] w-full items-center justify-center gap-2 rounded-full text-label-lg font-bold transition-all active:scale-[0.98] ${
                  top ? 'bg-primary-container text-on-primary shadow-[0_4px_16px_rgba(200,134,75,0.25)] hover:bg-primary' : 'bg-surface-container-low hover:bg-surface-container-high'
                }`}
              >
                <Icon name={top ? 'directions_walk' : 'map'} className={`text-[20px] ${top ? '' : 'text-primary'}`} />
                {top ? '이 코스로 산책 시작' : '코스 상세 및 지도 보기'}
              </Link>
            </article>
          )
        })}
      </div>

      {/* 산책 팁 (TODO: 날씨 API) */}
      <section className="flex flex-col items-start justify-between gap-4 rounded-2xl bg-surface-container-low p-6 shadow-[0_4px_20px_-2px_rgba(74,52,38,0.04)] md:flex-row md:items-center">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-secondary-container text-primary shadow-sm">
            <Icon name="wb_sunny" className="text-[26px]" />
          </div>
          <div className="flex flex-col gap-0.5">
            <div className="flex items-center gap-2 text-label-sm">
              <span className="rounded-full bg-surface-container-highest px-2 py-0.5 font-bold text-primary">실시간 산책 케어 팁</span>
              <span className="text-outline">바닥 체감 {WALK_TIP.floorTemp}℃</span>
            </div>
            <p className="text-headline-sm font-bold">{WALK_TIP.title}</p>
            <p className="text-body-sm text-on-surface-variant">
              {DOG.name}의 {WALK_TIP.body}
            </p>
          </div>
        </div>
        <div className="flex items-center justify-end gap-2 self-stretch md:self-auto">
          {WALK_TIP.checklist.map((item) => (
            <div key={item} className="flex items-center gap-1.5 rounded-full bg-surface-container-lowest px-4 py-2 text-label-md shadow-sm">
              <span className="material-symbols-outlined text-[18px] text-tertiary" style={filled}>check_circle</span>
              {item}
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
