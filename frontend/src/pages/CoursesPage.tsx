import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import { card } from '../components/common/styles'
import { SAVED_COURSES } from '../mocks/course'

// 저장한 코스: 추천받아 저장한 코스와 다녀온 날짜 (누르면 코스 상세)
export default function CoursesPage() {
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
        {SAVED_COURSES.map((c) => (
          <li key={c.id}>
            <Link to={`/course/plan?courseId=${c.courseId}`} className={`${card} flex items-center gap-4 p-5 transition-all hover:-translate-y-0.5 hover:shadow-[0_12px_30px_-8px_rgba(74,52,38,0.12)]`}>
              <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-secondary-container/60">
                <Icon name="route" className="text-[24px] text-primary" />
              </span>
              <div className="flex-1">
                <p className="font-bold">{c.stops.join(' → ')}</p>
                <p className="mt-1 text-label-sm text-outline">
                  총 {c.distanceKm}km · {c.savedBy} 저장
                </p>
              </div>
              {c.visitedAt ? (
                <span className="shrink-0 rounded-full bg-status-ok px-2.5 py-0.5 text-label-sm font-bold text-white">{c.visitedAt.slice(5).replace('-', '/')} 다녀옴</span>
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
