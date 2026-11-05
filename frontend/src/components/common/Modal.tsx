import type { ReactNode } from 'react'
import Icon from './Icon'
import { card } from './styles'

// 가운데 뜨는 창: 바깥을 누르거나 X를 누르면 닫힘
export default function Modal({ title, onClose, children, width = 'max-w-[460px]' }: { title: string; onClose: () => void; children: ReactNode; width?: string }) {
  return (
    <div role="dialog" aria-modal="true" aria-label={title} className="fixed inset-0 z-[100] flex items-center justify-center bg-on-surface/30 px-5 backdrop-blur-sm" onClick={onClose}>
      <div className={`${card} flex max-h-[85vh] w-full ${width} flex-col overflow-hidden`} onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between border-b border-outline-variant/30 px-6 py-4">
          <h2 className="text-headline-sm font-bold">{title}</h2>
          <button type="button" aria-label="닫기" onClick={onClose} className="flex h-9 w-9 items-center justify-center rounded-full text-outline hover:bg-surface-container-low">
            <Icon name="close" className="text-[20px]" />
          </button>
        </div>
        <div className="overflow-y-auto p-6">{children}</div>
      </div>
    </div>
  )
}
