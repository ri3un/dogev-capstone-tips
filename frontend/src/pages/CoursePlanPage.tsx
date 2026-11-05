import { useState } from 'react'
import Icon from '../components/common/Icon'
import { COURSE_PLAN, type StopKind } from '../mocks/course'
import { DOG } from '../mocks/dog'

const DISTANCES = [1, 2, 3]
const filled = { fontVariationSettings: "'FILL' 1" }
const cardShadow = 'shadow-[0_4px_24px_-4px_rgba(74,52,38,0.06)]'

// 지점 종류별 라벨·색상 (타임라인 번호, 배지, 지도 핀)
const STOP: Record<StopKind, { label: string; num: string; badge: string; note: string; pinLabel: string; pinNum: string }> = {
  start: { label: '출발', num: 'bg-secondary-container/70 text-primary', badge: 'bg-surface-container text-secondary', note: 'text-outline', pinLabel: 'bg-surface-container-lowest', pinNum: 'bg-primary text-on-primary' },
  via: { label: '경유', num: 'bg-tertiary-container/30 text-tertiary', badge: 'bg-tertiary-container/20 text-tertiary', note: 'font-bold text-tertiary', pinLabel: 'bg-tertiary text-on-tertiary', pinNum: 'bg-tertiary-container text-on-tertiary-container' },
  end: { label: '도착', num: 'bg-primary-container text-on-primary', badge: 'bg-secondary-container text-on-secondary-container', note: 'font-bold text-primary', pinLabel: 'bg-surface-container-lowest text-primary', pinNum: 'bg-primary-container text-on-primary' },
}

// 코스 추천 2단계 플로우: 왼쪽 조건 고르기 → 오른쪽 추천 결과 (Stitch 04-course-2step)
export default function CoursePlanPage() {
  const [distance, setDistance] = useState(2)
  const [order, setOrder] = useState([
    { label: '산책로', icon: 'park', color: 'text-tertiary' },
    { label: '카페', icon: 'local_cafe', color: 'text-primary' },
  ])
  const [dragIndex, setDragIndex] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  // 드래그한 칩을 놓은 자리와 바꿉니다.
  const drop = (to: number) => {
    if (dragIndex === null || dragIndex === to) return
    const next = [...order]
    ;[next[dragIndex], next[to]] = [next[to], next[dragIndex]]
    setOrder(next)
    setDragIndex(null)
  }

  // TODO: POST /api/courses/recommend { distanceKm, order } 호출
  const recommend = () => {
    setLoading(true)
    setTimeout(() => setLoading(false), 700)
  }

  // TODO: POST /api/courses/{id}/complete 호출
  const complete = () => {
    setDone(true)
    setTimeout(() => setDone(false), 2200)
  }

  return (
    <div className="mx-auto w-full max-w-[1440px] px-10 py-9">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <div className="mb-1.5 flex items-center gap-2">
            <span className="h-2 w-2 animate-pulse rounded-full bg-primary-container" />
            <span className="text-label-md tracking-wide text-secondary uppercase">Tailored Walk Pathway</span>
          </div>
          <h1 className="text-headline-lg font-bold tracking-tight">맞춤 산책 코스 추천</h1>
        </div>
        <div className="flex items-center gap-2.5 rounded-full bg-surface-container-low px-4 py-2">
          <span className="material-symbols-outlined text-[18px] text-tertiary" style={filled}>eco</span>
          <span className="text-label-md font-medium text-on-surface-variant">
            {DOG.name} ({DOG.breed} · {DOG.age}세) 맞춤 모드
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        {/* 1단계: 조건 고르기 */}
        <aside className={`flex w-full flex-col gap-6 rounded-2xl bg-surface-container-lowest p-6 lg:col-span-4 ${cardShadow}`}>
          <div className="flex items-center justify-between border-b border-surface-container-low pb-4">
            <div className="flex items-center gap-2">
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary text-label-md font-bold text-on-primary">1</span>
              <span className="text-label-lg font-bold">조건 고르기</span>
            </div>
            <Icon name="arrow_forward" className="text-[18px] text-outline" />
            <div className="flex items-center gap-2 opacity-60">
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-surface-container text-label-md font-bold text-on-surface-variant">2</span>
              <span className="text-label-lg font-medium text-on-surface-variant">추천 결과</span>
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <Label icon="home_pin" text="출발 위치" hint="집 또는 현재 위치" />
            <div className="relative flex items-center">
              {/* TODO: 회원 집 주소 / 브라우저 위치 */}
              <input readOnly value="평촌동 (우리 집)" className="h-12 w-full cursor-default rounded-xl bg-surface-container-lowest pr-24 pl-4 shadow-[0_2px_8px_rgba(74,52,38,0.04)] focus:ring-2 focus:ring-primary/30 focus:outline-none" />
              <button type="button" className="absolute right-1.5 flex items-center gap-1 rounded-lg bg-surface-container-low px-3 py-1.5 text-label-sm text-on-surface-variant transition-colors hover:bg-surface-container">
                <Icon name="my_location" className="text-[16px] text-primary" />
                현재 위치
              </button>
            </div>
          </div>

          <div className="flex flex-col gap-2.5">
            <Label icon="straighten" text="총 거리" hint="적정 소요 약 25~35분" hintClass="font-semibold text-tertiary" />
            <div className="grid grid-cols-3 gap-2 rounded-xl bg-surface-container-low p-1">
              {DISTANCES.map((km) => (
                <button
                  key={km}
                  type="button"
                  onClick={() => setDistance(km)}
                  className={`rounded-lg py-2.5 text-center text-label-md transition-all ${
                    distance === km ? 'bg-primary font-bold text-on-primary shadow-[0_2px_8px_rgba(136,81,27,0.2)]' : 'text-on-surface-variant hover:text-on-surface'
                  }`}
                >
                  {km}km
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-2.5">
            <Label icon="alt_route" text="들를 곳 순서" hint="드래그하여 순서 변경" />
            <div className="flex items-center gap-2 rounded-xl bg-surface-container-low/70 p-3">
              {order.map((o, i) => (
                <div key={o.label} className="contents">
                  {i > 0 && <Icon name="trending_flat" className="text-[18px] text-primary" />}
                  <div
                    draggable
                    onDragStart={() => setDragIndex(i)}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={() => drop(i)}
                    className="group flex flex-1 cursor-grab items-center justify-between rounded-lg bg-surface-container-lowest px-3 py-2 shadow-sm transition-all hover:bg-surface-bright active:cursor-grabbing"
                  >
                    <div className="flex items-center gap-1.5">
                      <Icon name="drag_indicator" className="text-[16px] text-outline transition-colors group-hover:text-primary" />
                      <span className="text-label-md font-bold">
                        {i + 1} {o.label}
                      </span>
                    </div>
                    <Icon name={o.icon} className={`text-[16px] ${o.color}`} />
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-start gap-2.5 rounded-xl bg-surface-container-high/60 p-3.5">
            <span className="text-[18px] leading-none select-none">💡</span>
            <p className="text-body-sm leading-relaxed font-medium text-secondary">
              {DOG.name}의 <strong className="font-semibold text-on-surface">최근 관절 메모</strong>로 2km 이내가 기본이에요. 경사로가 적은 평지 산책로를 우선 배치했습니다.
            </p>
          </div>

          <button
            type="button"
            onClick={recommend}
            disabled={loading}
            className="group flex h-[52px] w-full items-center justify-center gap-2 rounded-xl bg-primary-container text-label-lg font-bold text-on-primary shadow-[0_6px_20px_-4px_rgba(200,134,75,0.4)] transition-all hover:bg-primary active:scale-[0.98]"
          >
            <Icon name={loading ? 'refresh' : 'auto_awesome'} className={`text-[20px] transition-transform ${loading ? 'animate-spin' : 'group-hover:rotate-45'}`} />
            {loading ? `${DOG.name} 코스 분석 중...` : '코스 추천받기'}
          </button>
        </aside>

        {/* 2단계: 추천 결과 */}
        <section className={`flex w-full flex-col gap-6 rounded-2xl bg-surface-container-lowest p-7 lg:col-span-8 ${cardShadow}`}>
          <div className="flex flex-col justify-between gap-3 border-b border-surface-container-low pb-5 sm:flex-row sm:items-center">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-full bg-secondary-container/60">
                <span className="material-symbols-outlined text-[24px] text-primary" style={filled}>pets</span>
              </div>
              <div>
                <span className="text-label-sm font-bold tracking-wider text-primary">AI RECOMMENDATION</span>
                <h2 className="text-headline-md font-bold">{DOG.name} 맞춤 추천 코스</h2>
              </div>
            </div>
            <div className="flex items-center gap-2.5 self-start rounded-full bg-surface-container-low px-4 py-2 sm:self-auto">
              <Icon name="directions_walk" className="text-[20px] text-primary" />
              <span className="text-label-lg font-bold">
                총 {COURSE_PLAN.distanceKm}km · 약 {COURSE_PLAN.minutes}분
              </span>
            </div>
          </div>

          {/* 경로 지도 (TODO: 네이버 지도 SDK 폴리라인으로 교체) */}
          <div className="relative h-[320px] w-full overflow-hidden rounded-xl bg-surface-container-low shadow-inner sm:h-[360px]">
            <div className="h-full w-full bg-cover bg-center" style={{ backgroundImage: `url('${COURSE_PLAN.mapImageUrl}')` }} />
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-on-surface/40 via-transparent to-surface-container-lowest/30" />
            <svg className="pointer-events-none absolute inset-0 h-full w-full" fill="none" preserveAspectRatio="none" viewBox="0 0 800 360">
              <path d={COURSE_PLAN.routePath} stroke="rgba(255,248,245,0.9)" strokeLinecap="round" strokeWidth="7" />
              <path d={COURSE_PLAN.routePath} stroke="var(--color-primary-container)" strokeDasharray="8 8" strokeLinecap="round" strokeWidth="4.5" />
            </svg>
            {COURSE_PLAN.stops.map((s, i) => (
              <div key={s.name} style={s.pin} className="absolute flex -translate-x-1/2 -translate-y-1/2 flex-col items-center">
                <div className={`mb-1 rounded-md px-2.5 py-1 text-label-sm font-bold whitespace-nowrap shadow-md ${STOP[s.kind].pinLabel}`}>
                  [{i + 1}] {s.kind === 'start' ? '출발' : s.name}
                </div>
                <div className={`flex h-8 w-8 items-center justify-center rounded-full text-label-md font-bold shadow-lg ring-4 ring-surface-container-lowest ${STOP[s.kind].pinNum}`}>{i + 1}</div>
              </div>
            ))}
            <div className="absolute right-4 bottom-4 flex flex-col gap-1.5">
              {['add', 'remove'].map((n) => (
                <button key={n} type="button" aria-label={n === 'add' ? '지도 확대' : '지도 축소'} className="flex h-8 w-8 items-center justify-center rounded-lg bg-surface-container-lowest/95 shadow-md transition-transform hover:bg-surface-container-lowest active:scale-95">
                  <Icon name={n} className="text-[18px]" />
                </button>
              ))}
            </div>
            <div className="absolute top-4 left-4 flex items-center gap-1.5 rounded-full bg-surface-container-lowest/90 px-3 py-1.5 text-label-sm font-semibold text-secondary shadow-sm backdrop-blur-sm">
              <Icon name="nature" className="text-[15px] text-tertiary" />
              {COURSE_PLAN.surface}
            </div>
          </div>

          {/* 지점 타임라인 */}
          <ol className="flex flex-col px-1 pt-1">
            {COURSE_PLAN.stops.map((s, i) => {
              const last = i === COURSE_PLAN.stops.length - 1
              return (
                <li key={s.name} className="relative flex items-start gap-4">
                  <div className="flex flex-col items-center">
                    <div className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-label-md font-bold ${STOP[s.kind].num}`}>{i + 1}</div>
                    {!last && <div className="my-1 h-16 w-0.5 bg-outline-variant/60" />}
                  </div>
                  <div className={`flex-1 ${last ? '' : 'pb-4'}`}>
                    <div className="mb-1 flex flex-wrap items-center justify-between gap-1">
                      <div className="flex items-center gap-2">
                        <span className={`rounded px-2 py-0.5 text-label-sm font-semibold ${STOP[s.kind].badge}`}>{STOP[s.kind].label}</span>
                        <h3 className="text-headline-sm font-bold">{s.name}</h3>
                      </div>
                      {s.note && <span className={`text-label-sm ${STOP[s.kind].note}`}>{s.note}</span>}
                    </div>
                    {s.description && <p className="mb-1.5 text-body-sm text-on-surface-variant">{s.description}</p>}
                    <div className={`mt-1 inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-label-sm ${last ? 'bg-surface-container-high/50 text-secondary' : 'bg-surface-container-low text-on-surface-variant'}`}>
                      <Icon name={last ? 'turn_left' : 'directions_walk'} className={`text-[14px] ${last ? 'text-secondary' : 'text-primary'}`} />
                      {s.move}
                    </div>
                  </div>
                </li>
              )
            })}
          </ol>

          <div className="flex flex-col items-center gap-3 border-t border-surface-container-low pt-5 sm:flex-row">
            <button
              type="button"
              onClick={complete}
              className={`flex h-[50px] w-full items-center justify-center gap-2 rounded-xl text-label-lg font-bold text-on-primary shadow-[0_4px_16px_-2px_rgba(200,134,75,0.35)] transition-all active:scale-[0.98] sm:flex-1 ${
                done ? 'bg-tertiary' : 'bg-primary-container hover:bg-primary'
              }`}
            >
              <Icon name={done ? 'check' : 'task_alt'} className="text-[20px]" />
              {done ? '산책 완료가 기록되었습니다!' : '이 코스로 다녀왔어요'}
            </button>
            <button type="button" onClick={recommend} className="flex h-[50px] w-full items-center justify-center gap-2 rounded-xl bg-surface-container-low px-6 text-label-lg font-semibold text-on-surface-variant transition-all hover:bg-surface-container hover:text-on-surface sm:w-auto">
              <Icon name="replay" className="text-[18px]" />
              다시 추천
            </button>
          </div>
        </section>
      </div>
    </div>
  )
}

// 조건 항목 제목줄
function Label({ icon, text, hint, hintClass = 'text-outline' }: { icon: string; text: string; hint: string; hintClass?: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="flex items-center gap-1.5 text-label-lg font-bold">
        <Icon name={icon} className="text-[18px] text-primary" />
        {text}
      </span>
      <span className={`text-label-sm ${hintClass}`}>{hint}</span>
    </div>
  )
}
