// 상담·영양·코스 화면의 고정 문구

// AI 상담 추천 증상 칩
export const SYMPTOM_CHIPS = ['구토', '설사', '기침', '절뚝임']

// 영양 가이드 하단 출처
export const NUTRITION_SOURCES = [
  { label: 'ASPCA 반려동물 유해 음식', url: 'https://www.aspca.org/pet-care/animal-poison-control/people-foods-avoid-feeding-your-pets' },
  { label: 'WSAVA 영양 가이드라인', url: 'https://wsava.org/global-guidelines/global-nutrition-guidelines/' },
]

// 코스 화면 테마 칩 (누르면 /course/plan?theme= 으로 들를 곳 순서를 채움)
export type CourseTheme = 'light' | 'forest' | 'cafe'
export const THEME_LABEL: Record<CourseTheme, string> = {
  light: '가벼운 동네 30분 산책',
  forest: '주말 힐링 숲길 코스 (1~2시간)',
  cafe: '카페 투어 코스',
}
