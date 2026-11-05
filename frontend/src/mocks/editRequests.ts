// 화면 확인용 예시 데이터입니다. 수정 요청 API(GET /api/me/edit-requests, /api/admin/edit-requests)를 붙이면 지웁니다.

// 장소 조건 10개 (멘토피드백_반영.md 2-2, 결정사항 #8)
export const CONDITIONS = [
  { key: 'size', label: '입장 가능 크기', options: ['소형만', '중형까지', '대형까지'] },
  { key: 'space', label: '동반 공간', options: ['실내 가능', '테라스·야외만'] },
  { key: 'carrier', label: '이동장·유모차 필요', options: ['필요', '필요 없음'] },
  { key: 'leash', label: '목줄 필수', options: ['예', '아니오'] },
  { key: 'manner', label: '매너벨트·기저귀 필요', options: ['필요', '필요 없음'] },
  { key: 'seat', label: '의자·방석 착석', options: ['가능', '불가'] },
  { key: 'offleash', label: '오프리쉬 공간', options: ['있음', '없음'] },
  { key: 'water', label: '물그릇·펫 메뉴', options: ['있음', '없음'] },
  { key: 'parking', label: '주차', options: ['가능', '불가'] },
] as const
export type ConditionKey = (typeof CONDITIONS)[number]['key']
export const CONDITION_LABEL = Object.fromEntries(CONDITIONS.map((c) => [c.key, c.label])) as Record<ConditionKey, string>

export type EditStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export const EDIT_STATUS: Record<EditStatus, { label: string; cls: string }> = {
  PENDING: { label: '대기', cls: 'bg-status-check text-white' },
  APPROVED: { label: '승인', cls: 'bg-status-ok text-white' },
  REJECTED: { label: '반려', cls: 'bg-status-hard text-white' },
}

export interface EditRequest {
  id: number
  placeId: number
  placeName: string
  condition: ConditionKey
  currentValue: string
  requestedValue: string
  memo: string
  requester: string
  date: string
  status: EditStatus
  rejectReason?: string
}

export const EDIT_REQUESTS: EditRequest[] = [
  { id: 4, placeId: 1, placeName: '평촌 멍카페 (예시)', condition: 'parking', currentValue: '정보 없음', requestedValue: '가능', memo: '건물 뒤 공영주차장 이용 가능해요', requester: '엄마', date: '2026-11-05', status: 'PENDING' },
  { id: 3, placeId: 4, placeName: '학의천 산책길 (예시)', condition: 'offleash', currentValue: '있음', requestedValue: '없음', memo: '목줄 필수 안내판이 새로 생겼어요', requester: '아빠', date: '2026-11-03', status: 'PENDING' },
  { id: 2, placeId: 1, placeName: '평촌 멍카페 (예시)', condition: 'size', currentValue: '소형만', requestedValue: '중형까지', memo: '15kg까지 입장 가능하다고 들었어요', requester: '엄마', date: '2026-10-28', status: 'APPROVED' },
  { id: 1, placeId: 5, placeName: '범계 브런치하우스 (예시)', condition: 'space', currentValue: '테라스·야외만', requestedValue: '실내 가능', memo: '', requester: '엄마', date: '2026-10-20', status: 'REJECTED', rejectReason: '매장 공지에 실내 동반 불가로 되어 있어요' },
]

// 근거 블로그 (place_source). 블로그 원문은 저장하지 않고 제목·날짜·링크만 보여 줌
export const SOURCES = [
  { title: '평촌 애견동반 카페 다녀온 후기 (예시)', date: '2026-10-21', url: 'https://blog.naver.com/' },
  { title: '말티즈랑 테라스 카페 나들이 (예시)', date: '2026-10-09', url: 'https://blog.naver.com/' },
  { title: '안양 강아지 동반 카페 정리 (예시)', date: '2026-09-27', url: 'https://blog.naver.com/' },
]

// 지도 필터 더보기: 확인된 조건만 고르기 (TODO: GET /api/places?conditions=)
export const FILTERS: { key: ConditionKey; label: string }[] = [
  { key: 'size', label: '대형견 입장' },
  { key: 'space', label: '실내 동반' },
  { key: 'carrier', label: '이동장 없어도 됨' },
  { key: 'leash', label: '목줄 안 해도 됨' },
  { key: 'manner', label: '매너벨트 없어도 됨' },
  { key: 'seat', label: '의자·방석 착석' },
  { key: 'offleash', label: '오프리쉬 공간' },
  { key: 'water', label: '물그릇·펫 메뉴' },
  { key: 'parking', label: '주차 가능' },
]
// 장소별로 "가능/있음"이 확인된 조건 (예시)
export const PLACE_OK: Record<number, ConditionKey[]> = {
  1: ['space', 'carrier', 'water', 'parking', 'seat'],
  2: ['size', 'offleash', 'water', 'parking'],
  3: ['size', 'space', 'parking', 'water'],
  4: ['size', 'carrier', 'manner'],
  5: ['carrier', 'water'],
}
