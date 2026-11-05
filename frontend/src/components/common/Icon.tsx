// Material Symbols 아이콘. name은 https://fonts.google.com/icons 의 이름입니다.
export default function Icon({ name, className = '' }: { name: string; className?: string }) {
  return (
    <span aria-hidden className={`material-symbols-outlined ${className}`}>
      {name}
    </span>
  )
}
