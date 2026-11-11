import { PawIcon } from './Paw'

// 불러오는 동안: 발바닥이 하나씩 찍힘
// compact: 카드 안에 넣을 때 높이를 줄임
export default function PawLoading({ label = '킁킁, 냄새 맡는 중…', compact = false }: { label?: string; compact?: boolean }) {
  return (
    <div role="status" className={`flex ${compact ? 'py-10' : 'min-h-[50vh]'} flex-col items-center justify-center gap-3 text-body-sm text-outline`}>
      <div className="flex gap-3 text-primary-container">
        {[0, 0.3, 0.6, 0.9].map((delay) => (
          <span key={delay} className="kk-pawstep" style={{ animationDelay: `${delay}s` }}>
            <PawIcon className="h-6 w-6" />
          </span>
        ))}
      </div>
      {label}
    </div>
  )
}
