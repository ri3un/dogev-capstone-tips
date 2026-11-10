import { useEffect, useRef, useState } from 'react'
import { Link, NavLink } from 'react-router'
import Icon from '../common/Icon'
import Photo from '../common/Photo'
import { adminApi, authApi } from '../../api/endpoints'
import { logout } from '../../api/session'
import { useApi } from '../../hooks/useApi'
import { useCurrentDog } from '../../hooks/useCurrentDog'

const NAV = [
  { to: '/', label: '홈' },
  { to: '/map', label: '장소 지도' },
  { to: '/course', label: '코스 추천' },
  { to: '/consult', label: 'AI 상담' },
  { to: '/records', label: '건강 기록' },
]

const menuOf = (dogId?: number) => [
  { to: '/my', icon: 'person', label: '마이페이지' },
  { to: dogId ? `/dogs/${dogId}/edit` : '/dogs/new', icon: 'edit_note', label: '반려견 정보 수정' },
  { to: '/my/family', icon: 'group', label: '가족 그룹' },
  { to: '/questions', icon: 'help', label: '수의사 질문 메모' },
]
const ADMIN_ITEM = { to: '/admin/edit-requests', icon: 'admin_panel_settings', label: '수정 요청 관리' }
const ADMIN_MEMBERS = { to: '/admin/members', icon: 'manage_accounts', label: '회원 현황' }

export default function AppHeader() {
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const { data: me } = useApi(() => authApi.me(), [])
  const { dog } = useCurrentDog()
  const isAdmin = me?.role === 'ADMIN'
  // 관리자만: 대기 중인 수정 요청 건수 (화면 들어올 때 조회, 실시간 아님)
  const { data: pending } = useApi(() => (isAdmin ? adminApi.requests('PENDING') : null), [isAdmin])
  const PENDING = pending?.length ?? 0
  const MENU = menuOf(dog?.id)

  // 메뉴 밖을 누르거나 Esc를 누르면 닫음 (메뉴 항목을 누르면 onClick으로 닫음)
  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => !menuRef.current?.contains(e.target as Node) && setOpen(false)
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  return (
    <header className="fixed top-0 left-0 z-50 w-full border-b border-outline-variant bg-surface">
      <div className="mx-auto flex h-20 max-w-[1440px] items-center gap-14 px-10">
        {/* 로고: 팀 로고가 나오면 이 자리만 바꾸면 됨 */}
        <Link to="/" className="flex items-end gap-1 text-[26px] leading-none font-bold tracking-tight">
          킁킁
          <span className="mb-1 h-1.5 w-1.5 rounded-full bg-primary-container" />
        </Link>

        {/* 메뉴: 글자만, 지금 화면은 밑줄 */}
        <nav className="hidden flex-1 items-stretch gap-10 self-stretch md:flex">
          {NAV.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex items-center border-b-[3px] pt-[3px] text-[17px] font-bold transition-colors ${
                  isActive ? 'border-primary-container text-primary-container' : 'border-transparent text-on-surface hover:text-primary-container'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        {/* 프로필 메뉴: 반려견 사진을 누르면 열리고, 밖을 누르면 닫힘 */}
        <div ref={menuRef} className="relative ml-auto">
          <button
            type="button"
            aria-label="반려견 프로필 메뉴"
            aria-expanded={open}
            onClick={() => setOpen((v) => !v)}
            className="flex items-center gap-2 rounded-md py-1 pr-1.5 pl-1 transition-colors hover:bg-surface-container-low"
          >
            <span className="relative">
              <Photo src={dog?.photoUrl ?? undefined} alt={dog ? `${dog.name} 사진` : '반려견 사진'} dog className="h-9 w-9 rounded-full ring-1 ring-primary-container" />
              {isAdmin && PENDING > 0 && <span aria-label={`대기 중인 수정 요청 ${PENDING}건`} className="absolute -top-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-error ring-2 ring-surface" />}
            </span>
            {dog && <span className="text-label-lg font-semibold">{dog.name}</span>}
            {/* 햄버거 아이콘: 열리면 X로 바뀜 */}
            <Icon name={open ? 'close' : 'menu'} className="ml-1 text-[26px]! text-on-surface" />
          </button>
          <div className={`absolute top-full right-0 z-50 mt-2 w-52 rounded-md border border-outline-variant bg-surface-container-lowest py-2 shadow-[0_8px_24px_-8px_rgba(51,34,22,0.18)] transition-all duration-200 ${open ? 'opacity-100' : 'pointer-events-none invisible opacity-0'}`}>
            {[...MENU, ...(isAdmin ? [ADMIN_ITEM, ADMIN_MEMBERS] : [])].map(({ to, icon, label }) => (
              <Link key={to} to={to} onClick={() => setOpen(false)} className="flex items-center gap-2 px-4 py-2.5 text-label-md text-on-surface-variant transition-colors hover:bg-surface-container-low hover:text-on-surface">
                <Icon name={icon} className="text-[18px] text-primary" />
                {label}
                {to === ADMIN_ITEM.to && PENDING > 0 && <span className="ml-auto rounded-full bg-error px-1.5 text-[11px] font-bold text-white">{PENDING}</span>}
              </Link>
            ))}
            <div className="mx-3 my-1.5 border-t border-surface-container-high" />
            <Link to="/login" onClick={() => { logout(); setOpen(false) }} className="flex items-center gap-2 px-4 py-2.5 text-label-md text-outline transition-colors hover:bg-error-container/40 hover:text-error">
              <Icon name="logout" className="text-[18px]" />
              로그아웃
            </Link>
          </div>
        </div>
      </div>
    </header>
  )
}
