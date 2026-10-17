import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4">
      <p>페이지를 찾을 수 없습니다.</p>
      <Link to="/" className="text-blue-600 underline">
        처음으로
      </Link>
    </main>
  )
}
