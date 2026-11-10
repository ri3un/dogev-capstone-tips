import { useEffect, useRef, useState } from 'react'
import { Link, NavLink } from 'react-router'
import Icon from '../common/Icon'
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
    <header className="fixed top-0 left-0 z-50 w-full bg-surface/90 shadow-[0_1px_8px_rgba(74,52,38,0.04)] backdrop-blur-xl">
      <div className="mx-auto flex h-20 max-w-[1440px] items-center justify-between px-10">
        <Link to="/" className="group flex items-center gap-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary-container/60 transition-transform group-hover:scale-105">
            <Icon name="pets" className="text-[24px] text-primary" />
          </div>
          <span className="text-headline-lg font-bold tracking-tight transition-colors group-hover:text-primary">킁킁</span>
        </Link>

        <nav className="hidden items-center gap-1 rounded-full bg-surface-container-low/80 p-1.5 md:flex">
          {NAV.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `rounded-full px-5 py-2 text-label-lg transition-all ${
                  isActive
                    ? 'bg-surface-container-high font-bold text-primary shadow-[0_2px_12px_rgba(74,52,38,0.06)]'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-primary'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        {/* 프로필 메뉴: 발바닥을 누르면 열리고, 밖을 누르면 닫힘 */}
        <div ref={menuRef} className="relative">
          <button
            type="button"
            aria-label="반려견 프로필 메뉴"
            aria-expanded={open}
            onClick={() => setOpen((v) => !v)}
            className="flex items-center gap-2 rounded-full p-1 pr-2.5 pl-1.5 transition-all hover:bg-surface-container-high/60"
          >
            <span className="relative flex h-8 w-8 items-center justify-center rounded-full bg-secondary-container shadow-[0_2px_8px_rgba(74,52,38,0.08)]">
              <Icon name="pets" className="text-[18px] text-primary" />
              {isAdmin && PENDING > 0 && <span aria-label={`대기 중인 수정 요청 ${PENDING}건`} className="absolute -top-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-error ring-2 ring-surface" />}
            </span>
            <Icon name="expand_more" className={`text-[18px] text-outline transition-transform ${open ? 'rotate-180 text-on-surface' : ''}`} />
          </button>
          <div className={`absolute top-full right-0 z-50 mt-2 w-52 rounded-xl border border-outline-variant/30 bg-surface-container-lowest py-2 shadow-[0_16px_40px_-8px_rgba(74,52,38,0.12)] transition-all duration-200 ${open ? 'opacity-100' : 'pointer-events-none invisible opacity-0'}`}>
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
