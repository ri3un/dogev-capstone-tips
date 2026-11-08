import { Link } from 'react-router'
import { adminApi, authApi, familyApi } from '../api/endpoints'
import { ageText } from '../api/format'
import { logout } from '../api/session'
import Icon from '../components/common/Icon'
import Photo from '../components/common/Photo'
import { card } from '../components/common/styles'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'

const PROVIDER = {
  KAKAO: { label: '카카오', cls: 'bg-[#FEE500] text-black/85' },
  NAVER: { label: '네이버', cls: 'bg-[#03C75A] text-white' },
  DEV: { label: '개발용', cls: 'bg-surface-container-high text-on-surface-variant' },
}

// 마이페이지: 내 정보, 반려견, 메뉴, 로그아웃
export default function MyPage() {
  const { data: ME } = useApi(() => authApi.me(), [])
  const { data: FAMILY } = useApi(() => familyApi.get(), [])
  const { dogs } = useCurrentDog()
  const isAdmin = ME?.role === 'ADMIN'
  const { data: pendingList } = useApi(() => (isAdmin ? adminApi.requests('PENDING') : null), [isAdmin])
  if (!ME) return null

  const MENU = [
    { to: '/my/family', icon: 'group', label: '가족 관리', sub: FAMILY ? `${FAMILY.name} · ${FAMILY.members.length}명` : undefined },
    { to: '/questions', icon: 'help', label: '수의사 질문 메모' },
    { to: '/report', icon: 'description', label: '건강 리포트' },
    { to: '/nutrition', icon: 'restaurant', label: '영양 가이드' },
    { to: '/courses', icon: 'route', label: '저장한 코스' },
    { to: '/my/edit-requests', icon: 'flag', label: '내 장소 수정 요청' },
  ]
  // 관리자에게만 보이는 메뉴 (대기 건수 배지)
  const ADMIN_MENU = { to: '/admin/edit-requests', icon: 'admin_panel_settings', label: '수정 요청 관리', sub: `대기 ${pendingList?.length ?? 0}건` }
  const provider = PROVIDER[ME.provider]
  return (
    <div className="mx-auto flex w-full max-w-[760px] flex-col gap-6 px-5 py-9 lg:px-6">
      <h1 className="text-headline-lg font-bold tracking-tight">마이페이지</h1>

      <section className={`${card} flex items-center gap-4 p-6`}>
        <span className="flex h-14 w-14 items-center justify-center rounded-full bg-secondary-container text-headline-sm font-bold text-on-secondary-container">{ME.nickname[0]}</span>
        <div className="flex-1">
          <p className="text-headline-sm font-bold">{ME.nickname}</p>
          <span className={`mt-1 inline-block rounded-full px-2.5 py-0.5 text-label-sm font-semibold ${provider.cls}`}>{provider.label} 로그인</span>
        </div>
        {FAMILY?.myRole === 'OWNER' && <span className="rounded-full bg-primary-container px-3 py-1 text-label-sm font-bold text-white">가족 관리자</span>}
      </section>

      <section className={`${card} p-6`}>
        <h2 className="mb-3 text-body-lg font-bold">우리 반려견</h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {dogs.map((DOG) => (
            <Link key={DOG.id} to={`/dogs/${DOG.id}/edit`} className="flex items-center gap-3 rounded-xl bg-surface-container-low p-3 hover:bg-surface-container">
              <Photo src={DOG.photoUrl ?? undefined} alt={DOG.name} className="h-14 w-14 rounded-full" />
              <div className="flex-1">
                <p className="font-bold">{DOG.name}</p>
                <p className="text-label-sm text-outline">
                  {DOG.breed} · {ageText(DOG.ageMonths)} · {DOG.weightKg}kg
                </p>
              </div>
              <Icon name="chevron_right" className="text-[20px] text-outline" />
            </Link>
          ))}
          <Link to="/dogs/new" className="flex items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-outline-variant p-3 text-label-lg font-semibold text-on-surface-variant hover:border-primary-container hover:text-primary">
            <Icon name="add" className="text-[20px]" />
            반려견 추가
          </Link>
        </div>
      </section>

      <section className={`${card} p-2`}>
        {[...MENU, ...(isAdmin ? [ADMIN_MENU] : [])].map((m) => (
          <Link key={m.to} to={m.to} className="flex items-center gap-3 rounded-xl px-4 py-3.5 hover:bg-surface-container-low">
            <Icon name={m.icon} className="text-[22px] text-primary" />
            <span className="flex-1 font-semibold">{m.label}</span>
            {m.sub && <span className="text-label-sm text-outline">{m.sub}</span>}
            <Icon name="chevron_right" className="text-[20px] text-outline" />
          </Link>
        ))}
      </section>

      <Link to="/login" onClick={logout} className="flex items-center justify-center gap-1.5 rounded-full py-3 text-label-lg font-semibold text-outline hover:bg-error-container/40 hover:text-error">
        <Icon name="logout" className="text-[20px]" />
        로그아웃
      </Link>
    </div>
  )
}
