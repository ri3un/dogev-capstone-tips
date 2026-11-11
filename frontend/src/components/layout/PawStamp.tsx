import { useEffect } from 'react'

const PAW = '<svg viewBox="0 0 24 24" fill="currentColor"><ellipse cx="5.3" cy="10.6" rx="1.9" ry="2.4"/><ellipse cx="9.4" cy="6.2" rx="1.9" ry="2.5"/><ellipse cx="14.6" cy="6.2" rx="1.9" ry="2.5"/><ellipse cx="18.7" cy="10.6" rx="1.9" ry="2.4"/><path d="M12 12.6c-3.2 0-5.8 2.4-5.8 4.9 0 1.7 1.4 2.6 3 2.6 1.1 0 1.8-.5 2.8-.5s1.7.5 2.8.5c1.6 0 3-.9 3-2.6 0-2.5-2.6-4.9-5.8-4.9z"/></svg>'

// 화면을 누르면 그 자리에 발바닥이 찍혔다가 1초 뒤 사라짐 (입력칸·지도 위에서는 찍지 않음)
export default function PawStamp() {
  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if ((e.target as HTMLElement).closest('input, textarea, select, [data-no-paw]')) return
      const el = document.createElement('div')
      el.className = 'kk-stamp'
      el.style.left = `${e.clientX}px`
      el.style.top = `${e.clientY}px`
      el.innerHTML = PAW
      document.body.appendChild(el)
      setTimeout(() => el.remove(), 1000)
    }
    document.addEventListener('click', onClick)
    return () => document.removeEventListener('click', onClick)
  }, [])
  return null
}
