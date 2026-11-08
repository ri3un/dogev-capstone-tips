import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import { card } from '../components/common/styles'
import { placeApi } from '../api/endpoints'
import { localDate } from '../api/format'
import { conditionLabel, EDIT_STATUS, valueLabel } from '../constants/place'
import { useApi } from '../hooks/useApi'

// 내 수정 요청: 내가 보낸 장소 정보 수정 요청과 처리 상태 (가족과 공유하지 않음)
export default function MyEditRequestsPage() {
  const { data, loading } = useApi(() => placeApi.myRequests(), [])
  if (loading) return null
  const mine = data ?? []

  return (
    <div className="mx-auto flex w-full max-w-[840px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div>
        <p className="mb-1 text-label-md text-secondary">장소 정보</p>
        <h1 className="text-headline-lg font-bold tracking-tight">내 수정 요청</h1>
      </div>

      {mine.length === 0 ? (
        <section className={`${card} flex flex-col items-center gap-2 py-14 text-center`}>
          <Icon name="flag" className="text-[36px] text-outline" />
          <p className="text-label-md text-outline">아직 보낸 요청이 없어요. 지도에서 장소를 열고 '정보 수정 요청'을 눌러 보세요.</p>
        </section>
      ) : (
        <ul className="flex flex-col gap-3">
          {mine.map((r) => (
            <li key={r.id} className={`${card} flex flex-col gap-2 p-5`}>
              <div className="flex items-center justify-between gap-3">
                <Link to={`/map?placeId=${r.placeId}`} className="font-bold hover:text-primary">
                  {r.placeName}
                </Link>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-label-sm font-bold ${EDIT_STATUS[r.status].cls}`}>{EDIT_STATUS[r.status].label}</span>
              </div>
              <p className="text-on-surface-variant">
                {r.conditionLabel ?? conditionLabel(r.conditionKey)}: <span className="text-outline line-through">{valueLabel(r.conditionKey, r.currentValue)}</span> → <strong>{valueLabel(r.conditionKey, r.requestedValue)}</strong>
              </p>
              {r.status === 'REJECTED' && r.rejectReason && (
                <p className="rounded-xl bg-error-container/50 px-3 py-2 text-label-md text-on-error-container">반려 사유: {r.rejectReason}</p>
              )}
              <span className="text-label-sm text-outline">{localDate(new Date(r.createdAt))}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
