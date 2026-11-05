import { useState } from 'react'
import { Link } from 'react-router'
import Icon from '../components/common/Icon'
import { card, input, primaryBtn } from '../components/common/styles'
import { ME } from '../mocks/family'
import { VET_QUESTIONS } from '../mocks/questions'

const today = new Date().toISOString().slice(0, 10)

// 수의사 질문 메모: 직접 쓴 메모 + AI 상담에서 저장한 질문 (리포트 9번 섹션에 들어감)
export default function QuestionsPage() {
  const [list, setList] = useState(VET_QUESTIONS)
  const [text, setText] = useState('')

  // TODO: POST /api/dogs/{dogId}/vet-questions
  const add = () => {
    if (!text.trim()) return
    setList((l) => [{ id: Date.now(), text: text.trim(), author: ME.nickname, date: today, source: 'MEMO' }, ...l])
    setText('')
  }

  return (
    <div className="mx-auto flex w-full max-w-[840px] flex-col gap-6 px-5 py-9 lg:px-6">
      <div>
        <p className="mb-1 text-label-md text-secondary">병원 갈 때 잊지 않게</p>
        <h1 className="text-headline-lg font-bold tracking-tight">수의사 질문 메모</h1>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault()
          add()
        }}
        className={`${card} flex gap-2 p-4`}
      >
        <input value={text} onChange={(e) => setText(e.target.value)} placeholder="다음 병원 갈 때 물어볼 것" className={input} />
        <button type="submit" disabled={!text.trim()} className={`${primaryBtn} h-12 shrink-0 px-5 whitespace-nowrap`}>
          <Icon name="add" className="text-[20px]" />
          추가
        </button>
      </form>

      <section className={`${card} p-2`}>
        {list.length === 0 ? (
          <p className="py-10 text-center text-label-md text-outline">아직 메모가 없어요</p>
        ) : (
          <ul className="flex flex-col divide-y divide-outline-variant/30">
            {list.map((q) => (
              <li key={q.id} className="flex items-start gap-3 px-4 py-4">
                <Icon name="help" className="mt-0.5 text-[22px] text-primary" />
                <div className="flex-1">
                  <p className="font-semibold">{q.text}</p>
                  <p className="mt-1 text-label-sm text-outline">
                    {q.author} · {q.date}
                  </p>
                </div>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-label-sm font-bold ${q.source === 'CONSULT' ? 'bg-secondary-container text-on-secondary-container' : 'bg-surface-container text-on-surface-variant'}`}>
                  {q.source === 'CONSULT' ? 'AI 상담' : '직접 메모'}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <p className="flex items-center justify-center gap-1.5 text-label-sm text-outline">
        <Icon name="description" className="text-[16px]" />
        <Link to="/report" className="underline hover:text-primary">
          건강 리포트
        </Link>
        의 '수의사에게 물어볼 것'에 들어가요.
      </p>
    </div>
  )
}
