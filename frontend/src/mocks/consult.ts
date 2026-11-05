// 화면 확인용 예시 데이터입니다. 상담 API(POST /api/consults, GET /api/consults)를 붙이면 지웁니다.

export const CONSULT_HISTORY = [
  { id: 4, date: '오늘 오후 02:40', text: '산책 후 뒷다리를 살짝 터는 듯한...', isNew: true },
  { id: 3, date: '10월 12일', text: '사료를 평소보다 천천히 먹고 침을...' },
  { id: 2, date: '10월 5일', text: '오른쪽 귀를 긁으며 머리를 자주 흔듦' },
  { id: 1, date: '9월 24일', text: '환절기 풀숲 산책 후 재채기 반복' },
]

export const SYMPTOM_CHIPS = ['구토', '설사', '기침', '절뚝임']

// 과거 상담·질문 메모 검색 결과
export const PAST_MATCHES = [
  { id: 2, date: '2026. 10. 02', level: '주의 (24시간 관찰)', strong: true, text: '산책 후 뒷다리 살짝 저는 증상' },
  { id: 1, date: '2026. 09. 14', level: '경미', strong: false, text: '소파에서 뛰어내린 후 오른쪽 발 듬' },
]

// 무료 LLM 3개 교차검증 결과 (결정사항: Groq · Gemini · OpenRouter)
export const CONSULT_RESULT = {
  urgency: '주의 · 24~48시간 관찰',
  agree: 3,
  total: 3,
  headline: '발바닥 이물질 또는 가벼운 인대 긴장 가능성',
  summary: [
    '급성 파행(절뚝거림)이나 지속적인 통증 호소가 없다면 즉시 응급 상태는 아닐 가능성이 높습니다.',
    '발바닥 패드 사이 작은 모래나 가시가 끼었는지 먼저 꼼꼼히 확인하고 미지근한 물로 씻겨주세요.',
    '슬개골 주의 메모를 고려해 24시간 동안 소파 점프를 제한하고 걸음걸이를 지켜봐 주세요.',
  ],
  answers: [
    { model: 'Groq', color: 'text-primary', text: '경미한 지지 파행 징후. 슬개골 메모 기준, 발바닥 확인 후 48시간 내 지속 시 정형외과 촉진 권고.' },
    { model: 'Gemini', color: 'text-secondary', text: '열감, 붓기, 발바닥 패드 균열 확인 우선. 식욕 및 보행 지속 가능 시 안정 유지 상태로 모니터링 합당.' },
    { model: 'OpenRouter', color: 'text-tertiary', text: '산책로 잔디밭 가시 박힘 사례 빈발. 미온수 세척과 미끄럼 방지 매트 환경 조성 제안.' },
  ],
}
