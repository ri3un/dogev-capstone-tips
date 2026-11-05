// 화면 확인용 예시 데이터입니다. 수정 요청 API(/api/place-requests, /api/admin/place-requests)를 붙이면 지웁니다.

export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

// 장소 세부 조건 10개 (결정사항 #8) 중 사용자가 고칠 수 있는 항목
export interface ConditionField {
  key: string
  label: string
  options: string[]
}

export const CONDITION_FIELDS: ConditionField[] = [
  { key: 'size', label: '입장 가능 크기', options: ['소형만', '중형까지', '대형까지', '정보 없음'] },
  { key: 'space', label: '동반 공간', options: ['실내 가능', '테라스·야외만', '정보 없음'] },
  { key: 'carrier', label: '이동장·유모차 필요', options: ['필요', '필요 없음', '정보 없음'] },
  { key: 'leash', label: '목줄 필수', options: ['예', '아니오', '정보 없음'] },
  { key: 'mannerBelt', label: '매너벨트·기저귀 필요', options: ['필요', '필요 없음', '정보 없음'] },
  { key: 'seat', label: '반려견 의자·방석 위 착석', options: ['가능', '불가', '정보 없음'] },
  { key: 'offLeash', label: '오프리쉬 공간', options: ['있음', '없음', '정보 없음'] },
  { key: 'petMenu', label: '물그릇·펫 메뉴', options: ['있음', '없음', '정보 없음'] },
  { key: 'parking', label: '주차', options: ['가능', '불가', '정보 없음'] },
]

// 장소별 현재 조건 값 (TODO: GET /api/places/{id} 응답의 conditions)
export const PLACE_CONDITIONS: Record<string, string> = {
  size: '소형만',
  space: '테라스·야외만',
  carrier: '필요 없음',
  leash: '예',
  mannerBelt: '필요',
  seat: '정보 없음',
  offLeash: '있음',
  petMenu: '있음',
  parking: '정보 없음',
}

export interface PlaceEditRequest {
  id: number
  placeId: number
  placeName: string
  field: string // 조건 라벨
  before: string
  after: string
  note: string
  requester: string
  createdAt: string
  status: RequestStatus
  rejectReason?: string
}

export const MY_REQUESTS: PlaceEditRequest[] = [
  { id: 13, placeId: 1, placeName: '평촌 멍카페 (예시)', field: '주차', before: '정보 없음', after: '가능', note: '건물 지하 주차장 2시간 무료예요.', requester: '엄마', createdAt: '2026-11-04', status: 'PENDING' },
  { id: 9, placeId: 4, placeName: '학의천 산책길 (예시)', field: '목줄 필수', before: '아니오', after: '예', note: '입구에 목줄 필수 안내판이 새로 생겼어요.', requester: '엄마', createdAt: '2026-10-29', status: 'APPROVED' },
  { id: 6, placeId: 1, placeName: '평촌 멍카페 (예시)', field: '입장 가능 크기', before: '소형만', after: '대형까지', note: '', requester: '엄마', createdAt: '2026-10-20', status: 'REJECTED', rejectReason: '매장 공지에 10kg 미만만 입장 가능하다고 되어 있어요.' },
]

// 관리자 알림창에 뜨는 전체 요청 (다른 사용자 요청 포함)
export const ADMIN_REQUESTS: PlaceEditRequest[] = [
  MY_REQUESTS[0],
  { id: 12, placeId: 2, placeName: '안양천 도그 그라운드 (예시)', field: '물그릇·펫 메뉴', before: '없음', after: '있음', note: '음수대 옆에 강아지용 물그릇이 있어요.', requester: '콩이아빠', createdAt: '2026-11-03', status: 'PENDING' },
  { id: 11, placeId: 4, placeName: '학의천 산책길 (예시)', field: '주차', before: '불가', after: '가능', note: '', requester: '해피맘', createdAt: '2026-11-02', status: 'PENDING' },
  ...MY_REQUESTS.slice(1),
]

export const REQUEST_STATUS: Record<RequestStatus, { label: string; cls: string }> = {
  PENDING: { label: '검토 중', cls: 'bg-surface-container text-on-surface-variant' },
  APPROVED: { label: '반영됨', cls: 'bg-tertiary-container/20 text-tertiary' },
  REJECTED: { label: '반려', cls: 'bg-error-container/60 text-error' },
}

// 근거 블로그 글 (원문 재게시 없이 제목·출처·판정 근거만 보여 줌)
export interface Evidence {
  id: number
  title: string
  source: '네이버 블로그' | '카카오 블로그' | 'TourAPI' | '플레이스 리뷰'
  date: string
  url: string
  findings: string[] // 이 글에서 판정한 조건
}

export const EVIDENCE: Evidence[] = [
  { id: 1, title: '평촌 강아지 동반 카페 다녀온 후기', source: '네이버 블로그', date: '2026-10-21', url: 'https://blog.naver.com/', findings: ['소형만', '오프리쉬 있음', '매너벨트 필요'] },
  { id: 2, title: '말티즈랑 테라스 카페 나들이', source: '카카오 블로그', date: '2026-10-12', url: 'https://brunch.co.kr/', findings: ['테라스·야외만', '펫 메뉴 있음'] },
  { id: 3, title: '반려동물 동반여행 정보', source: 'TourAPI', date: '2026-09-30', url: 'https://api.visitkorea.or.kr/', findings: ['소형만', '목줄 필수'] },
]

// 내가 관리자인지 (TODO: GET /api/members/me 의 role === 'ADMIN')
export const IS_ADMIN = true
