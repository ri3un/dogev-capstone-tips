import { useState } from 'react'
import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import { card, chip, ghostBtn, input, primaryBtn } from '../components/common/styles'
import { ADMIN_REQUESTS, IS_ADMIN, REQUEST_STATUS, type PlaceEditRequest } from '../mocks/requests'

// 관리자 수정 요청 관리: 승인하면 장소 정보에 반영, 반려하면 사유 한 줄을 남김
export default function AdminRequestsPage() {
  const [list, setList] = useState(ADMIN_REQUESTS)
  const [tab, setTab] = useState<'PENDING' | 'DONE'>('PENDING')
  const [rejecting, setRejecting] = useState<PlaceEditRequest | null>(null)
  const [reason, setReason] = useState('')

  // TODO: 라우터 가드로 옮김 (ADMIN이 아니면 403)
  if (!IS_ADMIN) {
    return (
      <div className="mx-auto flex w-full max-w-[640px] flex-col items-center gap-4 px-5 py-20 text-center">
        <Icon name="lock" className="text-[40px] text-outline" />
        <p className="text-headline-sm font-bold">관리자만 볼 수 있는 화면이에요</p>
        <Link to="/" className={ghostBtn}>
          홈으로
        </Link>
      </div>
    )
  }

  // TODO: PATCH /api/admin/place-requests/{id} { status, rejectReason }
  const decide = (id: number, status: 'APPROVED' | 'REJECTED', rejectReason?: string) =>
    setList((l) => l.map((r) => (r.id === id ? { ...r, status, rejectReason } : r)))

  const pending = list.filter((r) => r.status === 'PENDING')
  const shown = tab === 'PENDING' ? pending : list.filter((r) => r.status !== 'PENDING')

  return (
    <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div>
        <p className="mb-1 text-label-md text-secondary">관리자</p>
        <h1 className="text-headline-lg font-bold tracking-tight">장소 수정 요청</h1>
      </div>

      <div className="flex gap-2">
        <button type="button" onClick={() => setTab('PENDING')} className={chip(tab === 'PENDING')}>
          대기 {pending.length}
        </button>
        <button type="button" onClick={() => setTab('DONE')} className={chip(tab === 'DONE')}>
          처리 완료
        </button>
      </div>

      {shown.length === 0 ? (
        <p className={`${card} py-12 text-center text-label-md text-outline`}>{tab === 'PENDING' ? '대기 중인 요청이 없어요' : '처리한 요청이 없어요'}</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {shown.map((r) => (
            <li key={r.id} className={`${card} flex flex-col gap-3 p-5 md:flex-row md:items-center`}>
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <Link to={`/map?placeId=${r.placeId}`} className="font-bold hover:text-primary hover:underline">
                    {r.placeName}
                  </Link>
                  <span className={`rounded-full px-2.5 py-0.5 text-label-sm font-bold ${REQUEST_STATUS[r.status].cls}`}>{REQUEST_STATUS[r.status].label}</span>
                </div>
                <p className="mt-2 flex flex-wrap items-center gap-1.5 text-label-lg">
                  <span className="font-semibold">{r.field}</span>
                  <span className="text-outline line-through">{r.before}</span>
                  <Icon name="arrow_forward" className="text-[16px] text-outline" />
                  <span className="font-bold text-primary">{r.after}</span>
                </p>
                {r.note && <p className="mt-1 text-label-md text-on-surface-variant">"{r.note}"</p>}
                <p className="mt-1 text-label-sm text-outline">
                  {r.requester} · {r.createdAt}
                </p>
                {r.rejectReason && <p className="mt-1 text-label-sm text-error">반려 사유: {r.rejectReason}</p>}
              </div>
              {r.status === 'PENDING' && (
                <div className="flex shrink-0 gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setRejecting(r)
                      setReason('')
                    }}
                    className={`${ghostBtn} h-11`}
                  >
                    반려
                  </button>
                  <button type="button" onClick={() => decide(r.id, 'APPROVED')} className={`${primaryBtn} h-11`}>
                    <Icon name="check" className="text-[20px]" />
                    승인
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {rejecting && (
        <div role="dialog" aria-modal="true" className="fixed inset-0 z-[100] flex items-center justify-center bg-on-surface/30 px-5 backdrop-blur-sm" onClick={() => setRejecting(null)}>
          <form
            className={`${card} w-full max-w-[440px] p-7`}
            onClick={(e) => e.stopPropagation()}
            onSubmit={(e) => {
              e.preventDefault()
              if (!reason.trim()) return
              decide(rejecting.id, 'REJECTED', reason.trim())
              setRejecting(null)
            }}
          >
            <h2 className="text-headline-sm font-bold">요청 반려</h2>
            <p className="mt-2 text-on-surface-variant">
              {rejecting.placeName} · {rejecting.field}. 장소 정보는 그대로 두고, 요청자에게 사유를 보여 줘요.
            </p>
            <input autoFocus value={reason} onChange={(e) => setReason(e.target.value)} maxLength={100} placeholder="사유 한 줄" className={`${input} mt-4`} />
            <div className="mt-6 flex justify-end gap-2">
              <button type="button" onClick={() => setRejecting(null)} className={`${ghostBtn} h-11`}>
                취소
              </button>
              <button type="submit" disabled={!reason.trim()} className={`${primaryBtn} h-11 bg-error hover:bg-on-error-container`}>
                반려하기
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
