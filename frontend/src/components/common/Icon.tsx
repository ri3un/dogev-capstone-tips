const FILLED = { fontVariationSettings: "'FILL' 1" }

// Material Symbols 아이콘. name은 https://fonts.google.com/icons 의 이름입니다. filled면 속이 찬 모양
export default function Icon({ name, className = '', filled }: { name: string; className?: string; filled?: boolean }) {
  return (
    <span aria-hidden className={`material-symbols-outlined ${className}`} style={filled ? FILLED : undefined}>
      {name}
    </span>
  )
}
