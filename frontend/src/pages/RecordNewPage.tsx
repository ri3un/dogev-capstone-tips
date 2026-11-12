import PawLoading from '../components/common/PawLoading'
import { useEffect, useRef, useState } from 'react'
import { toast } from '../components/common/toast'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'
import { consultApi, recordApi } from '../api/endpoints'
import { isQuestion } from '../utils/intent'
import { todayStr } from '../api/format'
import type { RecordCategory, RecordInput, StoolStatus } from '../api/types'
import Icon from '../components/common/Icon'
import { card, chip, ghostBtn, primaryBtn } from '../components/common/styles'
import { useCurrentDog } from '../hooks/useCurrentDog'
import { EXAMPLE_SENTENCES, PARSED_LABEL, PARSED_OPTIONS, type ParsedKind } from '../constants/records'

const KINDS = Object.keys(PARSED_LABEL) as ParsedKind[]

interface ParsedItem {
  id: number
  kind: ParsedKind
  category: RecordCategory // 서버 카테고리 (간식·병원처럼 화면 종류보다 자세한 값을 보존)
  value: string // 선택지 값 또는 자유 입력
  memo: string
  needsCheck?: boolean
}

const KIND_OF: Record<RecordCategory, ParsedKind> = {
  MEAL: 'meal', SNACK: 'meal', STOOL: 'poop', ACTIVITY: 'activity', SYMPTOM: 'symptom',
  WEIGHT: 'weight', MEDICATION: 'medicine', HOSPITAL: 'medicine', QUESTION: 'question',
}
const CATEGORY_OF: Record<ParsedKind, RecordCategory> = {
  meal: 'MEAL', poop: 'STOOL', activity: 'ACTIVITY', symptom: 'SYMPTOM', weight: 'WEIGHT', medicine: 'MEDICATION', question: 'QUESTION',
}

// 서버 후보 → 확인 카드 (값이 비어 있으면 '확인 필요'로 표시)
function toItem(c: RecordInput, i: number): ParsedItem {
  const kind = KIND_OF[c.category]
  const value =
    kind === 'poop' ? c.stoolStatus ?? ''
      : kind === 'activity' ? (c.walkMinutes != null ? `${c.walkMinutes}분` : '')
        : kind === 'weight' ? (c.weightKg != null ? `${c.weightKg}kg` : '')
          : kind === 'meal' ? String((c.data as { amount?: string } | null)?.amount ?? 'NORMAL')
            : ''
  const needsCheck = (kind === 'poop' || kind === 'activity' || kind === 'weight') && !value
  return { id: Date.now() + i, kind, category: c.category, value, memo: c.memo ?? '', needsCheck }
}

// 확인 카드 → 저장 요청
function toInput(item: ParsedItem, date: string): RecordInput {
  const num = parseFloat(item.value)
  return {
    category: item.category,
    recordDate: date,
    memo: item.memo || null,
    stoolStatus: item.kind === 'poop' && item.value ? (item.value as StoolStatus) : null,
    walkMinutes: item.kind === 'activity' && !Number.isNaN(num) ? Math.round(num) : null,
    weightKg: item.kind === 'weight' && !Number.isNaN(num) ? num : null,
    data: item.kind === 'meal' && item.value ? { amount: item.value } : null,
  }
}

// 기록 입력: 글 입력 → 바로 저장. 경고·애매한 항목이 있을 때만 2단계 확인 카드를 거침
export default function RecordNewPage() {
  const [params] = useSearchParams()
  const location = useLocation()
  const navigate = useNavigate()
  const today = todayStr()
  const { dog } = useCurrentDog()
  const kindParam = params.get('kind') as ParsedKind | null
  const [date, setDate] = useState(params.get('date') ?? today)
  const [text, setText] = useState((location.state as { text?: string } | null)?.text ?? '')
  // ?kind=weight 처럼 들어오면 글 없이 바로 그 항목 카드부터
  const [step, setStep] = useState<1 | 2>(kindParam && PARSED_LABEL[kindParam] ? 2 : 1)
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [warning, setWarning] = useState('')
  const [error, setError] = useState('')
  const [items, setItems] = useState<ParsedItem[]>(() =>
    kindParam && PARSED_LABEL[kindParam] ? [{ id: 1, kind: kindParam, category: CATEGORY_OF[kindParam], value: '', memo: '' }] : [],
  )

  // 통합 입력: 글은 항상 기록으로 바로 저장(확인 단계 없음, 목록에서 되돌리기). 질문이면 AI 3개 상담도 함께
  const parse = async () => {
    if (!dog || !text.trim()) return
    const q = text.trim()
    setLoading(true)
    setError('')
    const consult = isQuestion(q) ? consultApi.ask(dog.id, q).catch(() => null) : null
    let saved: { ids: number[]; needsCheckIds: number[] } | null = null
    try {
      const res = await recordApi.parse(dog.id, q, date)
      const parsed = res.candidates.map(toItem)
      setItems(parsed)
      setWarning(res.warnings.map((w) => w.message).join(' ')) // 2단계로 넘어갈 때만 보임 (바로 저장이면 따로 안 띄움)
      if (parsed.length > 0) {
        try {
          const records = await recordApi.batch(dog.id, parsed.map((i) => toInput(i, date)))
          saved = { ids: records.map((r) => r.id), needsCheckIds: records.filter((_, i) => parsed[i]?.needsCheck).map((r) => r.id) }
        } catch (e) {
          // 저장이 안 되면 확인 화면에서 다시 저장할 수 있게
          setError((e as { message?: string }).message ?? '바로 저장하지 못했어요. 확인하고 다시 저장해 주세요.')
        }
      }
    } catch (e) {
      // LLM이 모두 실패(503)해도 직접 입력으로 이어 갈 수 있게
      setItems([])
      setError(`${(e as { message?: string }).message ?? 'AI가 응답하지 않았어요'} 아래에서 항목을 직접 추가해 주세요.`)
    }
    const answer = consult && (await consult)
    if (answer) {
      toast(saved ? `킁킁, ${saved.ids.length}개 기록하고 AI에게 물어봤어요!` : '킁킁, AI에게 물어봤어요!')
      return navigate(`/consult/${answer.id}`)
    }
    if (saved) {
      return navigate(`/records?date=${date}`, { state: { savedIds: saved.ids, needsCheckIds: saved.needsCheckIds, text: q } })
    }
    setLoading(false)
    setStep(2)
  }

  // 홈에서 글을 들고 오면 바로 기록 후보로 바꿈 (한 번만)
  const autoParsed = useRef(false)
  useEffect(() => {
    if (!autoParsed.current && dog && text && (location.state as { text?: string } | null)?.text) {
      autoParsed.current = true
      parse()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dog])

  const update = (id: number, patch: Partial<ParsedItem>) => setItems((list) => list.map((i) => (i.id === id ? { ...i, ...patch, needsCheck: false } : i)))
  const remove = (id: number) => setItems((list) => list.filter((i) => i.id !== id))
  const add = (kind: ParsedKind) => setItems((list) => [...list, { id: Date.now(), kind, category: CATEGORY_OF[kind], value: PARSED_OPTIONS[kind]?.options[1]?.value ?? '', memo: '' }])

  const save = async () => {
    if (!dog || saving) return
    setSaving(true)
    setError('')
    try {
      await recordApi.batch(dog.id, items.map((i) => toInput(i, date)))
      toast('킁킁, 기록했어요!')
      navigate(`/records?date=${date}`)
    } catch (e) {
      setError((e as { message?: string }).message ?? '저장하지 못했어요')
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-[840px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div className="flex items-end justify-between gap-4">
        <div>
          <div className="mb-1 flex items-center gap-2 text-label-sm">
            <span className={`rounded-md px-2.5 py-0.5 font-bold ${step === 1 ? 'bg-primary-container text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>1 글 쓰기</span>
            <Icon name="arrow_forward" className="text-[14px] text-outline" />
            <span className={`rounded-md px-2.5 py-0.5 font-bold ${step === 2 ? 'bg-primary-container text-on-primary' : 'bg-surface-container text-on-surface-variant'}`}>2 필요할 때만 확인</span>
          </div>
          <h1 className="text-headline-lg font-bold tracking-tight">말하듯 기록하기</h1>
        </div>
        <label className="flex items-center gap-2 rounded-md bg-surface-container-lowest px-4 py-2 text-label-md shadow-sm">
          <Icon name="event" className="text-[18px] text-primary" />
          <input type="date" value={date} max={today} onChange={(e) => setDate(e.target.value)} className="bg-transparent focus:outline-none" />
        </label>
      </div>

      {step === 1 ? (
        <section className={`${card} flex flex-col gap-5 p-6 md:p-8`}>
          <div className="inline-flex items-center gap-2 self-start rounded-md bg-surface-container px-3 py-1.5 text-label-md">
            <span className="h-2 w-2 rounded-full bg-primary-container" />
            기록 대상: <strong>{dog?.name}</strong>
          </div>
          <textarea
            rows={6}
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="아침 사료 반 정도 먹고 오후에 무른 변 한 번, 저녁에 30분 산책"
            className="w-full resize-none rounded-xl border border-beige bg-surface-container-low p-5 text-body-lg leading-relaxed transition-all placeholder:text-outline focus:bg-surface-container-lowest focus:ring-2 focus:ring-primary-container focus:outline-none"
          />
          <div className="flex flex-col gap-2">
            <span className="text-label-sm text-outline">이렇게 써 보세요 (누르면 채워져요)</span>
            <div className="flex flex-wrap gap-2">
              {EXAMPLE_SENTENCES.map((s) => (
                <button key={s} type="button" onClick={() => setText(s)} className="rounded-md border border-beige bg-surface-container-lowest px-3.5 py-1.5 text-label-md text-on-surface-variant transition-all hover:bg-surface-container hover:text-primary">
                  {s}
                </button>
              ))}
            </div>
          </div>
          <div className="flex items-center justify-between gap-3 border-t border-outline-variant/30 pt-4">
            <span className="flex items-center gap-1.5 text-label-sm text-outline">
              <Icon name="auto_awesome" className="text-[16px] text-primary" />
              AI가 식사·배변·활동 등으로 나눠 바로 저장해요. 애매한 내용이 있을 때만 확인을 부탁드려요.
            </span>
            <button type="button" onClick={parse} disabled={!text.trim() || loading || !dog} className={primaryBtn}>
              <Icon name={loading ? 'refresh' : 'auto_awesome'} className={`text-[20px] ${loading ? 'animate-spin' : ''}`} />
              {loading ? '기록으로 정리하는 중' : '기록으로 바꾸기'}
            </button>
          </div>
          {/* AI가 글을 기록으로 나누는 동안 */}
          {loading && (
            <PawLoading
              compact
              label={`${dog?.name ?? ''} 보호자님! "${text.trim()}"을 저장하도록 하겠습니다 !${isQuestion(text) ? ' AI 3개에게도 물어보는 중이에요…' : ''}`}
            />
          )}
        </section>
      ) : (
        <section className="flex flex-col gap-4">
          {text && (
            <div className={`${card} flex items-start gap-3 p-5`}>
              <Icon name="format_quote" className="text-[22px] text-primary-container" />
              <p className="flex-1 leading-relaxed text-on-surface-variant">{text}</p>
              <button type="button" onClick={() => setStep(1)} className="shrink-0 text-label-md text-primary hover:underline">
                글 고치기
              </button>
            </div>
          )}

          {warning && (
            <div className="flex items-center gap-3 rounded-2xl bg-error-container px-5 py-4 text-on-error-container">
              <Icon name="warning" className="text-[22px]" />
              <p className="font-semibold">{warning}</p>
            </div>
          )}
          {error && <p role="alert" className="text-label-md text-error">{error}</p>}

          {items.map((item) => {
            const meta = PARSED_LABEL[item.kind]
            const opts = PARSED_OPTIONS[item.kind]
            return (
              <div key={item.id} className={`${card} flex flex-col gap-3 p-5 ${item.needsCheck ? 'ring-2 ring-status-check' : ''}`}>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="flex h-9 w-9 items-center justify-center rounded-md bg-secondary-container/60">
                      <Icon name={meta.icon} className="text-[20px] text-primary" />
                    </span>
                    <span className="text-headline-sm font-bold">{meta.label}</span>
                    {item.needsCheck && <span className="rounded-md bg-status-check px-2.5 py-0.5 text-label-sm font-bold text-white">확인 필요</span>}
                  </div>
                  <button type="button" aria-label="카드 삭제" onClick={() => remove(item.id)} className="flex h-8 w-8 items-center justify-center rounded-md text-outline transition-colors hover:bg-surface-container-low hover:text-error">
                    <Icon name="delete" className="text-[20px]" />
                  </button>
                </div>
                {opts ? (
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="mr-1 text-label-md text-outline">{opts.field}</span>
                    {opts.options.map((o) => (
                      <button
                        key={o.value}
                        type="button"
                        onClick={() => update(item.id, { value: o.value })}
                        className={chip(item.value === o.value)}
                      >
                        {o.label}
                      </button>
                    ))}
                  </div>
                ) : (
                  item.kind !== 'question' && (
                    <input value={item.value} onChange={(e) => update(item.id, { value: e.target.value })} placeholder="값 (예: 30분, 3.2kg)" className="h-11 rounded-xl border border-beige bg-surface-container-lowest px-4 focus:border-primary-container focus:outline-none" />
                  )
                )}
                <input value={item.memo} onChange={(e) => update(item.id, { memo: e.target.value })} placeholder="메모" className="h-11 rounded-xl border border-beige bg-surface-container-low px-4 text-body-sm focus:border-primary-container focus:bg-surface-container-lowest focus:outline-none" />
              </div>
            )
          })}

          <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-dashed border-outline-variant p-4">
            <span className="mr-1 text-label-md text-outline">빠진 항목 추가</span>
            {KINDS.map((k) => (
              <button key={k} type="button" onClick={() => add(k)} className="flex items-center gap-1 rounded-md bg-surface-container-low px-3 py-1.5 text-label-md text-on-surface-variant transition-colors hover:bg-surface-container-high">
                <Icon name="add" className="text-[16px] text-primary" />
                {PARSED_LABEL[k].label}
              </button>
            ))}
          </div>

          <div className="flex items-center justify-between gap-3 pt-2">
            <span className="text-label-sm text-outline">참고 정보이며 진단이 아니에요.</span>
            <div className="flex gap-2">
              <Link to="/records" className={ghostBtn}>
                취소
              </Link>
              <button type="button" onClick={save} disabled={items.length === 0 || saving} className={primaryBtn}>
                <Icon name="task_alt" className="text-[20px]" />
                {items.length}개 기록 저장
              </button>
            </div>
          </div>
        </section>
      )}
    </div>
  )
}
