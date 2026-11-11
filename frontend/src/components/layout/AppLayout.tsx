import { Link, Outlet } from 'react-router'
import { Toaster } from '../common/toast'
import AppHeader from './AppHeader'
import PawStamp from './PawStamp'

// 로그인 뒤 화면 공통 틀: 헤더 + 본문 + 푸터 (+ 저장 알림, 클릭 발자국)
export default function AppLayout() {
  return (
    <>
      <AppHeader />
      <main className="min-h-[calc(100vh-80px)] w-full pt-20">
        <Outlet />
      </main>
      <footer className="w-full border-t border-outline-variant bg-surface-container-low py-6">
        <p className="mx-auto max-w-[1440px] px-10 text-center text-body-sm text-outline">
          © 2026 킁킁 (Keung-Keung) · 소중한 반려견과의 건강하고 행복한 일상 ·{' '}
          <Link to="/terms" className="transition-colors hover:text-primary">이용약관</Link> ·{' '}
          <Link to="/privacy" className="transition-colors hover:text-primary">개인정보처리방침</Link>
        </p>
      </footer>
      <Toaster />
      <PawStamp />
    </>
  )
}
