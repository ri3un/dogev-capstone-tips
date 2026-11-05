import { Link, NavLink } from 'react-router'
import Icon from '../common/Icon'

const NAV = [
  { to: '/', label: '홈' },
  { to: '/map', label: '장소 지도' },
  { to: '/course', label: '코스 추천' },
  { to: '/consult', label: 'AI 상담' },
  { to: '/records', label: '건강 기록' },
]

// TODO: dogStore 연결 후 실제 반려견 id로 바꿉니다.
const MENU = [
  { to: '/my', icon: 'person', label: '마이페이지' },
  { to: '/dogs/1/edit', icon: 'edit_note', label: '반려견 정보 수정' },
  { to: '/my/family', icon: 'group', label: '가족 그룹' },
  { to: '/questions', icon: 'help', label: '수의사 질문 메모' },
]

export default function AppHeader() {
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

        {/* 프로필 메뉴: 마우스를 올리면 열립니다. */}
        <div className="group relative">
          <button
            type="button"
            aria-label="반려견 프로필 메뉴"
            className="flex items-center gap-2 rounded-full p-1 pr-2.5 pl-1.5 transition-all hover:bg-surface-container-high/60"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-secondary-container shadow-[0_2px_8px_rgba(74,52,38,0.08)]">
              <Icon name="pets" className="text-[18px] text-primary" />
            </span>
            <Icon name="expand_more" className="text-[18px] text-outline transition-transform group-hover:translate-y-0.5 group-hover:text-on-surface" />
          </button>
          <div className="pointer-events-none absolute top-full right-0 z-50 mt-2 w-52 rounded-xl border border-outline-variant/30 bg-surface-container-lowest py-2 opacity-0 shadow-[0_16px_40px_-8px_rgba(74,52,38,0.12)] transition-all duration-200 group-hover:pointer-events-auto group-hover:opacity-100">
            {MENU.map(({ to, icon, label }) => (
              <Link key={to} to={to} className="flex items-center gap-2 px-4 py-2.5 text-label-md text-on-surface-variant transition-colors hover:bg-surface-container-low hover:text-on-surface">
                <Icon name={icon} className="text-[18px] text-primary" />
                {label}
              </Link>
            ))}
            <div className="mx-3 my-1.5 border-t border-surface-container-high" />
            {/* TODO: authStore 로그아웃 처리 후 이동 */}
            <Link to="/login" className="flex items-center gap-2 px-4 py-2.5 text-label-md text-outline transition-colors hover:bg-error-container/40 hover:text-error">
              <Icon name="logout" className="text-[18px]" />
              로그아웃
            </Link>
          </div>
        </div>
      </div>
    </header>
  )
}
