import { Link, Navigate, useNavigate } from 'react-router'
import PawLoading from '../components/common/PawLoading'
import { courseApi } from '../api/endpoints'
import { monthDay, SIZE_LABEL } from '../api/format'
import type { Course } from '../api/types'
import Icon from '../components/common/Icon'
import Photo from '../components/common/Photo'
import { CATEGORY_ICON } from '../constants/place'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'
import { THEME_LABEL, type CourseTheme } from '../constants/care'

const THEMES = Object.keys(THEME_LABEL) as CourseTheme[]
// 태그 색: 순서대로 돌려 씀
const TONES = ['bg-tertiary-fixed/60 text-on-tertiary-fixed-variant', 'bg-secondary-fixed/70 text-on-secondary-fixed-variant', 'bg-surface-container-high text-on-surface-variant']
// 카드 순서별 강조 색 (Stitch 원본과 동일)
const AREA_COLOR = ['text-primary', 'text-secondary', 'text-on-surface-variant']
const LEVEL_COLOR = ['text-tertiary', 'text-secondary', 'text-on-surface-variant']

// 서버 코스 → 카드 표시 값
function cardOf(c: Course) {
  const km = c.totalDistanceM / 1000
  const first = c.stops[0]
  return {
    id: c.id,
    name: `${monthDay(c.createdAt)} 추천 코스`,
    area: `${c.stops.length}곳 코스`,
    feature: c.visitedAt ? '다녀온 코스' : '아직 안 간 코스',
    distanceKm: km.toFixed(1),
    minutes: c.totalMinutes,
    difficulty: km < 1.5 ? '쉬움' : km < 3 ? '보통' : '긴 코스',
    imageTag: { icon: first ? CATEGORY_ICON[first.category] : 'route', label: first?.categoryLabel ?? '코스' },
    route: c.stops.map((s) => s.name),
    tags: [...new Set(c.stops.flatMap((s) => s.keywords))].slice(0, 3),
  }
}

// 코스 추천 카드 목록 (Stitch 03-course). 최근 추천받은 코스 3개, 테마 칩은 조건을 채운 추천 화면으로
export default function CoursePage() {
  const navigate = useNavigate()
  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const { data: courses } = useApi(() => (dogId ? courseApi.list(dogId) : null), [dogId])

  if (loading) return <PawLoading />
  if (!dog) return <Navigate to="/onboarding" replace />

  const latest = courses?.[0]
  const cards = (courses ?? []).slice(0, 3).map(cardOf)

  return (
    <div className="mx-auto flex w-full max-w-[1240px] flex-col gap-9 px-10 py-9">
      <header className="flex flex-col gap-6">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div className="flex flex-col gap-1">
            <div className="inline-flex w-fit items-center gap-2 rounded-md bg-secondary-container/50 px-3 py-1 text-label-md text-on-secondary-container">
              <Icon name="favorite" filled className="text-[16px] text-primary" />
              {latest?.reasons[0] ?? `${dog.name} 기준 맞춤 추천`}
            </div>
            <h1 className="text-headline-xl tracking-tight">{dog.name} 맞춤형 산책 코스 🐕</h1>
            <p className="max-w-2xl text-body-lg text-on-surface-variant">
              {SIZE_LABEL[dog.size]}({dog.weightKg}kg) 기준으로 동반하기 어려운 곳은 빼고 이어 드려요.
            </p>
          </div>
          <div className="flex items-center gap-2 rounded-md bg-surface-container-lowest px-4 py-2.5 shadow-[0_4px_20px_-2px_rgba(74,52,38,0.05)]">
            <div className="h-2.5 w-2.5 animate-pulse rounded-full bg-tertiary" />
            <span className="text-label-md">현재 위치 기준 추천</span>
            {latest && <span className="text-label-sm text-outline">{monthDay(latest.createdAt)} 마지막 추천</span>}
          </div>
        </div>

        {/* 테마를 누르면 들를 곳 순서를 채운 추천 화면으로 */}
        <nav aria-label="산책 코스 테마 선택" className="flex flex-wrap items-center gap-2">
          {THEMES.map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => navigate(`/course/plan?theme=${t}`)}
              className="rounded-md bg-surface-container-lowest px-5 py-2.5 text-label-lg text-on-surface-variant transition-all hover:bg-surface-container-low hover:text-primary"
            >
              {THEME_LABEL[t]}
            </button>
          ))}
        </nav>
      </header>

      <div className="grid grid-cols-1 items-stretch gap-6 lg:grid-cols-3">
        {cards.length === 0 && (
          <p className="py-10 text-center text-body-sm text-outline lg:col-span-3">
            아직 추천받은 코스가 없어요.{' '}
            <Link to="/course/plan" className="font-bold text-primary hover:underline">
              코스 추천받기
            </Link>
          </p>
        )}
        {cards.map((c, i) => {
          const top = i === 0 // 가장 최근 코스만 강조
          return (
            <article key={c.id} className={`group relative flex flex-col justify-between overflow-hidden rounded-2xl border bg-surface-container-lowest p-6 ${top ? 'border-primary-container' : 'border-outline-variant'} shadow-[0_4px_20px_-2px_rgba(74,52,38,0.06)] transition-all duration-300 hover:shadow-[0_8px_28px_-4px_rgba(74,52,38,0.12)]`}>
              <div className="flex flex-col gap-4">
                <div className="relative h-44 w-full overflow-hidden rounded-xl bg-surface-container">
                  <Photo src={undefined} alt={c.name} className="h-full w-full transition-transform duration-500 group-hover:scale-105" />
                  {/* 가장 최근 코스 표시: 사진 칸 안쪽에 두어 카드 모서리에 잘리지 않게 */}
                  {top && (
                    <div className="absolute top-2.5 left-2.5 flex items-center gap-1 rounded-md bg-primary-container px-2.5 py-1 text-label-sm font-bold text-on-primary">
                      <Icon name="auto_awesome" className="text-[15px]!" />
                      {dog.name} 최근 추천
                    </div>
                  )}
                  <div className="absolute bottom-2.5 left-2.5 flex items-center gap-1.5 rounded-md bg-surface-container-lowest/90 px-3 py-1 shadow-sm backdrop-blur-md">
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
                      <span key={j} className="contents">
                        {j > 0 && <Icon name="arrow_forward" className="text-[16px] text-outline" />}
                        <span className={j === c.route.length - 1 ? 'font-bold text-primary' : ''}>{stop}</span>
                      </span>
                    ))}
                  </div>
                </div>

                <div className="flex flex-wrap gap-1.5 pt-1">
                  {c.tags.map((t, j) => (
                    <span key={t} className={`rounded-md px-2.5 py-1 text-label-sm font-semibold ${TONES[j % TONES.length]}`}>
                      {t}
                    </span>
                  ))}
                </div>
              </div>

              <Link
                to={`/course/plan?courseId=${c.id}`}
                className={`mt-6 flex h-[52px] w-full items-center justify-center gap-2 rounded-md text-label-lg font-bold transition-all active:scale-[0.98] ${
                  top ? 'bg-primary-container text-on-primary hover:bg-primary' : 'bg-surface-container-low hover:bg-surface-container-high'
                }`}
              >
                <Icon name={top ? 'directions_walk' : 'map'} className={`text-[20px] ${top ? '' : 'text-primary'}`} />
                {top ? '이 코스로 산책 시작' : '코스 상세 및 지도 보기'}
              </Link>
            </article>
          )
        })}
      </div>
    </div>
  )
}
