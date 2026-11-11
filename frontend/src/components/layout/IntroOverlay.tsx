import { useEffect, useState } from 'react'

const SEEN_KEY = 'kungkung-intro-seen'

// 첫 화면에서 강아지가 뛰어오는 투명 영상을 반투명 배경 위에 한 번 보여줍니다.
export default function IntroOverlay() {
  const [show, setShow] = useState(() => {
    try {
      // 이번 탭에서 이미 봤거나, 움직임 줄이기 설정이면 건너뜀
      if (sessionStorage.getItem(SEEN_KEY)) return false
      if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return false
    } catch {
      return true
    }
    return true
  })
  const [leaving, setLeaving] = useState(false)

  useEffect(() => {
    if (!show) return
    try {
      sessionStorage.setItem(SEEN_KEY, '1')
    } catch {
      // 저장이 막힌 브라우저면 다음에도 다시 보여줌
    }
  }, [show])

  // 페이드아웃(0.5초)이 끝나면 화면에서 제거
  useEffect(() => {
    if (!leaving) return
    const t = setTimeout(() => setShow(false), 500)
    return () => clearTimeout(t)
  }, [leaving])

  if (!show) return null

  const close = () => setLeaving(true)

  return (
    <div
      onClick={close}
      className={`fixed inset-0 z-[100] flex cursor-pointer items-center justify-center bg-cream/70 backdrop-blur-md transition-opacity duration-500 ${leaving ? 'opacity-0' : 'opacity-100'}`}
      aria-label="인트로 건너뛰기"
      role="button"
    >
      <video
        src="/intro/dog-intro.webm"
        autoPlay
        muted
        playsInline
        onEnded={() => setTimeout(close, 400)}
        onError={close}
        className="w-[min(960px,90vw)]"
      />
      <span className="absolute bottom-8 text-label-md text-outline">클릭하면 건너뛰어요</span>
    </div>
  )
}
