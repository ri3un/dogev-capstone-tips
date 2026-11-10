import { Link } from 'react-router'
import type { Family } from '../../api/types'
import Icon from '../common/Icon'
import Modal from '../common/Modal'

// 홈 "프로필 자세히 보기": 가족 구성원 전체(관리자·참여일·나)를 보여 주는 창
export default function FamilyMembersModal({ family, myId, onClose }: { family: Family; myId?: number; onClose: () => void }) {
  // 관리자 먼저, 그다음 먼저 들어온 순
  const members = [...family.members].sort((a, b) => (a.role === b.role ? a.joinedAt.localeCompare(b.joinedAt) : a.role === 'OWNER' ? -1 : 1))
  return (
    <Modal title={`${family.name} 가족 ${members.length}/${family.maxMembers}명`} onClose={onClose}>
      <ul className="flex flex-col divide-y divide-outline-variant/30">
        {members.map((m) => (
          <li key={m.memberId} className="flex items-center gap-3 py-3">
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-secondary-container text-label-lg font-bold text-on-secondary-container">{m.nickname[0]}</span>
            <div className="flex-1">
              <div className="flex items-center gap-1.5 font-semibold">
                {m.nickname}
                {m.memberId === myId && <span className="rounded-full bg-surface-container-high px-2 py-0.5 text-label-sm text-on-surface-variant">나</span>}
                {m.role === 'OWNER' ? (
                  <span className="rounded-full bg-primary-container px-2 py-0.5 text-label-sm font-bold text-white">관리자</span>
                ) : (
                  <span className="rounded-full bg-surface-container px-2 py-0.5 text-label-sm text-on-surface-variant">편집 가족</span>
                )}
              </div>
              <span className="text-label-sm text-outline">{m.joinedAt.slice(0, 10)} 참여</span>
            </div>
          </li>
        ))}
      </ul>
      <Link to="/my/family" className="mt-4 flex items-center justify-center gap-1 rounded-full bg-surface-container-low py-3 text-label-lg font-semibold text-on-surface-variant hover:bg-surface-container">
        <Icon name="group_add" className="text-[20px]" />
        가족 관리·초대하기
      </Link>
    </Modal>
  )
}
