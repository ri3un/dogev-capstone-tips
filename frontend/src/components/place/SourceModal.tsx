import Icon from '../common/Icon'
import Modal from '../common/Modal'
import { SOURCES } from '../../mocks/editRequests'

// 근거·출처 보기: 요약에 쓰인 블로그 글 제목·날짜·링크 (TODO: GET /api/places/{id} 응답의 sources)
export default function SourceModal({ placeName, count, onClose }: { placeName: string; count: number; onClose: () => void }) {
  return (
    <Modal title={`근거 ${count}건`} onClose={onClose}>
      <p className="mb-3 text-label-md text-outline">{placeName} · 네이버 블로그 글을 AI가 요약했어요</p>
      <ul className="flex flex-col gap-2">
        {SOURCES.map((s) => (
          <li key={s.title}>
            <a href={s.url} target="_blank" rel="noreferrer" className="flex items-center gap-3 rounded-xl bg-surface-container-low px-4 py-3 hover:bg-surface-container">
              <Icon name="article" className="text-[20px] text-primary" />
              <span className="flex-1">
                <span className="block text-label-lg font-semibold">{s.title}</span>
                <span className="text-label-sm text-outline">{s.date}</span>
              </span>
              <Icon name="open_in_new" className="text-[18px] text-outline" />
            </a>
          </li>
        ))}
      </ul>
      <p className="mt-4 text-label-sm text-outline">글 원문은 저장하지 않고 링크만 보여 드려요. 정보가 다르면 '정보 수정 요청'을 눌러 주세요.</p>
    </Modal>
  )
}
