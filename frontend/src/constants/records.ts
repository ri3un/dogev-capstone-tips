// 건강 기록 화면 표시용 상수 (달력 점·배지, 기록 입력 확인 카드)

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

// 기록 입력 예시 문장 (누르면 입력칸에 채워짐)
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
