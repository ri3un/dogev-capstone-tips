import { useEffect, useState } from 'react'
import { getHealth } from '../api/health'

// 토대 확인용 첫 화면: 백엔드 /api/health 연결 상태를 보여 줍니다.
export default function HomePage() {
  const [status, setStatus] = useState('확인 중…')

  useEffect(() => {
    getHealth()
      .then((data) => setStatus(`서버 연결됨 (${data.status})`))
      .catch((e) => setStatus(`서버 연결 실패: ${e.message}`))
  }, [])

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col items-center justify-center gap-4 p-6">
      <h1 className="text-2xl font-bold">개발하개 🐶</h1>
      <p className="text-gray-600">반려견 건강관리 · 외출 코스 추천</p>
      <p className="rounded-lg bg-gray-100 px-4 py-2 text-sm">{status}</p>
    </main>
  )
}
