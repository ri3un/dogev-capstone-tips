import { useState } from 'react'
import { DogFace } from './Paw'

// 이미지가 없거나 깨지면 배경색만 보여 줍니다. dog 를 주면 강아지 얼굴 선 그림을 대신 보여 줌
export default function Photo({ src, alt, className = '', dog }: { src?: string; alt: string; className?: string; dog?: boolean }) {
  const [failed, setFailed] = useState(false)
  if (!src || failed)
    return (
      <div role="img" aria-label={alt} className={`flex items-center justify-center bg-surface-container ${className}`}>
        {dog && <DogFace className="h-2/3 w-2/3 text-on-surface" />}
      </div>
    )
  return <img src={src} alt={alt} onError={() => setFailed(true)} className={`object-cover ${className}`} />
}
