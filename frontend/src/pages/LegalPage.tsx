import { card } from '../components/common/styles'

// 이용약관·개인정보처리방침 초안 (시연용, 정식 문구는 나중에 교체)
const DOCS = {
  terms: {
    title: '이용약관',
    sections: [
      ['목적', '이 약관은 킁킁(이하 "서비스")의 이용 조건과 절차를 정합니다.'],
      ['서비스 내용', '반려견 건강 기록, AI 상담, 반려견 동반 장소 안내와 산책 코스 추천을 제공합니다.'],
      ['AI 정보의 한계', 'AI 상담과 요약은 참고 정보이며 수의사의 진단을 대신하지 않습니다.'],
      ['장소 정보', '장소 정보는 공개된 블로그 글을 요약한 것으로 실제와 다를 수 있습니다. 방문 전 매장에 확인해 주세요.'],
      ['이용자의 의무', '다른 사람의 정보를 도용하거나 사실과 다른 수정 요청을 반복해서 보내면 이용이 제한될 수 있습니다.'],
    ],
  },
  privacy: {
    title: '개인정보처리방침',
    sections: [
      ['수집 항목', '소셜 로그인 식별값, 닉네임, 반려견 정보(이름·견종·체중 등), 건강 기록, 상담 내용'],
      ['이용 목적', '서비스 제공, 맞춤 영양·코스 추천, 건강 리포트 생성'],
      ['보관 기간', '회원 탈퇴 시 바로 삭제합니다. 단, 관련 법령에서 정한 기간이 있으면 그 기간 동안 보관합니다.'],
      ['제3자 제공', 'AI 상담 시 질문 내용이 AI 서비스(Groq·Gemini·OpenRouter)로 전송됩니다. 이름 등 개인을 알아볼 수 있는 정보는 보내지 않습니다.'],
      ['위치 정보', '코스 추천 시 출발 위치는 거리 계산에만 쓰고 저장하지 않습니다.'],
    ],
  },
}

export default function LegalPage({ kind }: { kind: keyof typeof DOCS }) {
  const doc = DOCS[kind]
  return (
    <div className="mx-auto flex w-full max-w-[760px] flex-col gap-6 px-5 py-9 lg:px-6">
      <h1 className="text-headline-lg font-bold tracking-tight">{doc.title}</h1>
      <article className={`${card} flex flex-col gap-5 p-7`}>
        {doc.sections.map(([h, body], i) => (
          <section key={h}>
            <h2 className="mb-1 text-body-lg font-bold">
              제{i + 1}조 ({h})
            </h2>
            <p className="leading-relaxed text-on-surface-variant">{body}</p>
          </section>
        ))}
        <p className="border-t border-outline-variant/30 pt-4 text-label-sm text-outline">시행일: 2026년 12월 17일 (초안)</p>
      </article>
    </div>
  )
}
