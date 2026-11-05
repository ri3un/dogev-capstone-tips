import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import Icon from '../components/common/Icon'
import { card, chip, ghostBtn, input, primaryBtn } from '../components/common/styles'
import { PLACES } from '../mocks/places'
import { CONDITION_FIELDS, PLACE_CONDITIONS } from '../mocks/requests'

// 장소 정보 수정 요청: 조건 하나를 골라 바른 값을 보내면 관리자가 승인
export default function PlaceEditRequestPage() {
  const { placeId } = useParams()
  const navigate = useNavigate()
  const place = PLACES.find((p) => p.id === Number(placeId))
  const [fieldKey, setFieldKey] = useState(CONDITION_FIELDS[0].key)
  const [after, setAfter] = useState('')
  const [note, setNote] = useState('')
  const [done, setDone] = useState(false)

  const field = CONDITION_FIELDS.find((f) => f.key === fieldKey)!
  const before = PLACE_CONDITIONS[fieldKey] ?? '정보 없음'
  const canSubmit = after !== '' && after !== before

  if (!place) {
    return (
      <div className="mx-auto flex w-full max-w-[640px] flex-col items-center gap-4 px-5 py-20 text-center">
        <p className="text-headline-sm font-bold">장소를 찾을 수 없어요</p>
        <Link to="/map" className={ghostBtn}>
          지도로 돌아가기
        </Link>
      </div>
    )
  }

  if (done) {
    return (
      <div className="mx-auto flex w-full max-w-[640px] flex-col items-center gap-4 px-5 py-20 text-center">
        <span className="flex h-16 w-16 items-center justify-center rounded-full bg-tertiary-container/20">
          <Icon name="task_alt" className="text-[36px] text-tertiary" />
        </span>
        <p className="text-headline-sm font-bold">수정 요청을 보냈어요</p>
        <p className="text-on-surface-variant">관리자가 확인하면 결과를 내 요청 목록에서 볼 수 있어요.</p>
        <div className="mt-2 flex gap-2">
          <Link to={`/map?placeId=${place.id}`} className={ghostBtn}>
            장소로 돌아가기
          </Link>
          <Link to="/my/requests" className={primaryBtn}>
            내 요청 목록
          </Link>
        </div>
      </div>
    )
  }

  // TODO: POST /api/places/{placeId}/edit-requests { field, after, note }
  const submit = () => canSubmit && setDone(true)

  return (
    <div className="mx-auto flex w-full max-w-[640px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div>
        <p className="mb-1 text-label-md text-secondary">{place.name}</p>
        <h1 className="text-headline-lg font-bold tracking-tight">정보 수정 요청</h1>
        <p className="mt-1 text-on-surface-variant">다녀와 보니 다른 점이 있었나요? 관리자가 확인한 뒤 반영해요.</p>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
        className={`${card} flex flex-col gap-6 p-6`}
      >
        <label className="flex flex-col gap-2">
          <span className="font-bold">어떤 정보가 다른가요?</span>
          <select
            value={fieldKey}
            onChange={(e) => {
              setFieldKey(e.target.value)
              setAfter('')
            }}
            className={input}
          >
            {CONDITION_FIELDS.map((f) => (
              <option key={f.key} value={f.key}>
                {f.label}
              </option>
            ))}
          </select>
        </label>

        <div className="flex flex-col gap-2">
          <span className="font-bold">바른 정보</span>
          <p className="text-label-md text-outline">
            지금 표시: <span className="font-semibold text-on-surface-variant">{before}</span>
          </p>
          <div className="flex flex-wrap gap-2">
            {field.options.map((o) => (
              <button key={o} type="button" disabled={o === before} onClick={() => setAfter(o)} className={`${chip(after === o)} disabled:cursor-not-allowed disabled:opacity-40`}>
                {o}
              </button>
            ))}
          </div>
        </div>

        <label className="flex flex-col gap-2">
          <span className="font-bold">
            설명 <span className="text-label-md font-normal text-outline">(선택)</span>
          </span>
          <textarea value={note} onChange={(e) => setNote(e.target.value)} maxLength={200} rows={3} placeholder="예: 입구에 대형견 출입 가능 안내가 있어요." className={`${input} h-auto py-3`} />
          <span className="self-end text-label-sm text-outline">{note.length}/200</span>
        </label>

        <div className="flex justify-end gap-2">
          <button type="button" onClick={() => navigate(-1)} className={ghostBtn}>
            취소
          </button>
          <button type="submit" disabled={!canSubmit} className={primaryBtn}>
            <Icon name="send" className="text-[20px]" />
            요청 보내기
          </button>
        </div>
      </form>
    </div>
  )
}
