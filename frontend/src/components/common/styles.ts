// 새 화면에서 같이 쓰는 클래스 묶음 (Stitch 화면의 카드·버튼 모양과 같음)
export const card = 'rounded-2xl bg-surface-container-lowest shadow-[0_4px_20px_-2px_rgba(74,52,38,0.05)]'
export const primaryBtn = 'inline-flex h-[52px] items-center justify-center gap-2 rounded-full bg-primary-container px-7 text-label-lg font-bold text-on-primary shadow-sm transition-all hover:bg-primary active:scale-[0.98] disabled:opacity-50'
export const ghostBtn = 'inline-flex h-[52px] items-center justify-center gap-2 rounded-full bg-surface-container-low px-6 text-label-lg font-semibold text-on-surface-variant transition-colors hover:bg-surface-container'
export const input = 'h-12 w-full rounded-xl border border-beige bg-surface-container-lowest px-4 transition-all placeholder:text-outline focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 focus:outline-none'
export const chip = (on: boolean) =>
  `rounded-full px-4 py-2 text-label-md transition-all ${on ? 'bg-primary-container font-bold text-white' : 'border border-beige bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container'}`
