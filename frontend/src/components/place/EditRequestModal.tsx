import { useState } from 'react'
import { placeApi } from '../../api/endpoints'
import Icon from '../common/Icon'
import Modal from '../common/Modal'
import { chip, input, primaryBtn } from '../common/styles'
import { CONDITIONS, type ConditionKey } from '../../constants/place'

// 장소 정보 수정 요청: 조건 하나를 고르고 맞는 값을 알려 줌 (관리자가 승인하면 반영)
export default function EditRequestModal({ placeId, placeName, onClose }: { placeId: number; placeName: string; onClose: () => void }) {
  const [key, setKey] = useState<ConditionKey | null>(null)
  const [value, setValue] = useState('')
  const [memo, setMemo] = useState('')
  const [sent, setSent] = useState(false)
  const [sending, setSending] = useState(false)
  const condition = CONDITIONS.find((c) => c.key === key)

  const send = async () => {
    if (!key || !value) return
    setSending(true)
    try {
      await placeApi.requestEdit(placeId, key, value, memo.trim() || undefined)
      setSent(true)
    } catch {
      alert('요청을 보내지 못했어요. 잠시 후 다시 시도해 주세요')
    } finally {
      setSending(false)
    }
  }

  if (sent)
    return (
      <Modal title="요청을 보냈어요" onClose={onClose}>
        <div className="flex flex-col items-center gap-3 py-4 text-center">
          <Icon name="mark_email_read" className="text-[44px] text-primary" />
          <p className="text-on-surface-variant">관리자가 확인한 뒤 반영돼요. 결과는 마이페이지의 '내 수정 요청'에서 볼 수 있어요.</p>
          <button type="button" onClick={onClose} className={`${primaryBtn} mt-2 h-11`}>
            확인
          </button>
        </div>
      </Modal>
    )

  return (
    <Modal title="정보 수정 요청" onClose={onClose}>
      <p className="mb-4 text-label-md text-outline">{placeName}</p>
      <div className="flex flex-col gap-5">
        <div className="flex flex-col gap-2">
          <span className="text-label-lg font-semibold">어떤 정보인가요?</span>
          <div className="flex flex-wrap gap-2">
            {CONDITIONS.map((c) => (
              <button
                key={c.key}
                type="button"
                onClick={() => {
                  setKey(c.key)
                  setValue('')
                }}
                className={chip(key === c.key)}
              >
                {c.label}
              </button>
            ))}
          </div>
        </div>
        {condition && (
          <div className="flex flex-col gap-2">
            <span className="text-label-lg font-semibold">실제로는</span>
            <div className="flex flex-wrap gap-2">
              {condition.options.map(([v, label]) => (
                <button key={v} type="button" onClick={() => setValue(v)} className={chip(value === v)}>
                  {label}
                </button>
              ))}
            </div>
          </div>
        )}
        <label className="flex flex-col gap-2">
          <span className="text-label-lg font-semibold">
            메모 <span className="text-label-sm font-normal text-outline">(선택)</span>
          </span>
          <input value={memo} onChange={(e) => setMemo(e.target.value)} maxLength={100} placeholder="예: 매장 안내문에 15kg까지 가능하다고 적혀 있어요" className={input} />
        </label>
        <button type="button" disabled={!value || sending} onClick={send} className={`${primaryBtn} w-full`}>
          <Icon name="send" className="text-[20px]" />
          요청 보내기
        </button>
      </div>
    </Modal>
  )
}
