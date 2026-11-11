import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { authApi, dogApi, familyApi, placeApi, recordApi } from '../api/endpoints'
import { formatDistance, todayStr } from '../api/format'
import type { Dog, HealthRecord, RecordCategory, RecordInput } from '../api/types'
import Icon from '../components/common/Icon'
import { BoneIcon, PawIcon } from '../components/common/Paw'
import PawLoading from '../components/common/PawLoading'
import Photo from '../components/common/Photo'
import { toast } from '../components/common/toast'
import FamilyMembersModal from '../components/family/FamilyMembersModal'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'

// 오늘의 맞춤 안내: 코스 추천과 같은 기준(노령견·관절이면 2km 이내)
function tipOf(dog: Dog) {
  if (dog.ageMonths >= 96) return '노령견이라 오늘은 2km 이내 산책을 추천해요.'
  if (dog.diseases.includes('JOINT')) return '관절 지병이 있어 오늘은 2km 이내 산책을 추천해요.'
  if (dog.activityLevel === 'HIGH') return '활동량이 많으니 뛰어놀 수 있는 곳을 넣어 보세요.'
  return '오늘도 가볍게 산책하고 기록을 남겨 보세요.'
}

// 하루 요약에 아이콘으로 두는 항목 (누르면 그 자리에서 기록 1건 저장)
const DAILY: { category: RecordCategory; label: string; icon: string; extra?: Partial<RecordInput> }[] = [
  { category: 'MEAL', label: '밥', icon: 'restaurant' },
  { category: 'ACTIVITY', label: '산책', icon: 'directions_walk' },
  { category: 'STOOL', label: '배변', icon: 'eco', extra: { stoolStatus: 'NORMAL' } },
  { category: 'SNACK', label: '간식', icon: 'cookie' },
]
const SLOTS = 3

// 홈 바로가기 아이콘 줄
const SHORTCUTS = [
  { to: '/map', label: '장소 지도', icon: 'location_on' },
  { to: '/course', label: '코스 추천', icon: 'route' },
  { to: '/consult', label: 'AI 상담', icon: 'chat' },
  { to: '/records', label: '건강 기록', icon: 'bone' },
  { to: '/report', label: '병원용 리포트', icon: 'description' },
  { to: '/my/family', label: '가족 그룹', icon: 'group' },
]

// 배경에 연하게 찍힌 발자국 위치 (오른쪽 위 빈 자리)
const TRAIL = [
  { right: 40, top: 62, rotate: -38 },
  { right: 82, top: 30, rotate: -62 },
  { right: 128, top: 54, rotate: -50 },
  { right: 172, top: 22, rotate: -70 },
  { right: 218, top: 42, rotate: -58 },
  { right: 262, top: 12, rotate: -76 },
]

const lowCard = 'flex flex-col gap-4 rounded-md bg-surface-container-lowest p-6'
const moreLink = 'text-label-md font-semibold text-primary-container hover:underline'

// 홈: 한 줄 기록 → 바로가기 → 오늘 하루 요약·권장량·근처 장소 (스크롤 없이 한 화면)
export default function HomePage() {
  const navigate = useNavigate()
  const [memo, setMemo] = useState('')
  const [showFamily, setShowFamily] = useState(false)
  const [busy, setBusy] = useState<RecordCategory | null>(null)

  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const today = todayStr()
  const { data: nutrition } = useApi(() => (dogId ? dogApi.nutrition(dogId) : null), [dogId])
  const { data: family } = useApi(() => familyApi.get(), [])
  const { data: me } = useApi(() => authApi.me(), [])
  const { data: todayRecords, setData: setTodayRecords } = useApi(() => (dogId ? recordApi.list(dogId, { from: today, to: today }) : null), [dogId, today])
  const { data: places } = useApi(() => (dogId ? placeApi.list({ dogId }) : null), [dogId])

  if (loading) return <PawLoading />
  if (!dog) return <Navigate to="/onboarding" replace />

  const records = todayRecords ?? []
  const nearby = (places ?? []).filter((p) => p.verdict?.level === 'OK').slice(0, 3)
  const weekday = ['일', '월', '화', '수', '목', '금', '토'][new Date(`${today}T00:00:00`).getDay()]
  const dateLabel = `${Number(today.slice(5, 7))}월 ${Number(today.slice(8, 10))}일 (${weekday})`

  // 한 줄 기록: 기록 입력 화면이 글을 받아 바로 기록 후보로 바꿈
  const submitMemo = () => memo.trim() && navigate('/records/new', { state: { text: memo.trim() } })

  // 빈 아이콘을 누르면 오늘 기록 1건 저장 (가족 모두에게 보임)
  const add = async (item: (typeof DAILY)[number]) => {
    if (busy) return
    setBusy(item.category)
    try {
      const saved = await recordApi.create(dog.id, { category: item.category, recordDate: today, ...item.extra })
      setTodayRecords([...records, saved])
      toast(`킁킁, ${item.label} 기록했어요!`)
    } catch {
      toast('기록하지 못했어요. 잠시 후 다시 눌러 주세요')
    } finally {
      setBusy(null)
    }
  }

  // 채워진 아이콘을 누르면 방금 누른 기록만 되돌림 (글이 있는 기록은 건강 기록 화면에서 지움)
  const undo = async (record: HealthRecord, label: string) => {
    if (busy) return
    if (record.memo) return navigate(`/records?date=${today}`)
    setBusy(record.category)
    try {
      await recordApi.remove(record.id)
      setTodayRecords(records.filter((r) => r.id !== record.id))
      toast(`${label} 기록을 되돌렸어요`)
    } catch {
      toast('되돌리지 못했어요')
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="flex w-full flex-col">
      {/* 1. 한 줄 기록 */}
      <section className="relative overflow-hidden bg-gradient-to-b from-surface-container-low to-surface pt-9 pb-8">
        {TRAIL.map(({ rotate, ...pos }) => (
          <PawIcon key={pos.right} className="pointer-events-none absolute h-5 w-5 text-primary-container opacity-15" style={{ ...pos, transform: `rotate(${rotate}deg)` }} />
        ))}
        <div className="relative mx-auto flex w-full max-w-[1080px] flex-col gap-5 px-5">
          <div className="flex items-center justify-center gap-4">
            <Photo src={dog.photoUrl ?? undefined} alt={`${dog.name} 사진`} dog className="h-14 w-14 shrink-0 rounded-full ring-1 ring-primary-container ring-offset-2" />
            <h1 className="text-[22px] font-bold tracking-tight">
              {dog.name}, 오늘은 뭘 했나요? <span className="text-primary-container">한 줄이면 기록이 돼요.</span>
            </h1>
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault()
              submitMemo()
            }}
            className="flex items-center rounded-md border border-primary-container bg-surface-container-lowest px-5 py-4"
          >
            <div className="hidden shrink-0 border-r border-outline-variant pr-7 pl-2 sm:block">
              <div className="text-label-md font-bold">날짜</div>
              <div className="mt-1 text-body-lg">{dateLabel}</div>
            </div>
            <label className="flex min-w-0 flex-1 flex-col px-7">
              <span className="text-label-md font-bold">오늘 있었던 일</span>
              <input
                value={memo}
                onChange={(e) => setMemo(e.target.value)}
                placeholder="예: 아침에 사료 반만 먹고 산책 20분 했어"
                className="mt-1 w-full bg-transparent text-body-lg placeholder:text-outline focus:outline-none"
              />
            </label>
            <button type="submit" disabled={!memo.trim()} className="shrink-0 rounded-md bg-primary-container px-6 py-3.5 text-label-lg font-bold text-on-primary transition-colors hover:bg-primary disabled:opacity-50">
              기록으로 바꾸기
            </button>
          </form>
        </div>
      </section>

      {/* 2. 바로가기 */}
      <nav aria-label="바로가기" className="mx-auto grid w-full max-w-[1080px] grid-cols-3 gap-y-6 px-5 py-8 text-center sm:grid-cols-6">
        {SHORTCUTS.map(({ to, label, icon }) => (
          <Link key={to} to={to} className="flex flex-col items-center gap-2 text-label-lg font-medium transition-colors hover:text-primary-container [&:hover_.kk-icon]:-translate-y-0.5">
            <span className="flex h-10 items-center justify-center">
              {icon === 'bone' ? <BoneIcon className="h-10 w-10" /> : <Icon name={icon} className="text-[36px]! font-light" />}
            </span>
            {label}
          </Link>
        ))}
      </nav>

      {/* 3. 오늘 하루 요약 · 권장량 · 근처 장소 */}
      <section className="flex-1 bg-surface-container-low py-8">
        <div className="mx-auto grid w-full max-w-[1080px] grid-cols-1 gap-5 px-5 lg:grid-cols-[1.35fr_1fr_1fr]">
          <div className={lowCard}>
            <div className="flex items-baseline justify-between">
              <h2 className="text-body-lg font-bold">오늘 {dog.name}의 하루</h2>
              <button type="button" onClick={() => setShowFamily(true)} className={moreLink}>
                가족 {family?.members.length ?? 1}명이 함께 봐요 ›
              </button>
            </div>
            <ul className="flex flex-col gap-3">
              {DAILY.map((item) => {
                const done = records.filter((r) => r.category === item.category)
                return (
                  <li key={item.category} className="flex items-center gap-3">
                    <span className="w-10 text-label-lg font-semibold">{item.label}</span>
                    <div className="flex gap-2">
                      {Array.from({ length: SLOTS }, (_, i) => {
                        const record = done[i]
                        const who = record ? `${record.createdByNickname ?? '가족'} ${record.createdAt.slice(11, 16)}` : ''
                        return (
                          <button
                            key={i}
                            type="button"
                            disabled={busy === item.category}
                            aria-pressed={!!record}
                            aria-label={record ? `${item.label} ${i + 1}번째: ${who} 기록됨, 누르면 되돌리기` : `${item.label} ${i + 1}번째 기록하기`}
                            title={record ? `${who} · 누르면 되돌려요` : '누르면 기록돼요'}
                            onClick={() => (record ? undo(done[done.length - 1], item.label) : add(item))}
                            className={`flex h-10 w-10 items-center justify-center rounded-md border transition-colors disabled:opacity-60 ${
                              record ? 'border-primary-container bg-primary-container text-on-primary' : 'border-outline-variant text-outline-variant hover:border-primary-container hover:text-primary-container'
                            }`}
                          >
                            <Icon name={item.icon} filled={!!record} className="text-[22px]" />
                          </button>
                        )
                      })}
                    </div>
                    <span className="ml-auto text-label-md text-outline">
                      {done.length ? `${done.length}번 · ${done[done.length - 1].createdByNickname ?? '가족'}` : '아직 없어요'}
                    </span>
                  </li>
                )
              })}
            </ul>
            <div className="mt-auto flex items-center justify-between border-t border-outline-variant pt-3 text-label-sm text-outline">
              아이콘을 누르면 기록되고 가족에게도 보여요
              <Link to={`/records?date=${today}`} className={moreLink}>
                자세히 보기 ›
              </Link>
            </div>
          </div>

          <div className={lowCard}>
            <h2 className="text-body-lg font-bold">오늘 권장량</h2>
            <div className="flex divide-x divide-outline-variant">
              <div className="flex-1 pr-4">
                <div className="text-[22px] leading-tight font-bold">{nutrition ? `약 ${nutrition.foodGrams}g` : '-'}</div>
                <div className="text-label-md text-outline">사료</div>
              </div>
              <div className="flex-1 pl-4">
                <div className="text-[22px] leading-tight font-bold">{nutrition ? `${nutrition.waterMinMl}~${nutrition.waterMaxMl}ml` : '-'}</div>
                <div className="text-label-md text-outline">물</div>
              </div>
            </div>
            <p className="text-body-sm text-on-surface-variant">{tipOf(dog)}</p>
            <div className="mt-auto flex items-center justify-between border-t border-outline-variant pt-3">
              <Link to="/nutrition" className={moreLink}>
                영양 가이드 ›
              </Link>
              <Link to="/course" className={moreLink}>
                오늘 코스 추천받기 ›
              </Link>
            </div>
          </div>

          <div className={lowCard}>
            <div className="flex items-baseline justify-between">
              <h2 className="text-body-lg font-bold">근처 함께 갈 수 있는 곳</h2>
              <Link to="/map" className={moreLink}>
                더 보기 ›
              </Link>
            </div>
            {nearby.length === 0 ? (
              <p className="py-4 text-center text-body-sm text-outline">킁킁, 근처 장소를 찾는 중이에요</p>
            ) : (
              <ul className="flex flex-col divide-y divide-outline-variant">
                {nearby.map((p) => (
                  <li key={p.id}>
                    <Link to={`/map?placeId=${p.id}`} className="flex items-center justify-between gap-3 py-2.5 transition-colors hover:text-primary-container">
                      <span className="truncate font-medium">{p.name}</span>
                      <span className="shrink-0 text-label-md text-outline">{p.distanceM != null ? formatDistance(p.distanceM) : p.categoryLabel}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </section>

      {showFamily && family && <FamilyMembersModal family={family} myId={me?.id} onClose={() => setShowFamily(false)} />}
    </div>
  )
}
