import { Link } from 'react-router'
import { DogFace } from '../components/common/Paw'
import { primaryBtn } from '../components/common/styles'

// 없는 주소로 들어왔을 때
export default function NotFoundPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-3 text-center">
      <DogFace className="h-16 w-16 text-on-surface" />
      <h1 className="text-headline-md font-bold">킁킁… 여기엔 아무 냄새도 안 나요</h1>
      <p className="text-body-sm text-outline">주소가 바뀌었거나 없는 페이지예요.</p>
      <Link to="/" className={`${primaryBtn} mt-3`}>
        홈으로 돌아가기
      </Link>
    </main>
  )
}
