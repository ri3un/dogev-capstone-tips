// 화면 확인용 예시 데이터입니다. 코스 추천 API(POST /api/courses/recommend)를 붙이면 지웁니다.
// TODO: 장소·코스 이름을 시연 지역 안양 데이터로 바꿉니다.

export type CourseTheme = 'light' | 'forest' | 'cafe'
export type Tone = 'green' | 'apricot' | 'neutral'

export interface CourseCard {
  id: number
  theme: CourseTheme
  name: string
  area: string
  feature: string
  distanceKm: number
  minutes: number
  difficulty: string
  imageUrl: string
  imageTag: { icon: string; label: string }
  route: string[] // 출발 → 경유 → 도착
  tags: { label: string; tone: Tone }[]
}

export const THEME_LABEL: Record<CourseTheme, string> = {
  light: '가벼운 동네 30분 산책',
  forest: '주말 힐링 숲길 코스 (1~2시간)',
  cafe: '카페 투어 코스',
}

export const COURSE_CARDS: CourseCard[] = [
  {
    id: 1,
    theme: 'light',
    name: '학의천 평지 힐링길 (예시)',
    area: '평촌 학의천 구역',
    feature: '경사도 0.5% (완전 평지)',
    distanceKm: 1.8,
    minutes: 35,
    difficulty: '아주 쉬움 평지',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBWbDo2Qu3JSd2deaHjFv07bQ88tgWPVjZ8zhvLJfMPNF9cP5o3yMdQzjqNeb0jRQfAWY6M_zhraHTjXn1eOu6lR1-gvh5WoA3bXdZ7bWRmsncevdltzHIdyB1h8BMu5k630QYCVujQIix0o6Gy4mM7dR0-Eh_-ob9miR0eq4WQLcSOIi9R9mDeyqYQAdwUVNuSHG16qupqIF_Dr3lOjlEhRySM8qm5kh7MBigy6Sg',
    imageTag: { icon: 'nature', label: '자연 친화 완만길' },
    route: ['잔디마당', '징검다리', '테라스 카페'],
    tags: [
      { label: '흙길 80%', tone: 'green' },
      { label: '관절 부담 적음', tone: 'apricot' },
      { label: '배변봉투 수거함 완비', tone: 'neutral' },
    ],
  },
  {
    id: 2,
    theme: 'light',
    name: '안양천 달빛 둔치 코스 (예시)',
    area: '안양천 구역',
    feature: '자전거길 분리형',
    distanceKm: 2.4,
    minutes: 45,
    difficulty: '쉬움',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCcRM0V0l6fYXoMMXh4Gbcbq0XaZbEj8Rr_SqCZiJgB1a6CE1k6OdGAySCfkqBQl8fblc9VUcanhEXIjqjjSqn6eYSqPUHb19-8ARfraFFSEucNRnTK4T47ohZrTCPJVn8uDGvYb5rzpOqQNATZfXR34n01xG99_vqqXCW74TX_hWhg2e-lvmO0tID_XfyGCftceHuRzvHdRC9JlO0tOG7ejISkDYn7uOWrBQ0c4bQ',
    imageTag: { icon: 'wb_twilight', label: '노을 & 야간 산책' },
    route: ['잔디밭', '강변 산책로', '쉼터 벤치'],
    tags: [
      { label: '탁 트인 시야', tone: 'neutral' },
      { label: '시원한 강바람', tone: 'apricot' },
      { label: '야간 조명 밝음', tone: 'green' },
    ],
  },
  {
    id: 3,
    theme: 'light',
    name: '수리산 단풍길 & 피크닉 루트 (예시)',
    area: '수리산 구역',
    feature: '반려견 친화 카페거리',
    distanceKm: 3.0,
    minutes: 60,
    difficulty: '보통',
    imageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBFNGSsN2e8qVV-lciAEzxERZ6L7O2CqBLmAOPCcWechknRvO29VyWWOTQZG5a7HrklZsB_iFOw3eTEpo1ufCvg0xD_QMG30cvaApY1jBuK77TRvamDXpUkTQkqItXAZZo9q62abE4rw1kjj2spzmlkfX1fzsUtQ84zmrC69RyVUKKjsr9GWyHyc0ir1LBQqhYDf6gT25Sjxu5OFkI_-FhAkYkvMJOBScj5SLkIU_c',
    imageTag: { icon: 'local_cafe', label: '브런치 & 피크닉' },
    route: ['산책로 입구', '징검다리 쉼터', '브런치 카페'],
    tags: [
      { label: '물소리 힐링', tone: 'green' },
      { label: '반려견 마실물 음수대', tone: 'apricot' },
      { label: '그늘 쉼터 풍부', tone: 'neutral' },
    ],
  },
]

export const WALK_TIP = {
  floorTemp: 20.4,
  title: '오늘 낮 기온 18℃, 아스팔트 온도가 적당해요. 충분한 수분 섭취를 잊지 마세요!',
  body: '슬개골 보호를 위해 무리한 계단 오르기보다는 평지 흙길 산책을 추천합니다.',
  checklist: ['물병 챙기기', '하네스 착용'],
}

// 2단계 플로우 결과(경로 상세)
export type StopKind = 'start' | 'via' | 'end'
export interface CourseStop {
  kind: StopKind
  name: string
  note?: string // 오른쪽 강조 문구
  description?: string
  move: string // 다음 지점까지 이동 안내
  pin: { left: string; top: string } // 지도 SDK 전 임시 위치(%)
}

export const COURSE_PLAN = {
  distanceKm: 1.8,
  minutes: 30,
  surface: '평지 흙길 85% · 완경사 코스',
  mapImageUrl: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCuaMjOOsHnDUSJUVnq_Sh1yoLkd2V9pvWtJOUzXjRbHLTqYXQZR0jN_R-uepgeq5RAlQBQt_x8jad11KzuEpjjGQpDrJ5LOQ6sWuRwGloPGNsWmmtk1Wv50v0OvPMPBdgFiY5PfJnDi-p7LbEIq8WjpLLpjHdeatFDaMBpVMMEK82rEnST8SdSes9qhejU27ui6xsYqc28Gmk9trGSSRCQ1MZoZcq_pGjz2UvZ10g',
  routePath: 'M 180 260 Q 320 220 400 150 T 630 110', // SVG 경로(viewBox 800x360)
  stops: [
    { kind: 'start', name: '범계역 3번 출구 인근', note: '0m · 시작 지점', move: '도보 12분 · 700m (평지 흙길 위주)', pin: { left: '15%', top: '80%' } },
    {
      kind: 'via',
      name: '학의천 산책길',
      note: '소형견 친화 완만길',
      description: '바닥이 푹신한 야자매트 구간 및 반려견 전용 음수대 위치',
      move: '도보 10분 · 600m',
      pin: { left: '50%', top: '40%' },
    },
    {
      kind: 'end',
      name: '평촌 멍카페',
      note: '야외 테라스 · 멍푸치노 제공',
      description: '도그 파킹 고리와 미끄럼 방지 매트가 완비된 펫 프렌들리 테라스',
      move: '도보 8분 · 500m 귀가 코스 연계 가능',
      pin: { left: '80%', top: '25%' },
    },
  ] as CourseStop[],
}
