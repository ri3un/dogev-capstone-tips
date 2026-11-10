import type { CSSProperties } from 'react'

// 킁킁 전용 선 그림: 발바닥과 강아지 얼굴 (글자색을 그대로 따라감)
export function PawIcon({ className = '', style }: { className?: string; style?: CSSProperties }) {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="currentColor" className={`kk-icon ${className}`} style={style}>
      <ellipse cx="5.3" cy="10.6" rx="1.9" ry="2.4" />
      <ellipse cx="9.4" cy="6.2" rx="1.9" ry="2.5" />
      <ellipse cx="14.6" cy="6.2" rx="1.9" ry="2.5" />
      <ellipse cx="18.7" cy="10.6" rx="1.9" ry="2.4" />
      <path d="M12 12.6c-3.2 0-5.8 2.4-5.8 4.9 0 1.7 1.4 2.6 3 2.6 1.1 0 1.8-.5 2.8-.5s1.7.5 2.8.5c1.6 0 3-.9 3-2.6 0-2.5-2.6-4.9-5.8-4.9z" />
    </svg>
  )
}

export function DogFace({ className = '' }: { className?: string }) {
  return (
    <svg aria-hidden viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" className={className}>
      <path d="M14 19c0-6 4.5-10 10-10s10 4 10 10v7c0 6-4.5 11-10 11s-10-5-10-11z" />
      <path d="M15 13c-5-2-9 1-9 7 0 5 2 9 5 9 2 0 3-2 3-4" />
      <path d="M33 13c5-2 9 1 9 7 0 5-2 9-5 9-2 0-3-2-3-4" />
      <circle cx="20" cy="22" r="1.2" fill="currentColor" stroke="none" />
      <circle cx="28" cy="22" r="1.2" fill="currentColor" stroke="none" />
      <ellipse cx="24" cy="27.5" rx="2.3" ry="1.7" fill="currentColor" stroke="none" />
      <path d="M24 29.2v2.300M20.5 31c.8 1.6 2.7 1.6 3.500.5M27.5 31c-.8 1.6-2.7 1.6-3.500.5" />
    </svg>
  )
}

// 뼈다귀 (건강 기록 아이콘)
export function BoneIcon({ className = '' }: { className?: string }) {
  return (
    <svg aria-hidden viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" className={`kk-icon ${className}`}>
      <path d="M8 10.5h8a2.3 2.3 0 1 1 3.2 1.5 2.3 2.3 0 1 1-3.2 1.500H8a2.3 2.3 0 1 1-3.2-1.500A2.3 2.3 0 1 1 8 10.5z" />
    </svg>
  )
}
