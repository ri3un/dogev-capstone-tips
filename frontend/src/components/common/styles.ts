// 화면에서 같이 쓰는 클래스 묶음 (각진 모서리 + 얇은 테두리, 그림자 없음)
export const card = 'rounded-md border border-outline-variant bg-surface-container-lowest'
export const primaryBtn = 'inline-flex h-[52px] items-center justify-center gap-2 rounded-md bg-primary-container px-7 text-label-lg font-bold text-on-primary transition-all hover:bg-primary active:scale-[0.98] disabled:opacity-50'
export const ghostBtn = 'inline-flex h-[52px] items-center justify-center gap-2 rounded-md border border-outline-variant bg-surface-container-lowest px-6 text-label-lg font-semibold text-on-surface-variant transition-colors hover:bg-surface-container-low'
export const input = 'h-12 w-full rounded-md border border-beige bg-surface-container-lowest px-4 transition-all placeholder:text-outline focus:border-primary-container focus:ring-2 focus:ring-primary-container/30 focus:outline-none'
export const chip = (on: boolean) =>
  `rounded-md px-4 py-2 text-label-md transition-all ${on ? 'bg-primary-container font-bold text-white' : 'border border-beige bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container'}`
