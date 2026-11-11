import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router'
import { finishLogin } from '../api/session'

// 소셜 로그인 후 백엔드가 FRONT_URL/oauth/callback#token=... 으로 보내면 토큰을 저장하고 이동
export default function OAuthCallbackPage() {
  const navigate = useNavigate()
  const done = useRef(false) // 개발 모드(StrictMode)에서 effect가 두 번 돌아도 한 번만 처리

  useEffect(() => {
    if (done.current) return
    done.current = true
    const token = new URLSearchParams(location.hash.slice(1)).get('token')
    if (!token) {
      navigate('/login', { replace: true })
      return
    }
    history.replaceState(null, '', location.pathname) // 주소창에서 토큰 지우기
    finishLogin(token, navigate)
  }, [navigate])

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <span className="h-10 w-10 animate-spin rounded-md border-4 border-secondary-container border-t-primary-container" />
      <p className="text-label-md text-on-surface-variant">로그인하는 중이에요</p>
    </div>
  )
}
