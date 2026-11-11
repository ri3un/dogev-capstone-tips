import { useState } from 'react'
import { toast } from '../components/common/toast'
import EmptyState from '../components/common/EmptyState'
import PawLoading from '../components/common/PawLoading'
import { Link, Navigate } from 'react-router'
import { familyApi, questionApi } from '../api/endpoints'
import { localDate } from '../api/format'
import Icon from '../components/common/Icon'
import { card, input, primaryBtn } from '../components/common/styles'
import { useApi } from '../hooks/useApi'
import { useCurrentDog } from '../hooks/useCurrentDog'

// 수의사 질문 메모: 직접 쓴 메모 + AI 상담에서 저장한 질문 (리포트 9번 섹션에 들어감)
export default function QuestionsPage() {
  const { dog, loading } = useCurrentDog()
  const dogId = dog?.id
  const { data, setData, loading: listLoading } = useApi(() => (dogId ? questionApi.list(dogId) : null), [dogId])
  const { data: family } = useApi(() => familyApi.get(), [])
  const [text, setText] = useState('')
  const [saving, setSaving] = useState(false)

  if (loading) return <PawLoading />
  if (!dog) return <Navigate to="/onboarding" replace />

  const list = data ?? []
  const authorOf = (id: number) => family?.members.find((m) => m.memberId === id)?.nickname ?? '가족'

  const add = async () => {
    if (!text.trim() || saving) return
    setSaving(true)
    try {
      const q = await questionApi.create(dog.id, text.trim())
      setData([q, ...list])
      setText('')
      toast('킁킁, 메모했어요!')
    } catch {
      alert('메모를 저장하지 못했어요. 잠시 후 다시 시도해 주세요')
    } finally {
      setSaving(false)
    }
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
        <button type="submit" disabled={!text.trim() || saving} className={`${primaryBtn} h-12 shrink-0 px-5 whitespace-nowrap`}>
          <Icon name="add" className="text-[20px]" />
          추가
        </button>
      </form>

      <section className={`${card} p-2`}>
        {listLoading && !data ? (
          <PawLoading compact />
        ) : list.length === 0 ? (
          <EmptyState title="아직 메모가 없어요">수의사에게 물어볼 것을 적어 두세요</EmptyState>
        ) : (
          <ul className="flex flex-col divide-y divide-outline-variant/30">
            {list.map((q) => (
              <li key={q.id} className="flex items-start gap-3 px-4 py-4">
                <Icon name="help" className="mt-0.5 text-[22px] text-primary" />
                <div className="flex-1">
                  <p className="font-semibold">{q.content}</p>
                  <p className="mt-1 text-label-sm text-outline">
                    {authorOf(q.createdBy)} · {localDate(new Date(q.createdAt))}
                  </p>
                </div>
                <span className={`shrink-0 rounded-md px-2.5 py-0.5 text-label-sm font-bold ${q.source === 'CONSULT' ? 'bg-secondary-container text-on-secondary-container' : 'bg-surface-container text-on-surface-variant'}`}>
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
