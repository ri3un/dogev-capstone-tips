import { useState } from 'react'
import Icon from '../common/Icon'
import Photo from '../common/Photo'
import EditRequestModal from './EditRequestModal'
import SourceModal from './SourceModal'
import { CATEGORY_LABEL, STATUS_BG, STATUS_LABEL, formatDistance, type BadgeTone, type Place } from '../../mocks/places'

const TONE: Record<BadgeTone, string> = {
  green: 'bg-tertiary-container/20 text-tertiary',
  apricot: 'bg-secondary-container/60 text-on-secondary-container',
  caramel: 'bg-surface-container-high text-primary',
}

// 장소 상세 드로어: 지도 오른쪽에 겹쳐 열립니다.
export default function PlaceDrawer({ place, onClose }: { place: Place; onClose: () => void }) {
  const [modal, setModal] = useState<'source' | 'edit' | null>(null)
  const [visited, setVisited] = useState(false)
  return (
    <aside className="absolute top-0 right-0 z-30 flex h-full w-full flex-col border-l border-outline-variant/30 bg-surface-container-lowest shadow-2xl sm:w-[420px]">
      <div className="flex-1 overflow-y-auto">
        <div className="relative h-56 w-full bg-surface-container">
          <Photo src={place.imageUrl} alt={place.name} className="h-full w-full" />
          <button type="button" aria-label="닫기" onClick={onClose} className="absolute top-4 right-4 flex h-9 w-9 items-center justify-center rounded-full bg-surface-container-lowest/90 shadow-md backdrop-blur-sm transition-colors hover:bg-surface-container-lowest">
            <Icon name="close" className="text-[20px]" />
          </button>
          <div className={`absolute bottom-3 left-4 rounded-full px-2.5 py-0.5 text-[12px] font-bold text-white shadow-sm ${STATUS_BG[place.status]}`}>
            동반 {STATUS_LABEL[place.status]}
          </div>
        </div>

        <div className="border-b border-surface-container-high/60 p-5 pb-3">
          <div className="mb-1 flex items-center gap-2 text-[13px] font-medium text-outline">
            동반 {CATEGORY_LABEL[place.category]} · {place.area} ·
            <span className="font-bold text-primary-container">{formatDistance(place.distanceM)}</span>
          </div>
          <h2 className="text-[22px] font-bold tracking-tight">{place.name}</h2>
        </div>

        {place.badges.length > 0 && (
          <div className="flex flex-wrap gap-2 border-b border-surface-container-high/60 p-5 py-4">
            {place.badges.map((b) => (
              <span key={b.label} className={`flex items-center gap-1 rounded-full px-3 py-1 text-[12px] font-bold ${TONE[b.tone]}`}>
                <Icon name={b.icon} className="text-[14px]" />
                {b.label}
              </span>
            ))}
          </div>
        )}

        <div className="flex flex-col gap-3 p-5">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5 text-label-md font-bold">
              <Icon name="smart_toy" className="text-[18px] text-primary" />
              AI 리뷰 요약 3줄
            </span>
            <span className="text-[11px] font-medium text-outline">최근 30일 데이터</span>
          </div>
          <ol className="flex flex-col gap-2.5 rounded-2xl border border-outline-variant/20 bg-surface-container-low p-4">
            {place.summary.map((line, i) => (
              <li key={i} className="flex items-start gap-2 text-[13px] leading-snug">
                <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-secondary-container text-[11px] font-bold text-on-secondary-container">{i + 1}</span>
                {line}
              </li>
            ))}
          </ol>
          <button type="button" onClick={() => setModal('source')} className="flex items-center justify-between rounded-xl px-3 py-2 text-[13px] font-medium text-outline transition-colors hover:bg-surface-container-low">
            <span className="flex items-center gap-1.5">
              <Icon name="find_in_page" className="text-[16px]" />
              근거 {place.evidenceCount}건 · 출처 보기
            </span>
            <Icon name="chevron_right" className="text-[16px]" />
          </button>
        </div>
      </div>

      <div className="flex shrink-0 flex-col gap-2 border-t border-surface-container-high/60 p-5 pt-3">
        {/* TODO: POST /api/places/{id}/visits (산책 기록 자동 생성) */}
        <button type="button" disabled={visited} onClick={() => setVisited(true)} className="flex h-12 w-full items-center justify-center gap-1.5 rounded-full bg-primary-container text-label-lg font-bold text-white shadow-sm transition-all hover:bg-primary disabled:bg-status-ok">
          <Icon name="task_alt" className="text-[20px]" />
          {visited ? '산책 기록에 남겼어요' : '다녀왔어요'}
        </button>
        <button type="button" onClick={() => setModal('edit')} className="flex w-full items-center justify-center gap-1 rounded-full py-2 text-label-md text-outline transition-colors hover:text-on-surface">
          <Icon name="flag" className="text-[15px]" />
          정보 수정 요청
        </button>
      </div>
      {modal === 'source' && <SourceModal placeName={place.name} count={place.evidenceCount} onClose={() => setModal(null)} />}
      {modal === 'edit' && <EditRequestModal placeName={place.name} onClose={() => setModal(null)} />}
    </aside>
  )
}
