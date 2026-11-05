// 화면 확인용 예시 데이터입니다. GET /api/dogs/{dogId}/nutrition-card를 붙이면 지웁니다.

// 하루 필요 열량: RER = 70 × 체중^0.75, 중성화 성견은 RER × 1.4~1.8 (서버에서 같은 식으로 계산)
export const rer = (kg: number) => 70 * kg ** 0.75

export const NUTRITION = {
  weightKg: 3.2,
  factor: [1.4, 1.8] as const,
  foodKcalPerKg: 3300 as number | null, // 사료 칼로리, 없으면 g 대신 안내 문구
  waterMlPerKg: [50, 60] as const,
}

export const AVOID_FOODS = [
  { name: '포도·건포도', level: 'danger', why: '적은 양으로도 급성 신부전' },
  { name: '초콜릿·커피', level: 'danger', why: '테오브로민·카페인 중독' },
  { name: '양파·마늘·파', level: 'danger', why: '적혈구 손상(빈혈)' },
  { name: '자일리톨', level: 'danger', why: '저혈당, 간 손상' },
  { name: '익힌 뼈', level: 'warn', why: '부서져 장을 찌를 수 있음' },
  { name: '우유', level: 'warn', why: '유당 때문에 설사할 수 있음' },
  { name: '밀(글루텐)', level: 'allergy', why: '꿍의 알레르기 성분' },
] as const

export const SAFE_FOODS = [
  { name: '삶은 소고기', how: '기름기 빼고 잘게' },
  { name: '삶은 계란', how: '노른자 포함, 주 2~3회' },
  { name: '고구마', how: '삶아서 소량' },
  { name: '단호박', how: '익혀서 씨 제거' },
  { name: '당근', how: '익히거나 얇게' },
  { name: '브로콜리', how: '데쳐서 소량' },
  { name: '사과', how: '씨·심 제거' },
  { name: '블루베리', how: '하루 2~3알' },
  { name: '오이', how: '껍질째 얇게' },
  { name: '무가당 플레인 요거트', how: '한 숟가락' },
]

export const DISEASE_TIPS = [
  { name: '슬개골(관절)', lines: ['체중이 늘면 무릎에 부담이 커져요. 지금 체중을 유지해 주세요.', '미끄러운 바닥과 높은 곳에서 뛰어내리기를 줄여 주세요.'] },
]

export const SOURCES = [
  { label: 'ASPCA 반려동물 유해 음식', url: 'https://www.aspca.org/pet-care/animal-poison-control/people-foods-avoid-feeding-your-pets' },
  { label: 'WSAVA 영양 가이드라인', url: 'https://wsava.org/global-guidelines/global-nutrition-guidelines/' },
]
