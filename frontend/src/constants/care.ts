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

// 산책 팁. shortcut: 날씨 API가 없어 고정 문구, 날씨 연동하면 기온·바닥 온도로 바꿀 것
export const WALK_TIP = {
  note: '날씨 연동 전 기본 팁',
  title: '산책 전후로 물을 충분히 챙겨 주세요',
  body: '발바닥과 관절을 위해 한낮의 뜨거운 아스팔트와 무리한 계단은 피해 주세요.',
  checklist: ['물병 챙기기', '하네스 착용'],
}
