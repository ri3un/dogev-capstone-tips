import type { NavigateFunction } from 'react-router'
import { TOKEN_KEY } from './client'
import { dogApi } from './endpoints'

/** 로그인 성공 뒤 공통 처리: 토큰 저장 → 초대 링크로 왔으면 초대 화면 → 반려견이 없으면 온보딩 */
export async function finishLogin(token: string, navigate: NavigateFunction) {
  localStorage.setItem(TOKEN_KEY, token)
  const invite = sessionStorage.getItem('inviteCode')
  if (invite) {
    sessionStorage.removeItem('inviteCode')
    navigate(`/invite/${invite}`, { replace: true })
    return
  }
  const dogs = await dogApi.list().catch(() => [])
  navigate(dogs.length ? '/' : '/onboarding', { replace: true })
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('dogId')
}
