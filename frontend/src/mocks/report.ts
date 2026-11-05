// 화면 확인용 예시 데이터입니다. GET /api/dogs/{dogId}/reports/preview?from&to를 붙이면 지웁니다.
// PDF 섹션 9개와 같은 순서입니다.

export interface ReportSection {
  key: string
  title: string
  icon: string
  alert?: string // 이상 신호(규칙 결과)
  rows: { label: string; value: string }[]
  empty?: boolean
}

export const REPORT_SUMMARY = [
  '소화 관련 상담이 2번 있었고, 21일 이후로는 배변이 안정적이에요.',
  '오른쪽 뒷다리 절뚝임이 1번 기록되었고 이후 재발 기록은 없어요.',
  '체중은 3.2kg으로 지난달보다 0.1kg 늘었고 표준 범위 안이에요.',
]

export const REPORT_SECTIONS: ReportSection[] = [
  { key: 'basic', title: '기본 정보', icon: 'pets', rows: [{ label: '이름', value: '꿍 (말티즈, 수컷, 중성화)' }, { label: '나이·체중', value: '3살 · 3.2kg (소형견)' }, { label: '건강 메모', value: '슬개골 주의' }] },
  { key: 'meal', title: '식사·간식', icon: 'restaurant', rows: [{ label: '사료', value: '하루 평균 82g (권장 85g의 96%)' }, { label: '간식', value: '주 4회, 닭가슴살 위주' }] },
  { key: 'poop', title: '배변', icon: 'eco', alert: '무른 변 2일 연속 (10/19~10/20)', rows: [{ label: '정상', value: '24회' }, { label: '무른 변·설사', value: '3회' }] },
  { key: 'symptom', title: '증상·이상', icon: 'monitor_heart', alert: '오른쪽 뒷다리 절뚝임 1회 (10/14)', rows: [{ label: '구토', value: '없음' }, { label: '절뚝임', value: '1회, 이후 없음' }] },
  { key: 'activity', title: '활동·산책', icon: 'directions_walk', rows: [{ label: '산책', value: '22회 · 평균 32분' }, { label: '다녀온 장소', value: '학의천 산책길, 평촌 멍카페 (예시)' }] },
  { key: 'weight', title: '체중', icon: 'monitor_weight', rows: [{ label: '10/05', value: '3.1kg' }, { label: '10/31', value: '3.2kg' }] },
  { key: 'medical', title: '투약·접종·병원', icon: 'vaccines', empty: true, rows: [] },
  { key: 'questions', title: '수의사에게 물어볼 것', icon: 'help', rows: [{ label: '1', value: '산책 후 뒷다리를 터는 행동이 슬개골과 관련 있을까요?' }, { label: '2', value: '사료 양을 줄여야 할까요?' }] },
]
