import { useEffect, useState } from 'react'
import { PawIcon } from './Paw'

const EVENT = 'kk-toast'

/** 화면 아래에 잠깐 뜨는 알림. 어디서든 toast('킁킁, 기록했어요!') 로 부릅니다 */
export function toast(message: string) {
  window.dispatchEvent(new CustomEvent<string>(EVENT, { detail: message }))
}

// AppLayout 에 한 번만 둡니다: toast() 가 보낸 글을 2.5초 동안 보여 줌
export function Toaster() {
  const [message, setMessage] = useState('')

  useEffect(() => {
    let timer: number
    const onToast = (e: Event) => {
      setMessage((e as CustomEvent<string>).detail)
      clearTimeout(timer)
      timer = window.setTimeout(() => setMessage(''), 2500)
    }
    window.addEventListener(EVENT, onToast)
    return () => {
      window.removeEventListener(EVENT, onToast)
      clearTimeout(timer)
    }
  }, [])

  if (!message) return null
  return (
    <div role="status" className="pointer-events-none fixed inset-x-0 bottom-10 z-[150] flex justify-center">
      <div className="kk-toast inline-flex items-center gap-2 rounded-md bg-on-surface px-5 py-3 text-label-lg font-bold text-white">
        <PawIcon className="h-4 w-4" />
        {message}
      </div>
    </div>
  )
}
