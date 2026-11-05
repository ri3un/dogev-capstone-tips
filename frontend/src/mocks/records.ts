// 화면 확인용 예시 데이터입니다. 기록 API(GET /api/dogs/{id}/records?month=)를 붙이면 지웁니다.

export type RecordKind = 'meal' | 'poop' | 'activity' | 'symptom' | 'weight'

export const KIND_LABEL: Record<RecordKind, string> = { meal: '식사', poop: '배변', activity: '활동', symptom: '증상', weight: '체중' }
// 달력 점 색
export const KIND_DOT: Record<RecordKind, string> = { meal: 'bg-primary-container', poop: 'bg-secondary', activity: 'bg-tertiary', symptom: 'bg-error', weight: 'bg-outline' }
// 일별 상세 배지 색
export const KIND_BADGE: Record<RecordKind, string> = {
  meal: 'bg-primary-container/20 text-on-primary-container',
  poop: 'bg-secondary-container text-on-secondary-container',
  activity: 'bg-tertiary-fixed text-on-tertiary-fixed-variant',
  symptom: 'bg-error-container text-on-error-container',
  weight: 'bg-surface-container text-on-surface-variant',
}

export interface HealthRecord {
  id: number
  date: string // YYYY-MM-DD
  kind: RecordKind
  time: string
  author: string // 기록한 가족
  title: string
  memo: string
}

export const WEIGHT = {
  current: 3.2,
  diff: '+0.1kg',
  note: '목표 체중 3.2kg 유지 중 · 소형견 표준 범위 내',
  labels: ['6월 (3.0kg)', '8월 (3.1kg)', '10월 (3.2kg)'],
}

export const MONTH_STAT = { rate: 94, days: 28 }

export const RECORDS: HealthRecord[] = [
  { id: 1, date: '2026-10-18', kind: 'meal', time: '오전 10:15', author: '엄마', title: '아침 사료 45g 완밥', memo: '유산균 분말 함께 급여, 남김없이 맛있게 먹음.' },
  { id: 2, date: '2026-10-18', kind: 'activity', time: '오후 14:30', author: '아빠', title: '학의천 산책 35분 (1.2km)', memo: '날씨 맑음, 다른 강아지 친구들과 인사 후 가볍게 달리기.' },
  { id: 3, date: '2026-10-18', kind: 'poop', time: '오후 14:45', author: '아빠', title: '정상 변 1회 (상태 양호)', memo: '산책 중 잔디밭에서 배변. 단단함과 색상 정상 범위.' },
  { id: 4, date: '2026-10-18', kind: 'symptom', time: '오후 19:20', author: '엄마', title: '뒷다리 핥음 없음, 활발함', memo: '최근 주의 관찰 항목이었던 왼쪽 뒷발 핥는 증상 미발생.' },
]

// 달력 점용 예시: 날짜별 기록 종류 (API에서는 월별 요약으로 받기)
export function dayKinds(date: string): RecordKind[] {
  const fromList = [...new Set(RECORDS.filter((r) => r.date === date).map((r) => r.kind))]
  if (fromList.length) return fromList
  const d = Number(date.slice(8))
  if (!date.startsWith('2026-10')) return []
  const extra: RecordKind[][] = [['activity'], ['poop'], ['poop', 'activity']]
  return ['meal', ...extra[d % 3], ...(d === 14 ? (['symptom'] as RecordKind[]) : []), ...(d === 5 || d === 31 ? (['weight'] as RecordKind[]) : [])]
}

// 기록 입력(글 → 확인 카드) 예시. POST /api/dogs/{dogId}/records/parse 응답 모양
export const EXAMPLE_SENTENCES = [
  '아침 사료 반 정도 먹고 오후에 무른 변 한 번, 저녁에 30분 산책',
  '오늘 체중 3.2kg, 심장사상충 약 먹였어',
  '산책 후 뒷다리를 터는 행동을 했어. 병원에 물어보고 싶어',
]

export type ParsedKind = 'meal' | 'poop' | 'activity' | 'symptom' | 'weight' | 'medicine' | 'question'

export const PARSED_LABEL: Record<ParsedKind, { label: string; icon: string }> = {
  meal: { label: '식사', icon: 'restaurant' },
  poop: { label: '배변', icon: 'eco' },
  activity: { label: '활동', icon: 'directions_walk' },
  symptom: { label: '증상', icon: 'monitor_heart' },
  weight: { label: '체중', icon: 'monitor_weight' },
  medicine: { label: '투약', icon: 'medication' },
  question: { label: '질문 메모', icon: 'help' },
}

// 카테고리별 선택지 (서버 enum과 맞춤)
export const PARSED_OPTIONS: Partial<Record<ParsedKind, { field: string; options: { value: string; label: string }[] }>> = {
  meal: { field: '양', options: [{ value: 'MORE', label: '많이' }, { value: 'NORMAL', label: '보통' }, { value: 'LESS', label: '적게' }, { value: 'NONE', label: '안 먹음' }] },
  poop: { field: '상태', options: [{ value: 'NORMAL', label: '정상' }, { value: 'SOFT', label: '무름' }, { value: 'DIARRHEA', label: '설사' }, { value: 'BLOODY', label: '혈변' }, { value: 'HARD', label: '딱딱함' }] },
}

export interface ParsedItem {
  id: number
  kind: ParsedKind
  value: string // 선택지 값 또는 자유 입력
  memo: string
  needsCheck?: boolean // 서버 검증에서 뺀 항목
}

export const PARSE_RESULT: { warning?: string; items: ParsedItem[] } = {
  items: [
    { id: 1, kind: 'meal', value: 'LESS', memo: '아침 사료 절반' },
    { id: 2, kind: 'poop', value: 'SOFT', memo: '오후 1회' },
    { id: 3, kind: 'activity', value: '30분', memo: '저녁 산책 30분', needsCheck: true },
  ],
}
