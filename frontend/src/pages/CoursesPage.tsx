import { Link, Navigate } from 'react-router'
import { courseApi, familyApi } from '../api/endpoints'
import Icon from '../components/common/Icon'
import { card } from '../components/common/styles'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'

// 저장한 코스: 추천받아 저장한 코스와 다녀온 날짜 (누르면 코스 상세)
export default function CoursesPage() {
  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const { data: courses } = useApi(() => (dogId ? courseApi.list(dogId) : null), [dogId])
  const { data: family } = useApi(() => familyApi.get(), [])
  if (loading) return null
  if (!dog) return <Navigate to="/onboarding" replace />

  const nameOf = (id: number) => family?.members.find((m) => m.memberId === id)?.nickname ?? '가족'
  // "2026-11-02T10:00:00" → "11/2"
  const md = (iso: string) => {
    const d = new Date(iso)
    return `${d.getMonth() + 1}/${d.getDate()}`
  }

  return (
    <div className="mx-auto flex w-full max-w-[840px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <p className="mb-1 text-label-md text-secondary">가족이 함께 봐요</p>
          <h1 className="text-headline-lg font-bold tracking-tight">저장한 코스</h1>
        </div>
        <Link to="/course" className="flex items-center gap-1 text-label-lg font-bold text-primary hover:underline">
          새 코스 추천받기 <Icon name="arrow_forward" className="text-[18px]" />
        </Link>
      </div>

      <ul className="flex flex-col gap-3">
        {(courses ?? []).length === 0 && <li className={`${card} py-14 text-center text-label-md text-outline`}>아직 추천받은 코스가 없어요</li>}
        {(courses ?? []).map((c) => (
          <li key={c.id}>
            <Link to={`/course/plan?courseId=${c.id}`} className={`${card} flex items-center gap-4 p-5 transition-all hover:-translate-y-0.5 hover:shadow-[0_12px_30px_-8px_rgba(74,52,38,0.12)]`}>
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-secondary-container/60">
                <Icon name="route" className="text-[24px] text-primary" />
              </span>
              <div className="flex-1">
                <p className="font-bold">{c.stops.map((s) => s.name).join(' → ')}</p>
                <p className="mt-1 text-label-sm text-outline">
                  총 {(c.totalDistanceM / 1000).toFixed(1)}km · {nameOf(c.createdBy)} 저장
                </p>
              </div>
              {c.visitedAt ? (
                <span className="shrink-0 rounded-full bg-status-ok px-2.5 py-0.5 text-label-sm font-bold text-white">{md(c.visitedAt)} 다녀옴</span>
              ) : (
                <span className="shrink-0 rounded-full bg-surface-container px-2.5 py-0.5 text-label-sm text-on-surface-variant">아직 안 감</span>
              )}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
