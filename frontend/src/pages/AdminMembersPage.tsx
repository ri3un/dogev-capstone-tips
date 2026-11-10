import { useState } from 'react'
import { adminApi, authApi } from '../api/endpoints'
import type { AdminMemberRow } from '../api/types'
import Icon from '../components/common/Icon'
import Modal from '../components/common/Modal'
import { card } from '../components/common/styles'
import { useApi } from '../hooks/useApi'

const PROVIDER = { KAKAO: '카카오', NAVER: '네이버', DEV: '개발용' } as const
const dateText = (iso: string | null) => (iso ? iso.slice(0, 10) : '-')

// 관리자 회원 현황: 가입 회원과 입력 데이터 개수·마지막 입력일 (내용은 보여 주지 않음)
export default function AdminMembersPage() {
  const { data: me, loading } = useApi(() => authApi.me(), [])
  const isAdmin = me?.role === 'ADMIN'
  const { data } = useApi(() => (isAdmin ? adminApi.members() : null), [isAdmin])
  const [selected, setSelected] = useState<AdminMemberRow | null>(null)
  const { data: detail } = useApi(() => (selected ? adminApi.member(selected.id) : null), [selected?.id])
  const [hideWithdrawn, setHideWithdrawn] = useState(true)

  if (loading) return null
  if (!isAdmin) return <p className="py-20 text-center text-on-surface-variant">관리자만 볼 수 있는 화면이에요.</p>
  if (!data) return null

  const rows = hideWithdrawn ? data.members.filter((m) => !m.withdrawn) : data.members
  const SUMMARY = [
    { icon: 'group', label: '전체 가입', value: data.memberCount },
    { icon: 'person_check', label: '이용 중', value: data.activeCount },
    { icon: 'person_off', label: '탈퇴', value: data.withdrawnCount },
  ]

  return (
    <div className="mx-auto flex w-full max-w-[1100px] flex-col gap-6 px-5 py-9 lg:px-10">
      <div>
        <p className="mb-1 text-label-md text-secondary">관리자</p>
        <h1 className="text-headline-lg font-bold tracking-tight">회원 현황</h1>
        <p className="mt-1 text-label-md text-outline">개인정보 보호를 위해 입력 내용은 보이지 않고 개수와 마지막 입력일만 보여요.</p>
      </div>

      <section className="grid grid-cols-3 gap-4">
        {SUMMARY.map((s) => (
          <div key={s.label} className={`${card} flex items-center gap-3 p-5`}>
            <Icon name={s.icon} className="text-[24px] text-primary" />
            <div className="flex flex-col">
              <span className="text-label-sm text-on-surface-variant">{s.label}</span>
              <span className="text-headline-sm font-bold">{s.value}명</span>
            </div>
          </div>
        ))}
      </section>

      <section className={`${card} overflow-x-auto p-6`}>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-headline-sm font-bold">회원 목록</h2>
          <label className="flex items-center gap-1.5 text-label-md text-on-surface-variant">
            <input type="checkbox" checked={hideWithdrawn} onChange={(e) => setHideWithdrawn(e.target.checked)} />
            탈퇴 회원 숨기기
          </label>
        </div>
        <table className="w-full min-w-[720px] text-left text-body-sm">
          <thead className="border-b border-outline-variant/40 text-label-md text-outline">
            <tr>
              <th className="py-2.5">닉네임</th>
              <th>로그인</th>
              <th>가입일</th>
              <th className="text-right">입력 데이터</th>
              <th>마지막 입력</th>
              <th />
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/30">
            {rows.map((m) => (
              <tr key={m.id} className={m.withdrawn ? 'text-outline' : ''}>
                <td className="py-3 font-semibold">
                  {m.nickname}
                  {m.role === 'ADMIN' && <span className="ml-1.5 rounded-full bg-primary-container px-2 py-0.5 text-label-sm font-bold text-white">관리자</span>}
                </td>
                <td>{PROVIDER[m.provider]}</td>
                <td>{dateText(m.joinedAt)}</td>
                <td className="text-right font-semibold">{m.totalCount}건</td>
                <td>{dateText(m.lastActivityAt)}</td>
                <td className="text-right">
                  <button type="button" onClick={() => setSelected(m)} className="inline-flex items-center gap-0.5 text-label-md text-on-surface-variant hover:text-primary">
                    자세히 보기
                    <Icon name="chevron_right" className="text-[16px]" />
                  </button>
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={6} className="py-10 text-center text-outline">회원이 없어요.</td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      {selected && (
        <Modal title={`${selected.nickname} 회원`} onClose={() => setSelected(null)}>
          {!detail || detail.member.id !== selected.id ? (
            <p className="py-6 text-center text-outline">불러오는 중…</p>
          ) : (
            <div className="flex flex-col gap-4">
              <p className="text-label-md text-on-surface-variant">
                {PROVIDER[detail.member.provider]} 로그인 · {dateText(detail.member.joinedAt)} 가입
                {detail.member.withdrawn && ' · 탈퇴'}
                {detail.familyName && ` · ${detail.familyName}(${detail.familySize}명, ${detail.familyRole === 'OWNER' ? '관리자' : '편집 가족'})`}
              </p>
              <ul className="flex flex-col divide-y divide-outline-variant/30">
                {detail.counts.map((c) => (
                  <li key={c.key} className="flex items-center justify-between py-2.5">
                    <span>{c.label}</span>
                    <span className="flex items-center gap-4">
                      <span className="font-semibold">{c.count}건</span>
                      <span className="w-24 text-right text-label-sm text-outline">{dateText(c.lastAt)}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </Modal>
      )}
    </div>
  )
}
