import { useState } from 'react'
import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import { card, chip } from '../components/common/styles'
import { MY_REQUESTS, REQUEST_STATUS, type RequestStatus } from '../mocks/requests'

const FILTERS: { key: RequestStatus | 'ALL'; label: string }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'PENDING', label: '검토 중' },
  { key: 'APPROVED', label: '반영됨' },
  { key: 'REJECTED', label: '반려' },
]

// 내 수정 요청 목록: 상태와 반려 사유 확인
export default function MyRequestsPage() {
  // TODO: GET /api/place-requests/me
  const [filter, setFilter] = useState<RequestStatus | 'ALL'>('ALL')
  const list = filter === 'ALL' ? MY_REQUESTS : MY_REQUESTS.filter((r) => r.status === filter)

  return (
    <div className="mx-auto flex w-full max-w-[840px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div>
        <p className="mb-1 text-label-md text-secondary">장소 정보를 함께 고쳐요</p>
        <h1 className="text-headline-lg font-bold tracking-tight">내 수정 요청</h1>
      </div>

      <div className="flex flex-wrap gap-2">
        {FILTERS.map((f) => (
          <button key={f.key} type="button" onClick={() => setFilter(f.key)} className={chip(filter === f.key)}>
            {f.label}
          </button>
        ))}
      </div>

      {list.length === 0 ? (
        <p className={`${card} py-12 text-center text-label-md text-outline`}>해당하는 요청이 없어요</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {list.map((r) => (
            <li key={r.id} className={`${card} p-5`}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <Link to={`/map?placeId=${r.placeId}`} className="font-bold hover:text-primary hover:underline">
                    {r.placeName}
                  </Link>
                  <p className="mt-0.5 text-label-sm text-outline">{r.createdAt} 요청</p>
                </div>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-label-sm font-bold ${REQUEST_STATUS[r.status].cls}`}>{REQUEST_STATUS[r.status].label}</span>
              </div>
              <p className="mt-3 flex flex-wrap items-center gap-1.5 text-label-lg">
                <span className="font-semibold">{r.field}</span>
                <span className="text-outline line-through">{r.before}</span>
                <Icon name="arrow_forward" className="text-[16px] text-outline" />
                <span className="font-bold text-primary">{r.after}</span>
              </p>
              {r.note && <p className="mt-1 text-label-md text-on-surface-variant">{r.note}</p>}
              {r.status === 'REJECTED' && r.rejectReason && (
                <p className="mt-3 flex items-start gap-1.5 rounded-xl bg-error-container/30 p-3 text-label-md text-on-error-container">
                  <Icon name="info" className="text-[18px]" />
                  반려 사유: {r.rejectReason}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}

      <p className="flex items-center justify-center gap-1.5 text-label-sm text-outline">
        <Icon name="flag" className="text-[16px]" />
        장소 상세의 '정보 수정 요청'에서 새 요청을 보낼 수 있어요.
      </p>
    </div>
  )
}
