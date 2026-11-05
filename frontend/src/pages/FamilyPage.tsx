import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import ConfirmModal from '../components/common/ConfirmModal'
import Icon from '../components/common/Icon'
import { card, input } from '../components/common/styles'
import { FAMILY } from '../mocks/family'

type Member = (typeof FAMILY.members)[number]
type Pending = { kind: 'kick' | 'owner'; member: Member } | { kind: 'leave' }

const newCode = () => Array.from({ length: 8 }, () => 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'[Math.floor(Math.random() * 32)]).join('')

// 가족 그룹 관리: 관리자(OWNER)만 이름 변경·내보내기·관리자 넘기기·링크 재발급
export default function FamilyPage() {
  const navigate = useNavigate()
  const [name, setName] = useState(FAMILY.name)
  const [editing, setEditing] = useState(false)
  const [members, setMembers] = useState(FAMILY.members)
  const [myRole, setMyRole] = useState(FAMILY.myRole)
  const [code, setCode] = useState(FAMILY.inviteCode)
  const [menuFor, setMenuFor] = useState<number | null>(null)
  const [pending, setPending] = useState<Pending | null>(null)
  const [copied, setCopied] = useState('')
  const isOwner = myRole === 'OWNER'
  const mustHandOver = isOwner && members.length > 1 // 관리자는 혼자가 아니면 넘긴 뒤에 나갈 수 있음
  const link = `${location.origin}/invite/${code}`

  const copy = (text: string, what: string) => {
    navigator.clipboard?.writeText(text)
    setCopied(what)
    setTimeout(() => setCopied(''), 1500)
  }

  // TODO: DELETE /api/family/members/{id}, PATCH /api/family/owner, DELETE /api/family/members/me
  const confirm = () => {
    if (!pending) return
    if (pending.kind === 'kick') setMembers((list) => list.filter((m) => m.id !== pending.member.id))
    if (pending.kind === 'owner') {
      setMembers((list) => list.map((m) => ({ ...m, role: m.id === pending.member.id ? 'OWNER' : 'EDITOR' })))
      setMyRole('EDITOR')
    }
    if (pending.kind === 'leave' && !mustHandOver) navigate('/onboarding')
    setPending(null)
  }

  const who = pending && 'member' in pending ? pending.member.nickname : ''
  const modal = pending && {
    kick: { title: `${who}님을 내보낼까요?`, body: '그동안 쓴 기록은 남아요. 다시 초대하면 참여할 수 있어요.', confirmLabel: '내보내기', danger: true },
    owner: { title: `${who}님에게 관리자를 넘길까요?`, body: '넘긴 뒤에는 나는 일반 가족이 되고, 이름 변경·내보내기를 할 수 없어요.', confirmLabel: '넘기기' },
    leave: mustHandOver
      ? { title: '먼저 관리자를 넘겨 주세요', body: '관리자는 나가기 전에 구성원 메뉴에서 다른 가족에게 관리자를 넘겨야 해요.', confirmLabel: '확인' }
      : { title: '가족 그룹에서 나갈까요?', body: '나가면 이 그룹의 반려견 기록을 더 이상 볼 수 없어요.', confirmLabel: '나가기', danger: true },
  }[pending.kind]

  return (
    <div className="mx-auto flex w-full max-w-[960px] flex-col gap-6 px-5 py-9 lg:px-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="mb-1 text-label-md text-secondary">가족 그룹</p>
          {editing ? (
            <form
              onSubmit={(e) => {
                e.preventDefault()
                setEditing(false) // TODO: PATCH /api/family
              }}
              className="flex items-center gap-2"
            >
              <input value={name} onChange={(e) => setName(e.target.value)} maxLength={20} autoFocus className={`${input} w-56 text-headline-sm font-bold`} />
              <button type="submit" className="rounded-full bg-primary-container px-4 py-2 text-label-md font-bold text-white">
                저장
              </button>
            </form>
          ) : (
            <h1 className="flex items-center gap-2 text-headline-lg font-bold tracking-tight">
              {name}
              {isOwner && (
                <button type="button" aria-label="이름 바꾸기" onClick={() => setEditing(true)} className="flex h-9 w-9 items-center justify-center rounded-full text-outline hover:bg-surface-container-low hover:text-primary">
                  <Icon name="edit" className="text-[20px]" />
                </button>
              )}
            </h1>
          )}
        </div>
        <span className="rounded-full bg-secondary-container px-4 py-1.5 text-label-lg font-bold text-on-secondary-container">
          가족 {members.length}/{FAMILY.maxMembers}명
        </span>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1fr_360px]">
        <section className={`${card} p-6`}>
          <h2 className="mb-3 text-headline-sm font-bold">구성원</h2>
          <ul className="flex flex-col divide-y divide-outline-variant/30">
            {members.map((m) => {
              const me = m.id === FAMILY.myMemberId
              return (
                <li key={m.id} className="relative flex items-center gap-3 py-3.5">
                  <span className="flex h-11 w-11 items-center justify-center rounded-full bg-surface-container text-label-lg font-bold text-primary">{m.nickname[0]}</span>
                  <div className="flex-1">
                    <div className="flex items-center gap-1.5 font-semibold">
                      {m.nickname}
                      {me && <span className="rounded-full bg-surface-container-high px-2 py-0.5 text-label-sm text-on-surface-variant">나</span>}
                      {m.role === 'OWNER' && <span className="rounded-full bg-primary-container px-2 py-0.5 text-label-sm font-bold text-white">관리자</span>}
                    </div>
                    <span className="text-label-sm text-outline">{m.joinedAt} 참여</span>
                  </div>
                  {isOwner && !me && (
                    <button type="button" aria-label={`${m.nickname} 메뉴`} onClick={() => setMenuFor(menuFor === m.id ? null : m.id)} className="flex h-9 w-9 items-center justify-center rounded-full text-outline hover:bg-surface-container-low">
                      <Icon name="more_vert" className="text-[20px]" />
                    </button>
                  )}
                  {menuFor === m.id && (
                    <div className="absolute top-14 right-0 z-10 w-44 overflow-hidden rounded-xl bg-surface-container-lowest shadow-[0_8px_24px_rgba(74,52,38,0.12)]">
                      {[
                        { kind: 'owner' as const, icon: 'shield_person', label: '관리자 넘기기', cls: '' },
                        { kind: 'kick' as const, icon: 'person_remove', label: '내보내기', cls: 'text-error' },
                      ].map((a) => (
                        <button
                          key={a.kind}
                          type="button"
                          onClick={() => {
                            setPending({ kind: a.kind, member: m })
                            setMenuFor(null)
                          }}
                          className={`flex w-full items-center gap-2 px-4 py-3 text-left text-label-md hover:bg-surface-container-low ${a.cls}`}
                        >
                          <Icon name={a.icon} className="text-[18px]" />
                          {a.label}
                        </button>
                      ))}
                    </div>
                  )}
                </li>
              )
            })}
          </ul>
        </section>

        <div className="flex flex-col gap-6">
          <section className={`${card} p-6`}>
            <h2 className="mb-3 text-headline-sm font-bold">함께 돌보는 반려견</h2>
            <ul className="flex flex-col gap-2">
              {FAMILY.dogs.map((d) => (
                <li key={d.id}>
                  <Link to={`/dogs/${d.id}/edit`} className="flex items-center gap-3 rounded-xl bg-surface-container-low px-4 py-3 hover:bg-surface-container">
                    <Icon name="pets" className="text-[20px] text-primary" />
                    <span className="font-semibold">{d.name}</span>
                    <span className="flex-1 text-label-sm text-outline">{d.breed}</span>
                    <Icon name="chevron_right" className="text-[20px] text-outline" />
                  </Link>
                </li>
              ))}
            </ul>
          </section>

          <section className={`${card} flex flex-col gap-3 p-6`}>
            <h2 className="text-headline-sm font-bold">가족 초대</h2>
            <p className="text-label-md text-outline">링크나 코드를 보내면 바로 참여할 수 있어요. (최대 {FAMILY.maxMembers}명)</p>
            <button type="button" onClick={() => copy(link, 'link')} className="flex items-center gap-2 rounded-xl border border-beige bg-surface-container-low px-4 py-3 text-left text-label-md hover:bg-surface-container">
              <Icon name="link" className="text-[18px] text-primary" />
              <span className="flex-1 truncate">{link}</span>
              <span className="shrink-0 font-bold text-primary">{copied === 'link' ? '복사됨' : '복사'}</span>
            </button>
            <button type="button" onClick={() => copy(code, 'code')} className="flex items-center gap-2 rounded-xl border border-beige bg-surface-container-low px-4 py-3 text-left hover:bg-surface-container">
              <Icon name="key" className="text-[18px] text-primary" />
              <span className="flex-1 font-mono text-body-lg font-bold tracking-[0.25em]">{code}</span>
              <span className="shrink-0 text-label-md font-bold text-primary">{copied === 'code' ? '복사됨' : '복사'}</span>
            </button>
            {isOwner && (
              // TODO: POST /api/family/invites (기존 코드는 바로 막힘)
              <button type="button" onClick={() => setCode(newCode())} className="flex items-center gap-1 self-start text-label-md text-on-surface-variant hover:text-primary">
                <Icon name="refresh" className="text-[18px]" />
                링크 새로 만들기
              </button>
            )}
          </section>

          <button type="button" onClick={() => setPending({ kind: 'leave' })} className="flex items-center justify-center gap-1.5 rounded-full py-3 text-label-lg font-semibold text-error hover:bg-error-container/50">
            <Icon name="logout" className="text-[20px]" />
            가족 그룹 나가기
          </button>
        </div>
      </div>

      {pending && modal && <ConfirmModal {...modal} onConfirm={confirm} onClose={() => setPending(null)} />}
    </div>
  )
}
