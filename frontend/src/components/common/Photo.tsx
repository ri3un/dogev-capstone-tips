import { useState } from 'react'

// 이미지가 없거나 깨지면 배경색만 보여 줍니다(Stitch 임시 이미지 대비).
export default function Photo({ src, alt, className = '' }: { src?: string; alt: string; className?: string }) {
  const [failed, setFailed] = useState(false)
  if (!src || failed) return <div role="img" aria-label={alt} className={`bg-surface-container ${className}`} />
  return <img src={src} alt={alt} onError={() => setFailed(true)} className={`object-cover ${className}`} />
}
