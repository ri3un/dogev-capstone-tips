import { useState } from 'react'
import { toast } from '../components/common/toast'
import PawLoading from '../components/common/PawLoading'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import ConfirmModal from '../components/common/ConfirmModal'
import { courseApi } from '../api/endpoints'
import { ageText, formatDistance } from '../api/format'
import type { ApiError, PlaceCategory } from '../api/types'
import Icon from '../components/common/Icon'
import NaverMap, { type NaverMapInstance } from '../components/common/NaverMap'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'
import { FALLBACK_LOCATION, useMyLocation } from '../hooks/useMyLocation'

type StopKind = 'start' | 'via' | 'end'

const DISTANCES = [1, 2, 3]
const cardShadow = 'shadow-[0_4px_24px_-4px_rgba(74,52,38,0.06)]'

// 들를 곳 칩 (서버 카테고리)
const TYPE_CHIP: Partial<Record<PlaceCategory, { label: string; icon: string; color: string }>> = {
  PARK: { label: '산책로', icon: 'park', color: 'text-tertiary' },
  CAFE: { label: '카페', icon: 'local_cafe', color: 'text-primary' },
  RESTAURANT: { label: '식당', icon: 'restaurant', color: 'text-secondary' },
}
const ADDABLE: PlaceCategory[] = ['PARK', 'CAFE', 'RESTAURANT']
const MAX_STOPS = 3 // 서버 CourseDtos.Recommend placeTypes @Size(max = 3)
const MAX_EXCLUDED = 500 // 서버 excludePlaceIds @Size(max = 500)
// 코스 화면 테마 칩에서 넘어오면 들를 곳 순서를 미리 채움
const THEME_ORDER: Record<string, PlaceCategory[]> = {
  light: ['PARK', 'CAFE'],
  forest: ['PARK', 'PARK'],
  cafe: ['CAFE', 'RESTAURANT'],
}

// 지점 종류별 라벨·색상 (타임라인 번호, 배지, 지도 핀)
const STOP: Record<StopKind, { label: string; num: string; badge: string; note: string; pinLabel: string; pinNum: string }> = {
  start: { label: '출발', num: 'bg-secondary-container/70 text-primary', badge: 'bg-surface-container text-secondary', note: 'text-outline', pinLabel: 'bg-surface-container-lowest', pinNum: 'bg-primary text-on-primary' },
  via: { label: '경유', num: 'bg-tertiary-container/30 text-tertiary', badge: 'bg-tertiary-container/20 text-tertiary', note: 'font-bold text-tertiary', pinLabel: 'bg-tertiary text-on-tertiary', pinNum: 'bg-tertiary-container text-on-tertiary-container' },
  end: { label: '도착', num: 'bg-primary-container text-on-primary', badge: 'bg-secondary-container text-on-secondary-container', note: 'font-bold text-primary', pinLabel: 'bg-surface-container-lowest text-primary', pinNum: 'bg-primary-container text-on-primary' },
}

// 코스 추천 2단계 플로우: 왼쪽 조건 고르기 → 오른쪽 추천 결과 (Stitch 04-course-2step)
export default function CoursePlanPage() {
  const [params, setParams] = useSearchParams()
  const courseId = Number(params.get('courseId')) || null
  const [distance, setDistance] = useState(2)
  const [order, setOrder] = useState<PlaceCategory[]>(() => THEME_ORDER[params.get('theme') ?? ''] ?? THEME_ORDER.light)
  const [dragIndex, setDragIndex] = useState<number | null>(null)
  const [adding, setAdding] = useState(false)
  // 지금 코스 이전에 보여 준 코스들의 장소. 다시 추천 때 빼고, 조건(key)이 바뀌면 버림
  const [shown, setShown] = useState<{ key: string; ids: number[] }>({ key: '', ids: [] })
  const [loading, setLoading] = useState(false)
  const [completing, setCompleting] = useState(false)
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const navigate = useNavigate()
  const [naverMap, setNaverMap] = useState<NaverMapInstance | null>(null)

  const { dog, loading: dogLoading } = useCurrentDog()
  const pos = useMyLocation()
  const { data: course, setData: setCourse } = useApi(() => (courseId ? courseApi.get(courseId) : null), [courseId])

  if (dogLoading) return <PawLoading />
  if (!dog) return <Navigate to="/onboarding" replace />

  // 반려견·거리·들를 곳 중 하나라도 바뀌면 이전 코스 제외 목록은 무효
  const conditionKey = JSON.stringify([dog.id, distance, order])
  const excluded = shown.key === conditionKey ? shown.ids : []

  // 드래그한 칩을 놓은 자리와 바꿉니다.
  const drop = (to: number) => {
    if (dragIndex === null || dragIndex === to) return
    const next = [...order]
    ;[next[dragIndex], next[to]] = [next[to], next[dragIndex]]
    setOrder(next)
    setDragIndex(null)
  }

  // 다시 추천: 지금까지 보여 준 장소를 빼고, 지금 코스가 임시 코스면 서버가 새 코스로 바꿉니다.
  const recommend = async (retry: boolean) => {
    if (!pos || loading) return
    setLoading(true)
    const base = { dogId: dog.id, lat: pos.lat, lng: pos.lng, placeTypes: order, radiusKm: distance, replaceCourseId: retry ? course?.id : undefined }
    // shortcut: 서버 한도(500개)를 넘으면 오래된 것부터 버림
    let exclude = retry && course ? [...excluded, ...course.stops.map((s) => s.placeId)].slice(-MAX_EXCLUDED) : []
    try {
      let c
      let exhausted = false
      try {
        c = await courseApi.recommend({ ...base, excludePlaceIds: exclude })
      } catch (e) {
        if ((e as ApiError).code !== 'COURSE_NOT_ENOUGH_PLACES' || !exclude.length) throw e
        // 반경 안 후보를 다 보여 줬으면 처음 코스부터 다시
        exclude = []
        exhausted = true
        c = await courseApi.recommend(base)
      }
      if (exhausted) toast('반경 안에 다른 코스가 없어요. 거리를 늘려 보세요')
      setShown({ key: conditionKey, ids: exclude })
      setCourse(c)
      // 다시 추천은 지운 임시 코스로 뒤로 가지 않게 기록을 바꿔 끼움
      setParams({ courseId: String(c.id) }, { replace: retry })
    } catch (e) {
      alert((e as { message?: string }).message ?? '코스를 만들지 못했어요. 잠시 후 다시 시도해 주세요')
    } finally {
      setLoading(false)
    }
  }

  // 다녀왔어요: 서버가 활동 기록을 남기고, 두 번 눌러도 한 번만 저장
  const complete = async () => {
    if (!course || course.visitedAt || completing) return
    setCompleting(true)
    try {
      setCourse(await courseApi.complete(course.id))
      toast('킁킁, 산책 다녀왔어요!')
    } catch {
      alert('산책 완료를 기록하지 못했어요')
    } finally {
      setCompleting(false)
    }
  }
  const done = !!course?.visitedAt

  // 코스 저장하기: 저장한 코스에 나오고, 다시 추천해도 지워지지 않음
  const save = async () => {
    if (!course || course.savedAt || saving) return
    setSaving(true)
    try {
      setCourse(await courseApi.save(course.id))
      toast('코스를 저장했어요')
    } catch {
      alert('코스를 저장하지 못했어요')
    } finally {
      setSaving(false)
    }
  }
  const saved = !!course?.savedAt

  const removeCourse = async () => {
    if (!course) return
    try {
      await courseApi.remove(course.id)
      toast('코스를 삭제했어요')
      navigate('/courses')
    } catch {
      alert('코스를 삭제하지 못했어요')
      setDeleting(false)
    }
  }

  // 출발점 + 들르는 곳. 출발 위치는 서버에 저장하지 않아서 지금 위치로 그림
  const start = pos ?? FALLBACK_LOCATION
  const stops = course
    ? [
        { kind: 'start' as StopKind, name: '출발', note: '0m · 시작 지점', description: null as string | null, lat: start.lat, lng: start.lng },
        ...course.stops.map((s, i) => ({
          kind: (i === course.stops.length - 1 ? 'end' : 'via') as StopKind,
          name: s.name,
          note: s.categoryLabel,
          description: s.keywords.length ? s.keywords.join(' ') : null,
          lat: s.lat,
          lng: s.lng,
        })),
      ]
    : []
  // 다음 지점까지 이동 안내: 서버 legDistanceM 은 "그 지점에 도착하는" 구간
  const moveText = (i: number) => {
    const next = course?.stops[i]
    return next ? `도보 ${next.legMinutes}분 · ${formatDistance(next.legDistanceM)}` : '코스 끝'
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
        <div className="flex items-center gap-2.5 rounded-md bg-surface-container-low px-4 py-2">
          <Icon name="eco" filled className="text-[18px] text-tertiary" />
          <span className="text-label-md font-medium text-on-surface-variant">
            {dog.name} ({dog.breed} · {ageText(dog.ageMonths)}) 맞춤 모드
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
              <input
                readOnly
                value={!pos ? '위치 확인 중…' : pos === FALLBACK_LOCATION ? '안양 평촌역 (위치 권한 없음)' : '현재 위치'}
                className="h-12 w-full cursor-default rounded-xl bg-surface-container-lowest pr-24 pl-4 shadow-[0_2px_8px_rgba(74,52,38,0.04)] focus:ring-2 focus:ring-primary/30 focus:outline-none"
              />
              <button type="button" className="absolute right-1.5 flex items-center gap-1 rounded-lg bg-surface-container-low px-3 py-1.5 text-label-sm text-on-surface-variant transition-colors hover:bg-surface-container">
                <Icon name="my_location" className="text-[16px] text-primary" />
                현재 위치
              </button>
            </div>
          </div>

          <div className="flex flex-col gap-2.5">
            <Label icon="straighten" text="총 거리" hint={`적정 소요 약 ${distance * 15}분`} hintClass="font-semibold text-tertiary" />
            <div className="grid grid-cols-3 gap-2 rounded-xl bg-surface-container-low p-1">
              {DISTANCES.map((km) => (
                <button
                  key={km}
                  type="button"
                  onClick={() => setDistance(km)}
                  className={`rounded-lg py-2.5 text-center text-label-md transition-all ${
                    distance === km ? 'bg-primary font-bold text-on-primary' : 'text-on-surface-variant hover:text-on-surface'
                  }`}
                >
                  {km}km
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-2.5">
            <Label icon="alt_route" text="들를 곳 순서" hint={`최대 ${MAX_STOPS}곳 · 드래그하여 순서 변경`} />
            {/* 좁은 칸에서도 이름이 잘리지 않게 3칸 격자 + 세로 카드로 배치 (번호가 순서를 나타냄) */}
            <div className="grid grid-cols-3 gap-2 rounded-xl bg-surface-container-low/70 p-3">
              {order.map((t, i) => {
                const o = TYPE_CHIP[t] ?? TYPE_CHIP.PARK!
                return (
                  <div
                    key={i}
                    draggable
                    onDragStart={() => setDragIndex(i)}
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={() => drop(i)}
                    className="group relative flex min-h-[64px] min-w-0 cursor-grab flex-col items-center justify-center gap-1 rounded-lg bg-surface-container-lowest px-2 pt-3 pb-2 shadow-sm transition-all hover:bg-surface-bright active:cursor-grabbing"
                  >
                    <span className="absolute top-1.5 left-2 text-[11px] leading-none font-bold text-primary">{i + 1}</span>
                    <Icon name={o.icon} className={`text-[20px] ${o.color}`} />
                    <span className="max-w-full truncate text-label-md font-bold">{o.label}</span>
                    {order.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setOrder(order.filter((_, j) => j !== i))}
                        aria-label={`${i + 1}번째 ${o.label} 빼기`}
                        className="absolute top-1 right-1 flex items-center rounded-full p-0.5 text-outline transition-colors hover:text-error"
                      >
                        <Icon name="close" className="text-[14px]" />
                      </button>
                    )}
                  </div>
                )
              })}
              {order.length < MAX_STOPS && (
                <div
                  className="relative"
                  // 메뉴 바깥을 누르거나 Esc 를 누르면 닫힘
                  onBlur={(e) => !e.currentTarget.contains(e.relatedTarget) && setAdding(false)}
                  onKeyDown={(e) => e.key === 'Escape' && setAdding(false)}
                >
                  <button
                    type="button"
                    onClick={() => setAdding((v) => !v)}
                    aria-label="들를 곳 추가"
                    aria-expanded={adding}
                    className="flex h-full min-h-[64px] w-full flex-col items-center justify-center gap-0.5 rounded-lg border border-dashed border-outline-variant text-primary transition-colors hover:bg-surface-container-lowest"
                  >
                    <Icon name="add" className="text-[18px]" />
                    <span className="text-[11px] font-bold">추가</span>
                  </button>
                  {adding && (
                    <div className="absolute top-full right-0 z-10 mt-1 flex flex-col rounded-lg bg-surface-container-lowest p-1 shadow-md">
                      {ADDABLE.map((t) => (
                        <button
                          key={t}
                          type="button"
                          onClick={() => {
                            setOrder([...order, t])
                            setAdding(false)
                          }}
                          className="flex items-center gap-1.5 rounded-md px-3 py-2 text-label-md whitespace-nowrap transition-colors hover:bg-surface-container-low"
                        >
                          <Icon name={TYPE_CHIP[t]!.icon} className={`text-[16px] ${TYPE_CHIP[t]!.color}`} />
                          {TYPE_CHIP[t]!.label}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          <div className="flex items-start gap-2.5 rounded-xl bg-surface-container-high/60 p-3.5">
            <span className="text-[18px] leading-none select-none">💡</span>
            <p className="text-body-sm leading-relaxed font-medium text-secondary">
              {course?.reasons.length ? (
                course.reasons.join(' ')
              ) : (
                <>
                  {dog.name}의 <strong className="font-semibold text-on-surface">나이·지병·최근 기록</strong>을 보고 거리를 맞추고, 동반이 어려운 곳은 빼고 추천해요.
                </>
              )}
            </p>
          </div>

          <button
            type="button"
            onClick={() => recommend(false)}
            disabled={loading || !pos}
            className="group flex h-[52px] w-full items-center justify-center gap-2 rounded-xl bg-primary-container text-label-lg font-bold text-on-primary transition-all hover:bg-primary active:scale-[0.98]"
          >
            <Icon name={loading ? 'refresh' : 'auto_awesome'} className={`text-[20px] transition-transform ${loading ? 'animate-spin' : 'group-hover:rotate-45'}`} />
            {loading ? `${dog.name} 코스 분석 중...` : '코스 추천받기'}
          </button>
        </aside>

        {/* 2단계: 추천 결과 */}
        <section className={`flex w-full flex-col gap-6 rounded-2xl bg-surface-container-lowest p-7 lg:col-span-8 ${cardShadow}`}>
          <div className="flex flex-col justify-between gap-3 border-b border-surface-container-low pb-5 sm:flex-row sm:items-center">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-md bg-secondary-container/60">
                <Icon name="pets" filled className="text-[24px] text-primary" />
              </div>
              <div>
                <span className="text-label-sm font-bold tracking-wider text-primary">AI RECOMMENDATION</span>
                <h2 className="text-headline-md font-bold">{dog.name} 맞춤 추천 코스</h2>
              </div>
            </div>
            {course && (
              <div className="flex items-center gap-2.5 self-start rounded-md bg-surface-container-low px-4 py-2 sm:self-auto">
                <Icon name="directions_walk" className="text-[20px] text-primary" />
                <span className="text-label-lg font-bold">
                  총 {(course.totalDistanceM / 1000).toFixed(1)}km · 약 {course.totalMinutes}분
                </span>
              </div>
            )}
          </div>

          {loading ? (
            <PawLoading label={`킁킁, ${dog.name}에게 맞는 코스를 찾는 중이에요…`} />
          ) : !course ? (
            <p className="py-16 text-center text-body-sm text-outline">왼쪽에서 조건을 고르고 '코스 추천받기'를 눌러 주세요.</p>
          ) : (
            <>
              {/* 경로 지도: 네이버 지도 + 출발·경유·도착 핀 + 점선 경로 */}
              <div className="relative h-[320px] w-full overflow-hidden rounded-xl bg-surface-container-low shadow-inner sm:h-[360px]">
                <NaverMap
                  className="h-full w-full"
                  onReady={setNaverMap}
                  fitKey={`${course.id}:${start.lat}:${start.lng}`}
                  path={stops}
                  pins={stops.map((s, i) => ({
                    id: i,
                    lat: s.lat,
                    lng: s.lng,
                    zIndex: i + 1,
                    node: (
                      <div className="absolute flex -translate-x-1/2 -translate-y-1/2 flex-col items-center">
                        <div className={`mb-1 rounded-md px-2.5 py-1 text-label-sm font-bold whitespace-nowrap shadow-md ${STOP[s.kind].pinLabel}`}>
                          [{i + 1}] {s.kind === 'start' ? '출발' : s.name}
                        </div>
                        <div className={`flex h-8 w-8 items-center justify-center rounded-full text-label-md font-bold shadow-lg ring-4 ring-surface-container-lowest ${STOP[s.kind].pinNum}`}>{i + 1}</div>
                      </div>
                    ),
                  }))}
                />
                <div className="absolute right-4 bottom-4 flex flex-col gap-1.5">
                  {['add', 'remove'].map((n) => (
                    <button key={n} type="button" onClick={() => naverMap?.setZoom(naverMap.getZoom() + (n === 'add' ? 1 : -1), true)} aria-label={n === 'add' ? '지도 확대' : '지도 축소'} className="flex h-8 w-8 items-center justify-center rounded-lg bg-surface-container-lowest/95 shadow-md transition-transform hover:bg-surface-container-lowest active:scale-95">
                      <Icon name={n} className="text-[18px]" />
                    </button>
                  ))}
                </div>
                <div className="absolute top-4 left-4 flex items-center gap-1.5 rounded-md bg-surface-container-lowest/90 px-3 py-1.5 text-label-sm font-semibold text-secondary shadow-sm backdrop-blur-sm">
                  <Icon name="nature" className="text-[15px] text-tertiary" />
                  {course.stops.length}곳 · {course.stops.map((s) => s.categoryLabel).join(' → ')}
                </div>
              </div>

              {/* 지점 타임라인 */}
              <ol className="flex flex-col px-1 pt-1">
                {stops.map((s, i) => {
                  const last = i === stops.length - 1
                  return (
                    <li key={i} className="relative flex items-start gap-4">
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
                          {moveText(i)}
                        </div>
                      </div>
                    </li>
                  )
                })}
              </ol>

              <button
                type="button"
                onClick={save}
                disabled={saved || saving}
                className={`-mb-3 flex h-[50px] w-full items-center justify-center gap-2 rounded-xl border text-label-lg font-bold transition-all active:scale-[0.98] ${
                  saved ? 'border-tertiary/40 text-tertiary' : 'border-primary-container text-primary hover:bg-surface-container-low'
                }`}
              >
                <Icon name={saved ? 'bookmark_added' : 'bookmark_add'} className="text-[20px]" />
                {saved ? '저장한 코스예요' : '코스 저장하기'}
              </button>
              <div className="flex flex-col items-center gap-3 border-t border-surface-container-low pt-5 sm:flex-row">
                <button
                  type="button"
                  onClick={complete}
                  disabled={done || completing}
                  className={`flex h-[50px] w-full items-center justify-center gap-2 rounded-xl text-label-lg font-bold text-on-primary transition-all active:scale-[0.98] sm:flex-1 ${
                    done ? 'bg-tertiary' : 'bg-primary-container hover:bg-primary'
                  }`}
                >
                  <Icon name={done ? 'check' : 'task_alt'} className="text-[20px]" />
                  {done ? '산책 완료가 기록되었습니다!' : '이 코스로 다녀왔어요'}
                </button>
                <button type="button" onClick={() => recommend(true)} disabled={loading || !pos} className="flex h-[50px] w-full items-center justify-center gap-2 rounded-xl bg-surface-container-low px-6 text-label-lg font-semibold text-on-surface-variant transition-all hover:bg-surface-container hover:text-on-surface sm:w-auto">
                  <Icon name="replay" className="text-[18px]" />
                  다시 추천
                </button>
              </div>
              <button type="button" onClick={() => setDeleting(true)} className="mx-auto flex items-center gap-1 text-label-md text-outline transition-colors hover:text-error">
                <Icon name="delete" className="text-[16px]" />
                코스 삭제
              </button>
            </>
          )}
        </section>
      </div>
      {deleting && course && (
        <ConfirmModal
          title="이 코스를 삭제할까요?"
          body="가족 모두의 저장한 코스에서 사라지고 되돌릴 수 없어요. 다녀온 산책 기록은 남아요."
          confirmLabel="삭제"
          danger
          onConfirm={removeCourse}
          onClose={() => setDeleting(false)}
        />
      )}
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
