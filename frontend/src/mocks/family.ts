// 화면 확인용 예시 데이터입니다. GET /api/family, GET /api/family/invites/{code}를 붙이면 지웁니다.

export type FamilyRole = 'OWNER' | 'EDITOR'

export const FAMILY = {
  name: '꿍이네',
  myMemberId: 1,
  myRole: 'OWNER' as FamilyRole,
  maxMembers: 6,
  inviteCode: 'KUNG2026',
  members: [
    { id: 1, nickname: '엄마', role: 'OWNER' as FamilyRole, joinedAt: '2026-10-01' },
    { id: 2, nickname: '아빠', role: 'EDITOR' as FamilyRole, joinedAt: '2026-10-03' },
    { id: 3, nickname: '언니', role: 'EDITOR' as FamilyRole, joinedAt: '2026-10-12' },
  ],
  dogs: [{ id: 1, name: '꿍', breed: '말티즈' }],
}

// 초대 미리보기 상태 (서버 응답 status)
export type InviteStatus = 'OK' | 'ALONE_WITH_DOGS' | 'WITH_OTHERS' | 'SAME_GROUP' | 'FULL' | 'INVALID'

export const INVITE_PREVIEW = {
  inviter: '엄마',
  groupName: '꿍이네',
  dogs: ['꿍'],
  memberCount: 3,
  myDogs: ['콩'], // ALONE_WITH_DOGS일 때 함께 옮겨지는 내 반려견
}

// 마이페이지 내 정보 (TODO: GET /api/members/me)
export const ME = { nickname: '엄마', provider: 'kakao' as 'kakao' | 'naver', role: 'ADMIN' as 'USER' | 'ADMIN' } // 시연용으로 ADMIN
