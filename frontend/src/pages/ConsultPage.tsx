import { useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router'
import { consultApi, questionApi } from '../api/endpoints'
import { ageText, localDate, monthDay, todayStr } from '../api/format'
import type { Consultation } from '../api/types'
import Icon from '../components/common/Icon'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'
import { SYMPTOM_CHIPS } from '../mocks/consult'

const softBorder = 'border border-beige bg-surface-container-lowest'
const MODEL_COLORS = ['text-primary', 'text-secondary', 'text-tertiary']
const STATUS_TEXT = { OK: '', TIMEOUT: '시간 안에 답하지 못했어요.', INVALID: '답변 형식이 맞지 않아 뺐어요.', ERROR: '답변을 받지 못했어요.' } as const

// 지난 상담 날짜: 오늘이면 "오늘 오후 02:40", 아니면 "10월 12일"
const whenText = (iso: string) => {
  const d = new Date(iso)
  if (localDate(d) !== todayStr()) return monthDay(iso)
  const h = d.getHours()
  return `오늘 ${h < 12 ? '오전' : '오후'} ${String(h % 12 || 12).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 결론 한 줄: 응급 > 병원 권고 > 지켜보기
const headlineOf = (r: Consultation) =>
  r.emergency ? '바로 동물병원에 가 주세요' : r.visitVet ? '동물병원 진료를 권해요' : '집에서 지켜봐도 괜찮아 보여요'

// AI 상담 (집중형 단일 컬럼, Stitch 05-consult)
export default function ConsultPage() {
  const navigate = useNavigate()
  const consultId = Number(useParams().consultId) || null
  const [keyword, setKeyword] = useState('')
  const [question, setQuestion] = useState('')
  const [compareOpen, setCompareOpen] = useState(false)
  const [saved, setSaved] = useState(false)
  const [asking, setAsking] = useState(false)

  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const { data: history, reload: reloadHistory } = useApi(() => (dogId ? consultApi.list(dogId) : null), [dogId])
  const { data: memos } = useApi(() => (dogId ? questionApi.list(dogId) : null), [dogId])
  const { data: loaded, setData: setResult } = useApi(() => (consultId ? consultApi.get(consultId) : null), [consultId])
  const R = consultId ? loaded : undefined // '새 상담'으로 /consult 에 오면 이전 답변을 숨김
  const reset = () => {
    setSaved(false)
    setCompareOpen(false)
  }

  if (loading) return null
  if (!dog) return <Navigate to="/onboarding" replace />

  // 과거 상담·질문 메모에서 검색어가 들어간 것
  const k = keyword.trim()
  const matches = k
    ? [
        ...(history ?? []).filter((h) => h.question.includes(k)).map((h) => ({ id: `c${h.id}`, date: localDate(new Date(h.createdAt)), level: h.urgencyLabel, strong: h.urgency === 'HIGH' || h.urgency === 'EMERGENCY', text: h.question })),
        ...(memos ?? []).filter((m) => m.content.includes(k)).map((m) => ({ id: `q${m.id}`, date: localDate(new Date(m.createdAt)), level: '질문 메모', strong: false, text: m.content })),
      ]
    : []

  const ask = async () => {
    if (!question.trim() || asking) return
    setAsking(true)
    try {
      const r = await consultApi.ask(dog.id, question.trim())
      setResult(r)
      reset()
      reloadHistory()
      navigate(`/consult/${r.id}`)
    } catch (e) {
      alert((e as { message?: string }).message ?? 'AI 답변을 받지 못했어요. 잠시 후 다시 시도해 주세요')
    } finally {
      setAsking(false)
    }
  }

  const save = async () => {
    if (!R || saved) return
    try {
      await questionApi.create(dog.id, R.question, 'CONSULT')
      setSaved(true)
    } catch {
      alert('질문 메모에 저장하지 못했어요')
    }
  }

  return (
    <div className="relative flex w-full flex-col">
      {/* 지난 상담 내역: 마우스를 올리면 펼쳐집니다. */}
      <aside className="group fixed top-20 bottom-0 left-0 z-40 flex w-16 flex-col overflow-hidden bg-surface-container-lowest shadow-[8px_0_32px_rgba(74,52,38,0.07)] transition-all duration-300 hover:w-72">
        <div className="flex h-full flex-col p-3.5">
          <div className="mb-6 flex items-center gap-3.5 px-1 py-1">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-container text-primary transition-transform group-hover:scale-105">
              <Icon name="history" className="text-[20px]" />
            </div>
            <div className="flex min-w-0 flex-col whitespace-nowrap opacity-0 transition-opacity duration-200 group-hover:opacity-100">
              <span className="text-label-lg font-semibold tracking-tight">지난 상담 내역</span>
              <span className="text-label-sm text-outline">{dog.name}의 상담 기록</span>
            </div>
          </div>
          <div className="flex-1 space-y-2 overflow-y-auto pr-1 opacity-0 transition-opacity duration-200 group-hover:opacity-100">
            {(history ?? []).map((h) => {
              const on = h.id === consultId // 지금 보고 있는 상담
              return (
                <Link key={h.id} to={`/consult/${h.id}`} onClick={reset} className={`block rounded-xl p-3 transition-all ${on ? 'bg-surface-container-low hover:bg-surface-container' : 'hover:bg-surface-container-low'}`}>
                  <div className="mb-1 flex items-center justify-between text-label-sm text-outline">
                    {whenText(h.createdAt)}
                    {on && <span className="h-1.5 w-1.5 rounded-full bg-secondary-container" />}
                  </div>
                  <p className={`line-clamp-1 text-body-sm ${on ? 'font-medium' : 'text-on-surface-variant'}`}>{h.question}</p>
                </Link>
              )
            })}
          </div>
          <div className="mt-auto pt-4 opacity-0 transition-opacity duration-200 group-hover:opacity-100">
            <button
              type="button"
              onClick={() => {
                setQuestion('')
                reset()
                navigate('/consult')
              }} className="flex w-full items-center justify-center gap-2 rounded-xl bg-surface-container px-3 py-2.5 text-label-md text-on-surface-variant transition-all hover:bg-surface-container-high">
              <Icon name="add" className="text-[18px] text-primary" />
              새 상담 시작하기
            </button>
          </div>
        </div>
      </aside>

      <div className="mx-auto flex w-full max-w-[840px] flex-col gap-6 px-5 py-8 md:py-10 lg:px-6">
        <div className="flex w-full items-center gap-3 rounded-2xl bg-secondary-container/30 px-4 py-3.5 shadow-[0_2px_12px_rgba(74,52,38,0.03)] sm:px-5 sm:py-4">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-secondary-container">
            <Icon name="verified_user" className="text-[18px] text-secondary" />
          </div>
          <p className="flex-1 leading-relaxed text-on-secondary-container">
            <strong className="font-semibold text-secondary">참고 정보이며 진단이 아니에요.</strong> 응급 상황이거나 심한 통증이 보이면 바로 동물병원에 가주세요.
          </p>
        </div>

        {/* 증상 검색 + 과거 상담·질문 메모 (TODO: GET /api/consults?keyword=) */}
        <section className="flex w-full flex-col gap-3">
          <div className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 shadow-[0_2px_12px_rgba(74,52,38,0.03)] transition-all focus-within:border-primary-container focus-within:shadow-[0_4px_16px_rgba(200,134,75,0.12)] ${softBorder}`}>
            <Icon name="search" className="shrink-0 text-[22px] text-primary-container" />
            <input aria-label="증상 검색" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="증상으로 검색해보세요 (예: 구토, 다리 절뚝임, 기침)" className="w-full bg-transparent placeholder:text-outline/70 focus:outline-none" />
            {keyword && (
              <button type="button" aria-label="검색어 지우기" onClick={() => setKeyword('')} className="p-1 text-outline transition-colors hover:text-on-surface">
                <Icon name="cancel" className="text-[18px]" />
              </button>
            )}
          </div>
          <div className="flex items-center gap-2 overflow-x-auto px-1 pb-1">
            <span className="mr-1 shrink-0 text-label-sm text-outline">추천 증상</span>
            {SYMPTOM_CHIPS.map((s) => {
              const on = keyword.includes(s)
              return (
                <button
                  key={s}
                  type="button"
                  onClick={() => setKeyword(s)}
                  className={`shrink-0 rounded-full border px-3 py-1.5 text-label-md transition-all ${
                    on ? 'border-primary-container/40 bg-secondary-container/60 font-bold text-primary shadow-[0_1px_6px_rgba(74,52,38,0.04)]' : 'border-beige bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container hover:text-primary'
                  }`}
                >
                  {s}
                </button>
              )
            })}
          </div>

          {keyword && (
            <div className={`flex w-full flex-col gap-3.5 rounded-2xl p-4 shadow-[0_4px_20px_rgba(74,52,38,0.04)] sm:p-5 ${softBorder}`}>
              <div className="flex items-center justify-between border-b border-surface-container-high/60 pb-2">
                <div className="flex items-center gap-2">
                  <Icon name="manage_search" className="text-[18px] text-primary" />
                  <h3 className="text-label-lg font-bold">
                    과거 상담 및 질문 메모 검색 결과 <span className="text-primary">{matches.length}건</span>
                  </h3>
                </div>
                <span className="text-label-sm text-outline">'{keyword}' 일치 항목</span>
              </div>
              <div className="flex flex-col gap-2.5">
                {matches.map((m) => (
                  <div key={m.id} className="flex flex-col justify-between gap-2 rounded-xl bg-surface-container-low/70 p-3.5 transition-all hover:bg-surface-container sm:flex-row sm:items-center">
                    <div className="flex min-w-0 flex-col gap-1">
                      <div className="flex items-center gap-2 text-label-sm">
                        <span className="text-outline">{m.date}</span>
                        <span className={`inline-flex items-center rounded-full px-2 py-0.5 font-semibold ${m.strong ? 'bg-secondary-container/60 text-secondary' : 'bg-surface-container text-on-surface-variant'}`}>{m.level}</span>
                      </div>
                      <p className="line-clamp-1 font-medium">{m.text}</p>
                    </div>
                    <button type="button" onClick={() => setQuestion(m.text)} className="inline-flex shrink-0 items-center gap-1 text-label-md text-primary hover:underline sm:self-center">
                      새로 물어보기
                      <Icon name="arrow_forward" className="text-[16px]" />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>

        {/* 질문 입력 */}
        <div className="flex w-full flex-col gap-4 rounded-2xl bg-surface-container-lowest p-5 shadow-[0_8px_30px_rgba(74,52,38,0.04)] transition-all focus-within:shadow-[0_12px_36px_rgba(200,134,75,0.1)] md:p-6">
          <div className="flex items-center justify-between">
            <div className="inline-flex items-center gap-2 rounded-full bg-surface-container px-3 py-1.5 text-label-md">
              <span className="h-2 w-2 rounded-full bg-primary-container" />
              <span>
                상담 대상: <strong>{dog.name}</strong> ({dog.breed} · {ageText(dog.ageMonths)})
              </span>
            </div>
            <span className="flex items-center gap-1 text-label-sm text-outline">
              <Icon name="check_circle" className="text-[15px] text-tertiary" />
              지병·알레르기 정보 반영
            </span>
          </div>
          <textarea
            rows={3}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder={`${dog.name}의 증상이나 궁금한 점을 적어주세요 (예: 산책 다녀온 후 뒷다리를 살짝 터는 듯한 행동을 보여요)`}
            className="w-full resize-none bg-transparent text-body-lg leading-relaxed transition-all placeholder:text-outline/70 focus:outline-none"
          />
          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <div className="flex items-center gap-2 text-label-sm text-outline">
              <Icon name="tune" className="text-[16px]" />
              무료 AI 3개 교차 검증
            </div>
            <button type="button" onClick={ask} disabled={!question.trim() || asking} className="inline-flex items-center gap-2 rounded-full bg-primary-container px-6 py-3 text-label-lg text-on-primary shadow-[0_4px_16px_rgba(200,134,75,0.25)] transition-all hover:bg-primary hover:shadow-[0_6px_20px_rgba(200,134,75,0.35)] active:scale-[0.98] disabled:opacity-50">
              <Icon name="neurology" className="text-[18px]" />
              {asking ? 'AI 3개에게 묻는 중…' : '3개 AI에게 물어보기'}
            </button>
          </div>
        </div>

        {/* 답변 카드 */}
        {R && (
        <article className="flex w-full flex-col gap-6 rounded-2xl bg-surface-container-lowest p-6 shadow-[0_10px_32px_rgba(74,52,38,0.06)] md:p-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="inline-flex items-center gap-1.5 rounded-full bg-secondary-container/50 px-3.5 py-1.5 text-label-lg font-bold text-on-secondary-container">
              <Icon name="visibility" className="text-[18px] text-secondary" />
              {R.urgencyLabel}
            </div>
            <div className="inline-flex items-center gap-2 rounded-full bg-surface-container-low px-3 py-1.5 text-label-md text-on-surface-variant">
              <div className="flex items-center gap-1">
                {Array.from({ length: R.totalModels }, (_, i) => (
                  <span key={i} className={`h-2 w-2 rounded-full ${i < R.answeredCount ? 'bg-tertiary' : 'bg-outline-variant'}`} />
                ))}
              </div>
              <span>
                AI {R.totalModels}개 중 <strong>{R.answeredCount}개</strong>가 답했어요
              </span>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="text-label-sm tracking-wider text-outline">{R.question}</span>
            <h2 className="text-headline-lg font-bold tracking-tight">{headlineOf(R)}</h2>
          </div>

          <ol className="flex flex-col gap-3.5 rounded-xl bg-surface-container-low/70 p-5 md:p-6">
            {R.summary.map((line, i) => (
              <li key={i} className="flex items-start gap-3">
                <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary-container text-label-sm text-on-primary">{i + 1}</span>
                <p className="leading-relaxed">{line}</p>
              </li>
            ))}
          </ol>

          <div className="rounded-xl bg-surface-container-low transition-all">
            <button type="button" aria-expanded={compareOpen} onClick={() => setCompareOpen((v) => !v)} className="flex w-full items-center justify-between px-5 py-4 text-left text-label-lg text-on-surface-variant transition-colors hover:text-on-surface">
              <div className="flex items-center gap-2.5">
                <Icon name="analytics" className="text-[19px] text-primary" />
                AI별 답변 비교 보기
                <span className="text-label-sm text-outline">({R.answers.map((a) => a.model).join(' · ')} 종합)</span>
              </div>
              <Icon name="expand_more" className={`text-[20px] text-outline transition-transform duration-200 ${compareOpen ? 'rotate-180' : ''}`} />
            </button>
            {compareOpen && (
              <div className="space-y-3 px-5 pt-1 pb-5 text-body-sm text-on-surface-variant">
                {R.answers.map((a, i) => (
                  <div key={a.model} className="rounded-lg bg-surface-container-lowest p-3.5">
                    <div className={`mb-1 text-label-md font-bold ${MODEL_COLORS[i % MODEL_COLORS.length]}`}>{a.model}</div>
                    <p className="leading-relaxed">{a.answer ?? STATUS_TEXT[a.status]}</p>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex flex-col justify-between gap-3 pt-2 sm:flex-row sm:items-center">
            <div className="flex items-center gap-2 text-body-sm text-outline">
              <Icon name="local_hospital" className="text-[18px] text-primary" />
              동물병원 진료 시 수의사에게 바로 보여줄 수 있어요
            </div>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={save}
                className={`inline-flex items-center justify-center gap-2 rounded-full px-5 py-2.5 text-label-lg transition-all active:scale-[0.98] ${saved ? 'bg-primary-fixed text-on-primary-fixed' : 'bg-surface-container hover:bg-surface-container-high'}`}
              >
                <Icon name={saved ? 'bookmark' : 'bookmark_border'} filled={saved} className="text-[18px] text-primary" />
                {saved ? '메모 저장 완료' : '질문 메모에 저장'}
              </button>
              {saved && (
                <Link to="/questions" className="text-label-md font-semibold text-primary hover:underline">
                  메모 보기
                </Link>
              )}
            </div>
          </div>
        </article>
        )}

        <p className="py-2 text-center text-body-sm text-outline">킁킁 AI는 무료 AI 3개의 답변을 비교해 참고용 사전 가이드를 제공합니다.</p>
      </div>
    </div>
  )
}
