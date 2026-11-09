// 장소 화면에서 같이 쓰는 표시용 상수 (키·값은 백엔드 ConditionKey 와 같음)
import type { EditStatus, PlaceCategory, VerdictLevel } from '../api/types'

// 지도 칩: 산책로·공원은 서버에서 PARK 하나
export const CATEGORY_LABEL: Record<PlaceCategory, string> = { CAFE: '카페', RESTAURANT: '식당', PARK: '산책로·공원', ETC: '기타' }
export const CATEGORY_ICON: Record<PlaceCategory, string> = { CAFE: 'local_cafe', RESTAURANT: 'restaurant', PARK: 'park', ETC: 'pets' }

export const VERDICT_BG: Record<VerdictLevel, string> = { OK: 'bg-status-ok', CHECK: 'bg-status-check', HARD: 'bg-status-hard' }
export const VERDICT_LABEL: Record<VerdictLevel, string> = { OK: '가능', CHECK: '확인 필요', HARD: '어려움' }

// 장소 조건 9개 (결정 #8)
export const CONDITIONS = [
  { key: 'size_allowed', label: '입장 가능 크기', options: [['small_only', '소형만'], ['up_to_medium', '중형까지'], ['up_to_large', '대형까지']] },
  { key: 'indoor', label: '동반 공간', options: [['indoor', '실내 가능'], ['outdoor_only', '테라스·야외만']] },
  { key: 'carrier_required', label: '이동장·유모차 필요', options: [['yes', '필요'], ['no', '필요 없음']] },
  { key: 'leash_required', label: '목줄 필수', options: [['yes', '예'], ['no', '아니오']] },
  { key: 'manner_belt_required', label: '매너벨트·기저귀 필요', options: [['yes', '필요'], ['no', '필요 없음']] },
  { key: 'seat_allowed', label: '의자·방석 착석', options: [['yes', '가능'], ['no', '불가']] },
  { key: 'off_leash_area', label: '오프리쉬 공간', options: [['yes', '있음'], ['no', '없음']] },
  { key: 'water_or_pet_menu', label: '물그릇·펫 메뉴', options: [['yes', '있음'], ['no', '없음']] },
  { key: 'parking', label: '주차', options: [['yes', '가능'], ['no', '불가']] },
] as const
export type ConditionKey = (typeof CONDITIONS)[number]['key']

export const conditionLabel = (key: string) => CONDITIONS.find((c) => c.key === key)?.label ?? key

/** 서버 값 → 화면 글자. 예: ('size_allowed', 'up_to_medium') → '중형까지' */
export function valueLabel(key: string, value: string | null) {
  if (!value || value === 'unknown') return '정보 없음'
  if (value === 'NEEDS_CHECK') return '확인 필요'
  const opts: readonly (readonly [string, string])[] = CONDITIONS.find((c) => c.key === key)?.options ?? []
  return opts.find(([v]) => v === value)?.[1] ?? value
}

// 지도 필터 더보기: 서버 conditions 파라미터 "키:값"
export const FILTERS = [
  { key: 'size_allowed:up_to_large', label: '대형견 입장' },
  { key: 'indoor:indoor', label: '실내 동반' },
  { key: 'carrier_required:no', label: '이동장 없어도 됨' },
  { key: 'leash_required:no', label: '목줄 안 해도 됨' },
  { key: 'manner_belt_required:no', label: '매너벨트 없어도 됨' },
  { key: 'seat_allowed:yes', label: '의자·방석 착석' },
  { key: 'off_leash_area:yes', label: '오프리쉬 공간' },
  { key: 'water_or_pet_menu:yes', label: '물그릇·펫 메뉴' },
  { key: 'parking:yes', label: '주차 가능' },
]

export const EDIT_STATUS: Record<EditStatus, { label: string; cls: string }> = {
  PENDING: { label: '대기', cls: 'bg-status-check text-white' },
  APPROVED: { label: '승인', cls: 'bg-status-ok text-white' },
  REJECTED: { label: '반려', cls: 'bg-status-hard text-white' },
}

// 코스 결과 지도 배경 (Stitch 임시 주소)
export const COURSE_MAP_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCuaMjOOsHnDUSJUVnq_Sh1yoLkd2V9pvWtJOUzXjRbHLTqYXQZR0jN_R-uepgeq5RAlQBQt_x8jad11KzuEpjjGQpDrJ5LOQ6sWuRwGloPGNsWmmtk1Wv50v0OvPMPBdgFiY5PfJnDi-p7LbEIq8WjpLLpjHdeatFDaMBpVMMEK82rEnST8SdSes9qhejU27ui6xsYqc28Gmk9trGSSRCQ1MZoZcq_pGjz2UvZ10g'

// 지도 SDK(네이버 지도) 붙이기 전까지 쓰는 배경 이미지. Stitch 임시 주소라 만료될 수 있음
export const MAP_PLACEHOLDER_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDSbu8DAZ3vZHn5DD3kaNcg0KYELZjhPMnxCdbVU_rdGn-FNwvOus6Y18bn_vxuGhxEkbFDJyU0Eo2M8KLrj1jGpSESqjcYQNCJdpZMOAIq38z7P-wU_REEU-ImPM2_f9Fk5RIKfMzly1D480fbWnLHCljJQZq_XZb4DN0Pyy-MJJ_DsqhQOtJmGP7i2fwthr6onSypEzb74hQm6iiZXVyMvkFk4-xA6790_esLNME'
