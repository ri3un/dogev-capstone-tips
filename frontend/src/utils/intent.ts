// 통합 입력창 질문/기록 판별 (LLM 없음). 백엔드 IntentClassifier 와 같은 규칙, 예문 30개 테스트는 백엔드에 있음
const TRAILING = /[.!~ㅠㅜㅋㅎ\s]+$/
const QUESTION_ENDING = /(\?|까요|나요|까|[인은는]가요|[인은는]가|야 하나|어떡해|어떡하지|어떻게 해(야 해)?|어쩌죠|도 돼요|도 되나|도 돼)$/

export const isQuestion = (text: string) => QUESTION_ENDING.test(text.trim().replace(TRAILING, ''))
