import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import Icon from '../components/common/Icon'
import { card, input, primaryBtn } from '../components/common/styles'

// 온보딩: 첫 로그인 후 반려견이 없을 때 — 직접 등록 또는 가족 초대 코드로 참여
export default function OnboardingPage() {
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const valid = /^[A-Z0-9]{8}$/.test(code)

  return (
    <div className="mx-auto flex w-full max-w-[880px] flex-col items-center gap-9 px-5 py-14 text-center lg:px-10">
      <div className="flex flex-col items-center gap-3">
        <span className="flex h-16 w-16 items-center justify-center rounded-full bg-secondary-container text-on-secondary-container shadow-[0_4px_16px_rgba(200,134,75,0.12)]">
          <Icon name="pets" filled className="text-[32px]" />
        </span>
        <h1 className="text-headline-xl font-bold tracking-tight">킁킁에 오신 걸 환영해요!</h1>
        <p className="text-body-lg text-on-surface-variant">함께할 반려견을 먼저 알려 주세요. 가족이 이미 등록했다면 초대 코드로 바로 참여할 수 있어요.</p>
      </div>

      <div className="grid w-full grid-cols-1 gap-5 md:grid-cols-2">
        <Link to="/dogs/new" className={`${card} group flex flex-col items-start gap-4 p-7 text-left transition-all hover:-translate-y-1 hover:shadow-[0_12px_30px_-8px_rgba(74,52,38,0.12)]`}>
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-container text-on-primary">
            <Icon name="add" className="text-[26px]" />
          </span>
          <div>
            <h2 className="text-headline-sm font-bold">반려견 등록하기</h2>
            <p className="mt-1 text-on-surface-variant">이름, 견종, 몸무게 등 6가지만 입력하면 끝나요.</p>
          </div>
          <span className="mt-auto flex items-center gap-1 text-label-lg font-bold text-primary">
            시작하기 <Icon name="arrow_forward" className="text-[18px] transition-transform group-hover:translate-x-1" />
          </span>
        </Link>

        <form
          onSubmit={(e) => {
            e.preventDefault()
            if (valid) navigate(`/invite/${code}`)
          }}
          className={`${card} flex flex-col items-start gap-4 p-7 text-left`}
        >
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-secondary-container text-on-secondary-container">
            <Icon name="group_add" className="text-[26px]" />
          </span>
          <div>
            <h2 className="text-headline-sm font-bold">가족 초대 코드로 참여</h2>
            <p className="mt-1 text-on-surface-variant">가족에게 받은 8자리 코드를 입력해 주세요.</p>
          </div>
          <div className="mt-auto flex w-full gap-2">
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8))}
              placeholder="예: KUNG2026"
              aria-label="초대 코드"
              className={`${input} font-mono tracking-[0.2em]`}
            />
            <button type="submit" disabled={!valid} className={`${primaryBtn} h-12 shrink-0 px-5 whitespace-nowrap`}>
              참여
            </button>
          </div>
        </form>
      </div>

      <p className="text-label-sm text-outline">반려견은 나중에 마이페이지에서 더 추가할 수 있어요.</p>
    </div>
  )
}
