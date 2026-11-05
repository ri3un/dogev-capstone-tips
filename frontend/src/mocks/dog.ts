// 화면 확인용 예시 데이터입니다. GET /api/dogs/{id}, 영양 카드 API를 붙이면 지웁니다.
export const DOG = {
  name: '꿍',
  breed: '말티즈',
  size: '소형견',
  age: 3,
  weightKg: 3.2,
  photoUrl:
    'https://lh3.googleusercontent.com/aida-public/AB6AXuBn1a6AXPvCtOLvPSIhh0U7Bkcs8Cwm6HETxXLLYS7Um62ZEpixqc6JDrVad2VoQW4hXsfzIFS3EhFYL3dxBTeom4KETF5Ki4wMmQjXpyX-1DugvtSdGGiu4pgIn7mcHRfd_uEeWSzB6cOD8YFpkI6hMEY5v951jC2ca0TrLGeevEFsW-NRmEVMmzqb9OQK5mP5aM7lKAQFJhpUbPz90BN0nPkFyXcN9RW8pkIx9DTEzT2Z5rBjvPZq',
  stats: [
    { icon: 'restaurant', label: '오늘 사료 권장량', value: '85g' },
    { icon: 'water_drop', label: '물 권장량', value: '160ml' },
    { icon: 'directions_walk', label: '최근 기록', value: '어제 산책 30분' },
  ],
  family: ['나', '엄마'],
  tip: { before: '최근 관절 메모가 있어 오늘은 ', strong: '2km 이내', after: ' 산책을 추천해요 🐾' },
}

export type Trend = 'good' | 'watch' | 'keep'

export const MONTHLY_SUMMARY = {
  consultCount: 4,
  recordCount: 18,
  text: '이번 달은 소화 관련 상담이 2번 있었어요. 21일 이후로는 배변이 안정적이에요. 오른쪽 뒷다리 절뚝임은 한 번 있었고 이후 기록은 없어요.',
  items: [
    { label: '소화', status: '좋아짐', trend: 'good' as Trend },
    { label: '관절', status: '지켜보기', trend: 'watch' as Trend },
    { label: '체중', status: '유지', trend: 'keep' as Trend },
  ],
}
