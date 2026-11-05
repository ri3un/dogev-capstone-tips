// 화면 확인용 예시 데이터입니다. GET /api/dogs/{dogId}/vet-questions를 붙이면 지웁니다.
export type QuestionSource = 'MEMO' | 'CONSULT'

export interface VetQuestion {
  id: number
  text: string
  author: string
  date: string
  source: QuestionSource
}

export const VET_QUESTIONS: VetQuestion[] = [
  { id: 3, text: '산책 후 뒷다리를 터는 행동이 슬개골과 관련 있을까요?', author: '엄마', date: '2026-11-05', source: 'CONSULT' },
  { id: 2, text: '사료 양을 줄여야 할까요?', author: '아빠', date: '2026-10-28', source: 'MEMO' },
  { id: 1, text: '다음 예방접종은 언제 맞으면 되나요?', author: '엄마', date: '2026-10-15', source: 'MEMO' },
]
