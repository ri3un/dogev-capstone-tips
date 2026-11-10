import type { ReactNode } from 'react'
import { DogFace } from './Paw'

// 보여 줄 내용이 없을 때: 강아지 얼굴 + 한 줄 안내
export default function EmptyState({ title, children, className = '' }: { title: string; children?: ReactNode; className?: string }) {
  return (
    <div className={`flex flex-col items-center gap-2 py-10 text-center ${className}`}>
      <DogFace className="h-12 w-12 text-on-surface" />
      <p className="text-label-lg font-bold text-on-surface">{title}</p>
      {children && <p className="text-body-sm text-outline">{children}</p>}
    </div>
  )
}
