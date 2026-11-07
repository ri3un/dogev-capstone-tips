import { useEffect } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import Icon from '../components/common/Icon'
import { card, primaryBtn } from '../components/common/styles'
import { FAMILY, INVITE_PREVIEW, type InviteStatus } from '../mocks/family'

type ViewState = InviteStatus | 'GUEST'
const STATES: ViewState[] = ['GUEST', 'OK', 'ALONE_WITH_DOGS', 'WITH_OTHERS', 'SAME_GROUP', 'FULL', 'INVALID']
// 참여할 수 없는 상태의 안내 문구
const BLOCKED: Partial<Record<ViewState, { icon: string; title: string; body: string }>> = {
  WITH_OTHERS: { icon: 'group_off', title: '지금 그룹을 먼저 나가야 해요', body: '다른 가족과 함께 쓰는 그룹이 있어요. 가족 관리에서 나간 뒤 다시 링크를 열어 주세요.' },
  SAME_GROUP: { icon: 'favorite', title: '이미 이 가족이에요', body: `${INVITE_PREVIEW.groupName} 가족으로 함께하고 있어요.` },
  FULL: { icon: 'block', title: `가족이 가득 찼어요(${FAMILY.maxMembers}명)`, body: '관리자에게 자리를 만들어 달라고 해 주세요.' },
  INVALID: { icon: 'link_off', title: '초대 링크가 올바르지 않아요', body: '링크가 바뀌었거나 취소되었어요. 가족에게 새 링크를 받아 주세요.' },
}

// 초대 수락: 로그인 여부와 서버 미리보기 상태(status)에 따라 화면이 바뀜
export default function InvitePage() {
  const { code = '' } = useParams()
  const [params, setParams] = useSearchParams()
  // TODO: 로그인 여부 + GET /api/family/invites/{code} 응답으로 바꾸기 (지금은 ?state=로 확인)
  const state = (params.get('state') as ViewState) ?? 'OK'
  const blocked = BLOCKED[state]

  // 로그인 후 돌아와서 참여할 수 있게 코드를 저장
  useEffect(() => {
    if (state === 'GUEST') sessionStorage.setItem('inviteCode', code)
  }, [state, code])

  // TODO: POST /api/family/invites/{code}/accept 후 dogStore 비우고 GET /api/dogs
  const accept = () => alert('가족으로 참여했어요! (API 연결 전)')

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-6 px-5 py-12">
      <Link to="/" className="flex items-center gap-2 text-primary">
        <span className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary-container text-on-secondary-container">
          <Icon name="pets" className="text-[22px]" />
        </span>
        <span className="text-headline-lg font-bold tracking-tight">킁킁</span>
      </Link>

      <div className={`${card} flex w-full max-w-[460px] flex-col items-center gap-5 rounded-[28px] px-8 py-10 text-center shadow-[0_12px_40px_-8px_rgba(74,52,38,0.08)]`}>
        {blocked ? (
          <>
            <span className="flex h-16 w-16 items-center justify-center rounded-full bg-surface-container text-primary">
              <Icon name={blocked.icon} className="text-[30px]" />
            </span>
            <div>
              <h1 className="text-headline-md font-bold">{blocked.title}</h1>
              <p className="mt-2 leading-relaxed text-on-surface-variant">{blocked.body}</p>
            </div>
            {state === 'WITH_OTHERS' && (
              <button type="button" disabled className={`${primaryBtn} w-full`}>
                참여하기
              </button>
            )}
            <Link to={state === 'WITH_OTHERS' ? '/my/family' : '/'} className="text-label-lg font-bold text-primary hover:underline">
              {state === 'WITH_OTHERS' ? '가족 관리로 가기' : '홈으로'}
            </Link>
          </>
        ) : (
          <>
            <span className="flex h-16 w-16 items-center justify-center rounded-full bg-secondary-container text-on-secondary-container">
              <Icon name="family_restroom" filled className="text-[32px]" />
            </span>
            <div>
              <h1 className="text-headline-md font-bold leading-snug">
                {INVITE_PREVIEW.inviter}님이 <span className="text-primary">'{INVITE_PREVIEW.groupName}'</span> 가족으로 초대했어요
              </h1>
              <p className="mt-1 text-label-md text-outline">지금 가족 {INVITE_PREVIEW.memberCount}명</p>
            </div>
            <div className="flex flex-wrap justify-center gap-2">
              {INVITE_PREVIEW.dogs.map((d) => (
                <span key={d} className="flex items-center gap-1 rounded-full bg-surface-container-low px-3.5 py-1.5 text-label-md font-semibold">
                  <Icon name="pets" className="text-[16px] text-primary" />
                  {d}
                </span>
              ))}
            </div>

            {state === 'GUEST' ? (
              <div className="flex w-full flex-col gap-2.5">
                <p className="mb-1 text-label-md text-on-surface-variant">로그인하고 참여하기</p>
                <a href="/oauth2/authorization/kakao" className="flex h-[52px] items-center justify-center rounded-[14px] bg-[#FEE500] text-label-lg font-semibold text-black/85">
                  카카오로 시작하기
                </a>
                <a href="/oauth2/authorization/naver" className="flex h-[52px] items-center justify-center rounded-[14px] bg-[#03C75A] text-label-lg font-semibold text-white">
                  네이버로 시작하기
                </a>
              </div>
            ) : (
              <>
                <p className="rounded-xl bg-surface-container-low px-4 py-3 text-label-md text-on-surface-variant">편집 가족으로 참여해요. 기록·프로필을 함께 쓸 수 있어요.</p>
                {state === 'ALONE_WITH_DOGS' && (
                  <p className="flex items-center gap-1.5 text-label-md font-semibold text-secondary">
                    <Icon name="info" className="text-[18px]" />내 반려견 {INVITE_PREVIEW.myDogs.join(', ')}도 함께 옮겨져요
                  </p>
                )}
                <button type="button" onClick={accept} className={`${primaryBtn} w-full`}>
                  <Icon name="group_add" className="text-[20px]" />
                  참여하기
                </button>
              </>
            )}
          </>
        )}
      </div>

      {import.meta.env.DEV && (
        // 개발 중 화면 확인용 상태 전환 (배포 빌드에는 안 보임)
        <div className="flex flex-wrap justify-center gap-1.5 text-label-sm">
          {STATES.map((s) => (
            <button key={s} type="button" onClick={() => setParams({ state: s })} className={`rounded-full px-2.5 py-1 ${s === state ? 'bg-on-surface text-surface' : 'bg-surface-container text-on-surface-variant'}`}>
              {s}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
