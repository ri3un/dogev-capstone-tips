import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { authApi } from '../api/endpoints'
import { finishLogin } from '../api/session'
import Icon from '../components/common/Icon'
import PuppyIllustration from '../components/login/PuppyIllustration'

// 소셜 로그인 시작 주소 (Spring Security OAuth2, Vite proxy가 백엔드로 넘김)
const OAUTH = {
  kakao: '/oauth2/authorization/kakao',
  naver: '/oauth2/authorization/naver',
}
const socialBtn = 'flex h-[52px] w-full items-center justify-center gap-2.5 rounded-md text-label-lg transition-all duration-150 active:scale-[0.98]'

// 개발 모드(npm run dev)에서만 보이는 로그인: SNS 키 없이 닉네임으로 들어갑니다. 배포 빌드에는 없음
function DevLogin() {
  const navigate = useNavigate()
  const [nickname, setNickname] = useState('')
  const [admin, setAdmin] = useState(false)
  const [error, setError] = useState('')
  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!nickname.trim()) return
    try {
      const { token } = await authApi.devLogin(nickname.trim(), admin)
      await finishLogin(token, navigate)
    } catch (err) {
      setError((err as { message?: string }).message ?? '로그인하지 못했어요')
    }
  }
  return (
    <form onSubmit={submit} className="mt-2 flex flex-col gap-2 rounded-md border border-dashed border-outline-variant p-3 text-left">
      <span className="text-label-sm text-outline">개발용 로그인 (배포에는 없음)</span>
      <input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="닉네임" aria-label="개발용 닉네임" className="rounded-lg bg-surface-container-low px-3 py-2 text-body-sm" />
      <label className="flex items-center gap-2 text-label-sm text-on-surface-variant">
        <input type="checkbox" checked={admin} onChange={(e) => setAdmin(e.target.checked)} /> 관리자로
      </label>
      <button type="submit" className="rounded-lg bg-primary py-2 text-label-md text-on-primary">들어가기</button>
      {error && <p role="alert" className="text-label-sm text-error">{error}</p>}
    </form>
  )
}
// 로그인 (Stitch 07-login): 카카오·네이버 소셜 로그인만 사용
export default function LoginPage() {
  return (
    <div className="flex min-h-screen flex-col justify-between selection:bg-secondary-container selection:text-on-secondary-container">
      <header className="fixed top-0 left-0 z-50 w-full border-b border-outline-variant bg-surface">
        <div className="mx-auto flex h-20 max-w-[1440px] items-center justify-between px-5 lg:px-10">
          <Link to="/login" className="flex items-end gap-1 text-[26px] leading-none font-bold tracking-tight">
            킁킁
            <span className="mb-1 h-1.5 w-1.5 rounded-full bg-primary-container" />
          </Link>
          <span className="inline-flex items-center rounded-md bg-surface-container-low px-3 py-1 text-label-md text-secondary">반려견 케어</span>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-[1440px] flex-1 flex-col items-center justify-center px-5 pt-20 pb-9 lg:px-10">
        <div className="relative flex w-full max-w-[460px] items-center justify-center py-2 sm:py-6">

          <div className="relative flex w-full max-w-[440px] flex-col items-center rounded-md bg-surface-container-lowest px-7 py-9 text-center shadow-[0_12px_40px_-8px_rgba(74,52,38,0.08)] sm:px-9 sm:py-10">
            <div className="mb-5 flex flex-col items-center">
              <div className="flex h-16 w-16 items-center justify-center rounded-md bg-surface-container-low transition-transform duration-300 hover:scale-105">
                <div className="flex h-12 w-12 items-center justify-center rounded-md bg-secondary-container text-on-secondary-container">
                  <Icon name="pets" filled className="text-[28px]" />
                </div>
              </div>
              <h1 className="mt-4 text-headline-xl font-bold tracking-tight text-primary">킁킁</h1>
              <p className="mt-2 max-w-[280px] leading-snug font-medium">
                외출이 기록이 되고,
                <br />
                기록이 다음 외출을 바꿔요
              </p>
            </div>

            <div className="my-2 flex h-[148px] w-full max-w-[260px] items-center justify-center overflow-hidden select-none">
              <PuppyIllustration />
            </div>

            <div className="mt-4 flex w-full flex-col gap-3">
              {/* 카카오·네이버 공식 버튼 색상은 브랜드 가이드 그대로 둡니다. */}
              <a href={OAUTH.kakao} className={`${socialBtn} bg-[#FEE500] text-[#191919] shadow-[0_2px_8px_rgba(254,229,0,0.3)] hover:bg-[#FADA0A]`}>
                <svg className="h-5 w-5 shrink-0 fill-current" viewBox="0 0 24 24">
                  <path d="M12 3C6.477 3 2 6.477 2 10.77c0 2.78 1.874 5.215 4.708 6.558-.207.76-.75 2.75-.86 3.18-.135.536.197.528.414.385.17-.112 2.716-1.844 3.82-2.597.625.088 1.267.135 1.918.135 5.523 0 10-3.477 10-7.77C22 6.477 17.523 3 12 3z" />
                </svg>
                <span className="font-semibold tracking-tight">카카오로 시작하기</span>
              </a>
              <a href={OAUTH.naver} className={`${socialBtn} bg-[#03C75A] text-white shadow-[0_2px_8px_rgba(3,199,90,0.25)] hover:bg-[#02B351]`}>
                <svg className="h-4 w-4 shrink-0 fill-current" viewBox="0 0 24 24">
                  <path d="M16.273 12.845L7.376 0H0v24h7.727V11.155L16.624 24H24V0h-7.727v12.845z" />
                </svg>
                <span className="font-semibold tracking-tight">네이버로 시작하기</span>
              </a>
              {import.meta.env.DEV && <DevLogin />}
            </div>

            <div className="mt-6 flex w-full flex-col items-center pt-5">
              <div className="mb-3 flex items-start justify-center gap-1.5 text-body-sm text-on-surface-variant">
                <Icon name="info" filled className="mt-0.5 text-[16px] text-primary" />
                <span className="break-keep">처음이면 로그인 후 반려견을 등록해요. 별도 회원가입은 없어요.</span>
              </div>
              <div className="flex items-center justify-center gap-2 text-label-sm text-outline">
                <Link to="/terms" className="underline-offset-2 transition-colors hover:text-primary hover:underline">이용약관</Link>
                <span className="text-outline-variant select-none">·</span>
                <Link to="/privacy" className="underline-offset-2 transition-colors hover:text-primary hover:underline">개인정보처리방침</Link>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="w-full bg-surface-container-low/70">
        <p className="mx-auto flex max-w-[1440px] flex-wrap items-center justify-center gap-x-2 gap-y-1 px-5 py-6 text-center text-body-sm text-on-surface-variant lg:px-10">
          <span>© 2026 킁킁 (Keung-Keung)</span>·<span>소중한 반려견과의 건강하고 행복한 일상</span>·
          <Link to="/terms" className="underline decoration-outline-variant underline-offset-4 transition-colors hover:text-primary">이용약관</Link>·
          <Link to="/privacy" className="underline decoration-outline-variant underline-offset-4 transition-colors hover:text-primary">개인정보처리방침</Link>
        </p>
      </footer>
    </div>
  )
}
