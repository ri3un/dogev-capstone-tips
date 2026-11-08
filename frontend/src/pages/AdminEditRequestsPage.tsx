import { useState } from 'react'
import { Link } from 'react-router'
import ConfirmModal from '../components/common/ConfirmModal'
import Icon from '../components/common/Icon'
import Modal from '../components/common/Modal'
import { card, chip, input, primaryBtn } from '../components/common/styles'
import { adminApi, authApi } from '../api/endpoints'
import { localDate } from '../api/format'
import type { EditRequest, EditStatus } from '../api/types'
import { conditionLabel, EDIT_STATUS, valueLabel } from '../constants/place'
import { useApi } from '../hooks/useApi'

const TABS: EditStatus[] = ['PENDING', 'APPROVED', 'REJECTED']
const condOf = (r: EditRequest) => r.conditionLabel ?? conditionLabel(r.conditionKey)

// 관리자 수정 요청 관리: 대기 요청을 승인(확인 모달)하거나 사유와 함께 반려
export default function AdminEditRequestsPage() {
  const [tab, setTab] = useState<EditStatus>('PENDING')
  const { data: me, loading } = useApi(() => authApi.me(), [])
  const isAdmin = me?.role === 'ADMIN'
  const { data, setData } = useApi(() => (isAdmin ? adminApi.requests() : null), [isAdmin])
  const [approving, setApproving] = useState<EditRequest | null>(null)
  const [rejecting, setRejecting] = useState<EditRequest | null>(null)
  const [reason, setReason] = useState('')

  if (loading) return null
  // shortcut: 화면 안에서만 막음(서버도 ADMIN 아니면 403). 관리자 화면이 늘면 라우트 가드로
  if (!isAdmin)
    return <p className="py-20 text-center text-on-surface-variant">관리자만 볼 수 있는 화면이에요.</p>

  const list = data ?? []
  const decide = async (job: () => Promise<EditRequest>) => {
    try {
      const done = await job()
      setData(list.map((r) => (r.id === done.id ? done : r)))
    } catch (e) {
      alert((e as { message?: string }).message ?? '처리하지 못했어요')
    }
  }
  const closeReject = () => {
    setRejecting(null)
    setReason('') // X로 닫아도 다음 요청에 사유가 남지 않게
  }
  const shown = list.filter((r) => r.status === tab)

  return (
    <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-5 py-9 lg:px-10">
      <div>
        <p className="mb-1 text-label-md text-secondary">관리자</p>
        <h1 className="text-headline-lg font-bold tracking-tight">수정 요청 관리</h1>
      </div>

      <div className="flex gap-2">
        {TABS.map((t) => (
          <button key={t} type="button" onClick={() => setTab(t)} className={chip(tab === t)}>
            {EDIT_STATUS[t].label} {list.filter((r) => r.status === t).length}
          </button>
        ))}
      </div>

      {shown.length === 0 ? (
        <p className={`${card} py-14 text-center text-label-md text-outline`}>{EDIT_STATUS[tab].label} 요청이 없어요</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {shown.map((r) => (
            <li key={r.id} className={`${card} flex flex-col gap-3 p-5 md:flex-row md:items-center`}>
              <div className="flex flex-1 flex-col gap-1.5">
                <Link to={`/map?placeId=${r.placeId}`} className="flex items-center gap-1 font-bold hover:text-primary">
                  {r.placeName}
                  <Icon name="open_in_new" className="text-[16px] text-outline" />
                </Link>
                <p className="text-on-surface-variant">
                  {condOf(r)}: <span className="text-outline">{valueLabel(r.conditionKey, r.currentValue)}</span> → <strong className="text-primary">{valueLabel(r.conditionKey, r.requestedValue)}</strong>
                </p>
                {r.comment && <p className="text-label-md text-on-surface-variant">"{r.comment}"</p>}
                {r.rejectReason && <p className="text-label-md text-error">반려 사유: {r.rejectReason}</p>}
                <span className="text-label-sm text-outline">
                  {r.nickname} · {localDate(new Date(r.createdAt))}
                </span>
              </div>
              {r.status === 'PENDING' && (
                <div className="flex shrink-0 gap-2">
                  <button type="button" onClick={() => setRejecting(r)} className="rounded-full border border-beige px-5 py-2.5 text-label-lg font-semibold text-error hover:bg-error-container/40">
                    반려
                  </button>
                  <button type="button" onClick={() => setApproving(r)} className={`${primaryBtn} h-11`}>
                    승인
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {approving && (
        <ConfirmModal
          title="이 요청을 승인할까요?"
          body={`${approving.placeName}의 '${condOf(approving)}'이(가) '${valueLabel(approving.conditionKey, approving.requestedValue)}'(으)로 바뀌어요.`}
          confirmLabel="승인"
          onConfirm={() => {
            decide(() => adminApi.approve(approving.id))
            setApproving(null)
          }}
          onClose={() => setApproving(null)}
        />
      )}
      {rejecting && (
        <Modal title="반려 사유" onClose={closeReject}>
          <p className="mb-3 text-label-md text-outline">요청자가 '내 수정 요청'에서 이 사유를 봐요. 장소 정보는 그대로 유지돼요.</p>
          <input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={60} placeholder="한 줄로 적어 주세요" className={input} />
          <div className="mt-5 flex justify-end">
            <button
              type="button"
              disabled={!reason.trim()}
              onClick={() => {
                decide(() => adminApi.reject(rejecting.id, reason.trim()))
                closeReject()
              }}
              className={`${primaryBtn} h-11 bg-error hover:bg-on-error-container`}
            >
              반려하기
            </button>
          </div>
        </Modal>
      )}
    </div>
  )
}
