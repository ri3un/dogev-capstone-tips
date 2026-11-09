import { useState } from 'react'
import { placeApi } from '../../api/endpoints'
import { formatDistance, monthDay } from '../../api/format'
import { useApi } from '../../hooks/useApi'
import Icon from '../common/Icon'
import Photo from '../common/Photo'
import EditRequestModal from './EditRequestModal'
import SourceModal from './SourceModal'
import { CATEGORY_LABEL, VERDICT_BG, VERDICT_LABEL } from '../../constants/place'

// 조건 키워드 배지 색: 순서대로 돌려 씀
const TONES = ['bg-tertiary-container/20 text-tertiary', 'bg-secondary-container/60 text-on-secondary-container', 'bg-surface-container-high text-primary']

// 장소 상세 드로어: 지도 오른쪽에 겹쳐 열립니다.
// showVerdict: 지도에서 "우리 강아지 기준"을 끄면 동반 판정 배지를 숨김
export default function PlaceDrawer({ placeId, dogId, showVerdict, distanceM, onClose }: { placeId: number; dogId: number; showVerdict: boolean; distanceM: number | null; onClose: () => void }) {
  const [modal, setModal] = useState<'source' | 'edit' | null>(null)
  const [visited, setVisited] = useState(false)
  const [visiting, setVisiting] = useState(false)
  const { data: place } = useApi(() => placeApi.get(placeId, dogId), [placeId, dogId])
  if (!place) return null

  const badges = place.conditions.filter((c) => c.tag).map((c) => c.tag as string)
  const area = (place.roadAddress || place.address || '').split(' ').slice(1, 3).join(' ') // 서버가 빈 문자열을 줄 때가 있어 || 사용

  const visit = async () => {
    setVisiting(true)
    try {
      await placeApi.visit(place.id, dogId)
      setVisited(true)
    } catch {
      alert('산책 기록에 남기지 못했어요. 잠시 후 다시 시도해 주세요')
    } finally {
      setVisiting(false)
    }
  }

  return (
    <aside className="absolute top-0 right-0 z-30 flex h-full w-full flex-col border-l border-outline-variant/30 bg-surface-container-lowest shadow-2xl sm:w-[420px]">
      <div className="flex-1 overflow-y-auto">
        <div className="relative h-56 w-full bg-surface-container">
          <Photo src={place.imageUrl ?? undefined} alt={place.name} className="h-full w-full" />
          <button type="button" aria-label="닫기" onClick={onClose} className="absolute top-4 right-4 flex h-9 w-9 items-center justify-center rounded-full bg-surface-container-lowest/90 shadow-md backdrop-blur-sm transition-colors hover:bg-surface-container-lowest">
            <Icon name="close" className="text-[20px]" />
          </button>
          {showVerdict && place.verdict && (
            <div className={`absolute bottom-3 left-4 rounded-full px-2.5 py-0.5 text-[12px] font-bold text-white shadow-sm ${VERDICT_BG[place.verdict.level]}`}>
              동반 {VERDICT_LABEL[place.verdict.level]}
            </div>
          )}
        </div>

        <div className="border-b border-surface-container-high/60 p-5 pb-3">
          <div className="mb-1 flex items-center gap-2 text-[13px] font-medium text-outline">
            동반 {CATEGORY_LABEL[place.category]}
            {area && ` · ${area}`}
            {distanceM != null && (
              <>
                {' ·'}
                <span className="font-bold text-primary-container">{formatDistance(distanceM)}</span>
              </>
            )}
          </div>
          <h2 className="text-[22px] font-bold tracking-tight">{place.name}</h2>
        </div>

        {badges.length > 0 && (
          <div className="flex flex-wrap gap-2 border-b border-surface-container-high/60 p-5 py-4">
            {badges.map((b, i) => (
              <span key={b} className={`flex items-center gap-1 rounded-full px-3 py-1 text-[12px] font-bold ${TONES[i % TONES.length]}`}>
                <Icon name="verified" className="text-[14px]" />
                {b}
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
            <span className="text-[11px] font-medium text-outline">{place.reviewSummarizedAt && `${monthDay(place.reviewSummarizedAt)} 요약`}</span>
          </div>
          <ol className="flex flex-col gap-2.5 rounded-2xl border border-outline-variant/20 bg-surface-container-low p-4">
            {place.summary.length === 0 && <li className="text-[13px] text-outline">아직 블로그 리뷰 요약이 없어요</li>}
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
              근거 {place.reviewSourceCount}건 · 출처 보기
            </span>
            <Icon name="chevron_right" className="text-[16px]" />
          </button>
        </div>
      </div>

      <div className="flex shrink-0 flex-col gap-2 border-t border-surface-container-high/60 p-5 pt-3">
        <button type="button" disabled={visited || visiting} onClick={visit} className="flex h-12 w-full items-center justify-center gap-1.5 rounded-full bg-primary-container text-label-lg font-bold text-white shadow-sm transition-all hover:bg-primary disabled:bg-status-ok">
          <Icon name="task_alt" className="text-[20px]" />
          {visited ? '산책 기록에 남겼어요' : '다녀왔어요'}
        </button>
        <button type="button" onClick={() => setModal('edit')} className="flex w-full items-center justify-center gap-1 rounded-full py-2 text-label-md text-outline transition-colors hover:text-on-surface">
          <Icon name="flag" className="text-[15px]" />
          정보 수정 요청
        </button>
      </div>
      {modal === 'source' && <SourceModal placeName={place.name} sources={place.sources} onClose={() => setModal(null)} />}
      {modal === 'edit' && <EditRequestModal placeId={place.id} placeName={place.name} onClose={() => setModal(null)} />}
    </aside>
  )
}
