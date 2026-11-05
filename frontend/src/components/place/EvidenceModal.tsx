import Icon from '../common/Icon'
import { card } from '../common/styles'
import { EVIDENCE } from '../../mocks/requests'

// 근거 블로그 목록(출처) 모달: 원문은 싣지 않고 제목·출처·판정 근거만 보여 줌
export default function EvidenceModal({ placeName, total, onClose }: { placeName: string; total: number; onClose: () => void }) {
  // TODO: GET /api/places/{id}/evidences
  return (
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-[100] flex items-center justify-center bg-on-surface/30 px-5 backdrop-blur-sm" onClick={onClose}>
      <div className={`${card} flex max-h-[80vh] w-full max-w-[520px] flex-col`} onClick={(e) => e.stopPropagation()}>
        <div className="flex items-start justify-between gap-3 border-b border-surface-container-high/60 p-6 pb-4">
          <div>
            <p className="text-label-md text-outline">{placeName}</p>
            <h2 className="text-headline-sm font-bold">판정 근거 {total}건</h2>
            <p className="text-label-sm text-outline">최근 {EVIDENCE.length}건을 보여 드려요</p>
          </div>
          <button type="button" aria-label="닫기" onClick={onClose} className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-surface-container-low">
            <Icon name="close" className="text-[20px]" />
          </button>
        </div>

        <ul className="flex flex-col gap-3 overflow-y-auto p-6 pt-4">
          {EVIDENCE.map((e) => (
            <li key={e.id} className="rounded-xl bg-surface-container-low p-4">
              <div className="mb-1 flex items-center gap-2 text-label-sm text-outline">
                <span className="rounded-full bg-surface-container-high px-2 py-0.5 font-semibold text-on-surface-variant">{e.source}</span>
                {e.date}
              </div>
              <a href={e.url} target="_blank" rel="noreferrer" className="flex items-center gap-1 font-semibold hover:text-primary hover:underline">
                {e.title}
                <Icon name="open_in_new" className="text-[15px] text-outline" />
              </a>
              <div className="mt-2 flex flex-wrap gap-1.5">
                {e.findings.map((f) => (
                  <span key={f} className="rounded-full bg-secondary-container/60 px-2.5 py-0.5 text-label-sm font-semibold text-on-secondary-container">
                    {f}
                  </span>
                ))}
              </div>
            </li>
          ))}
        </ul>

        <p className="flex items-center gap-1.5 border-t border-surface-container-high/60 px-6 py-4 text-label-sm text-outline">
          <Icon name="info" className="text-[16px]" />
          여러 글의 판정을 다수결로 합쳤어요. 근거가 없으면 '정보 없음'으로 표시해요.
        </p>
      </div>
    </div>
  )
}
