// 화면 확인용 예시 데이터입니다. 장소 API(GET /api/places)를 붙이면 지웁니다.
// 이미지는 Stitch 임시 주소라 만료될 수 있습니다(깨지면 배경색만 보임).
// TODO: 시연 지역 안양의 실제 수집 데이터(data/place)로 바꿉니다.

export type PlaceCategory = 'cafe' | 'restaurant' | 'trail' | 'park'
// 우리 강아지 기준 동반 판정(서버 다수결 결과). NEEDS_CHECK = 확인 필요
export type PetStatus = 'OK' | 'NEEDS_CHECK' | 'HARD'
export type BadgeTone = 'green' | 'apricot' | 'caramel'

export interface Place {
  id: number
  name: string
  category: PlaceCategory
  area: string
  distanceM: number
  status: PetStatus
  imageUrl?: string
  badges: { icon: string; label: string; tone: BadgeTone }[]
  summary: string[] // 블로그 리뷰 LLM 요약 3줄
  evidenceCount: number
  pin?: { top: string; left: string; icon: string } // 지도 SDK 붙이기 전 임시 위치(%)
}

export const CATEGORY_LABEL: Record<PlaceCategory, string> = {
  cafe: '카페',
  restaurant: '식당',
  trail: '산책로',
  park: '공원',
}

export const STATUS_BG: Record<PetStatus, string> = { OK: 'bg-status-ok', NEEDS_CHECK: 'bg-status-check', HARD: 'bg-status-hard' }
export const STATUS_LABEL: Record<PetStatus, string> = { OK: '가능', NEEDS_CHECK: '확인 필요', HARD: '어려움' }

// 거리 표시: 1km 미만은 m, 이상은 소수 첫째 자리 km
export const formatDistance = (m: number) => (m < 1000 ? `${m}m` : `${(m / 1000).toFixed(1)}km`)

export const PLACES: Place[] = [
  {
    id: 1,
    name: '평촌 멍카페 (예시)',
    category: 'cafe',
    area: '평촌동',
    distanceM: 1200,
    status: 'OK',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDXUQuGPNeSGZkOM32wz-VNpDuY1X6Lcw8rhBOSWfKnd-IwJVWJbi4dVLXC3eod1bBML0ZkOUlif-3RvV2TQKa48-0NUWn0w1EdFslckfihYXIefO_uygYB_33mjiF-rGyQsj10ANqHPoUfaeWV-lLnRXr2OcF2Lb81zLaksWbG5cLFAHbiqHf-Kr6Od_Y46cg2XhZeopYnHW_VAtwH7bYSkEVIov6cDUNh5Kc1KcQ',
    badges: [
      { icon: 'verified', label: '소형견 동반 가능', tone: 'green' },
      { icon: 'deck', label: '오프리쉬 테라스', tone: 'apricot' },
      { icon: 'local_cafe', label: '멍푸치노 제공', tone: 'caramel' },
    ],
    summary: [
      '테라스 바닥이 미끄럽지 않아 소형견이 뛰놀기 좋아요.',
      '유모차 반입 가능하며 리드줄 고정 고리가 비치되어 있어요.',
      '실내 마킹 방지 매너벨트 착용 필수입니다.',
    ],
    evidenceCount: 12,
    pin: { top: '38%', left: '34%', icon: 'pets' },
  },
  {
    id: 2,
    name: '안양천 도그 그라운드 (예시)',
    category: 'park',
    area: '석수동',
    distanceM: 2500,
    status: 'OK',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCy_UfHRJ0nbHyz2O3jz275VqTp-QCVZxu1VVxSvXguUbbLNRJs6JyVciuCi6ijD_gfB91SO9J4XbXusLJaRlKbOWuk7FduruY-jK6XpwbmvRqDZFY7wP9NG7TpxfJMCYumLatAkV62Id4mdGhnoDbW8oLXy243vFHENNvmdGFETNWZGstp_EVYPWzQtcIFr-fAdivCR6vnnOpVTLFSJ9Kao5e_wzix7w2-bt2sj-Y',
    badges: [{ icon: 'fence', label: '소형견 분리 구역', tone: 'green' }],
    summary: ['소형견 전용 천연 잔디 구역이 있어요.', '이중 안전문이 있어 안심돼요.', '주말 오후에는 붐벼요.'],
    evidenceCount: 8,
    pin: { top: '58%', left: '52%', icon: 'park' },
  },
  {
    id: 3,
    name: '수리산 멍스테이 (예시)',
    category: 'restaurant',
    area: '안양동',
    distanceM: 4100,
    status: 'NEEDS_CHECK',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBo80ZbNktYprNLaFVTTvLaE46oSuqbYBNvxIgLRYnTHf_1m97t-5akvAbpyMbOXYWZFhUPgrtddE1F8uNQojeiO2gWm_ixM25oxd7RKt7EvHUVr9fGxP-_9_3UEsu08lrcNHZdlQsCli7dUevXOed3Tpb_JZUicP5dfHlF5VLXJAPLobA_pxcEIM8y0V7CpSsb2DlWdFsCTOUqPdJLxiDHMwYPQPCgbmZEUdmt-30',
    badges: [{ icon: 'deck', label: '테라스 동반', tone: 'apricot' }],
    summary: ['야외 테라스에서만 동반할 수 있어요.', '반려견 전용 화식 메뉴가 있어요.', '실내 동반 여부는 리뷰마다 달라요.'],
    evidenceCount: 5,
    pin: { top: '28%', left: '64%', icon: 'restaurant' },
  },
  {
    id: 4,
    name: '학의천 산책길 (예시)',
    category: 'trail',
    area: '평촌동',
    distanceM: 3000,
    status: 'OK',
    badges: [],
    summary: ['하천 옆 평지라 소형견 산책에 좋아요.', '그늘이 적어 여름 낮에는 더워요.', '자전거 도로와 붙어 있어 리드줄이 필요해요.'],
    evidenceCount: 6,
  },
  {
    id: 5,
    name: '범계 브런치하우스 (예시)',
    category: 'restaurant',
    area: '호계동',
    distanceM: 1800,
    status: 'HARD',
    badges: [],
    summary: ['실내 동반이 안 된다는 리뷰가 많아요.', '이동장 이용 시에도 거절된 사례가 있어요.', '방문 전 전화 확인을 권해요.'],
    evidenceCount: 4,
  },
]

// 검색 자동완성 예시
export const SEARCH_SUGGESTIONS = [
  { name: '평촌 반려견 동반 카페', category: '카페', distanceM: 800 },
  { name: '범계 야외 브런치', category: '식당', distanceM: 2100 },
  { name: '안양천 반려견 놀이터', category: '공원', distanceM: 3400 },
]

// 지도 SDK(네이버 지도) 붙이기 전까지 쓰는 배경 이미지
export const MAP_PLACEHOLDER_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuDSbu8DAZ3vZHn5DD3kaNcg0KYELZjhPMnxCdbVU_rdGn-FNwvOus6Y18bn_vxuGhxEkbFDJyU0Eo2M8KLrj1jGpSESqjcYQNCJdpZMOAIq38z7P-wU_REEU-ImPM2_f9Fk5RIKfMzly1D480fbWnLHCljJQZq_XZb4DN0Pyy-MJJ_DsqhQOtJmGP7i2fwthr6onSypEzb74hQm6iiZXVyMvkFk4-xA6790_esLNME'
