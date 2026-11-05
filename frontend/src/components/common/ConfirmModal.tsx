import { card, ghostBtn, primaryBtn } from './styles'

interface Props {
  title: string
  body: string
  confirmLabel: string
  danger?: boolean
  onConfirm: () => void
  onClose: () => void
}

// 되돌리기 어려운 동작(내보내기, 관리자 넘기기, 나가기) 전에 한 번 더 묻는 창
export default function ConfirmModal({ title, body, confirmLabel, danger, onConfirm, onClose }: Props) {
  return (
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-[100] flex items-center justify-center bg-on-surface/30 px-5 backdrop-blur-sm" onClick={onClose}>
      <div className={`${card} w-full max-w-[400px] p-7`} onClick={(e) => e.stopPropagation()}>
        <h2 className="text-headline-sm font-bold">{title}</h2>
        <p className="mt-2 leading-relaxed text-on-surface-variant">{body}</p>
        <div className="mt-6 flex justify-end gap-2">
          <button type="button" onClick={onClose} className={`${ghostBtn} h-11`}>
            취소
          </button>
          <button type="button" onClick={onConfirm} className={`${primaryBtn} h-11 ${danger ? 'bg-error hover:bg-on-error-container' : ''}`}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
