// 견종 검색 목록 (국내 등록이 많은 견종 위주, 목록에 없으면 직접 입력)
export const BREEDS = [
  '말티즈', '푸들', '토이 푸들', '미니어처 푸들', '스탠다드 푸들', '포메라니안', '시츄', '비숑 프리제', '치와와', '요크셔 테리어',
  '닥스훈트', '미니어처 닥스훈트', '웰시 코기', '진돗개', '시바견', '골든 리트리버', '래브라도 리트리버', '보더 콜리', '비글', '프렌치 불독',
  '잉글리시 불독', '퍼그', '페키니즈', '파피용', '미니어처 핀셔', '미니어처 슈나우저', '슈나우저', '코커 스패니얼', '카발리에 킹 찰스 스패니얼', '셔틀랜드 쉽독',
  '사모예드', '시베리안 허스키', '알래스칸 말라뮤트', '저먼 셰퍼드', '도베르만', '로트와일러', '그레이트 피레니즈', '버니즈 마운틴 독', '베들링턴 테리어', '잭 러셀 테리어',
  '웨스트 하이랜드 화이트 테리어', '스피츠', '재패니즈 스피츠', '보스턴 테리어', '이탈리안 그레이하운드', '휘핏', '샤페이', '차우차우', '삽살개', '풍산개',
  '말티푸', '폼피츠', '믹스',
]

const CHO = 'ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ'
// 한글 음절을 초성으로 바꿈 (예: 말티즈 → ㅁㅌㅈ)
const toChosung = (s: string) =>
  [...s].map((c) => {
    const code = c.charCodeAt(0) - 0xac00
    return code >= 0 && code < 11172 ? CHO[Math.floor(code / 588)] : c
  }).join('')
const norm = (s: string) => s.replace(/\s+/g, '').toLowerCase()

// 띄어쓰기 무시 부분 일치 + 초성 검색
export function matchBreeds(query: string, limit = 8): string[] {
  const q = norm(query)
  if (!q) return BREEDS.slice(0, limit)
  const onlyCho = [...q].every((c) => CHO.includes(c))
  return BREEDS.filter((b) => (onlyCho ? toChosung(norm(b)).includes(q) : norm(b).includes(q))).slice(0, limit)
}
