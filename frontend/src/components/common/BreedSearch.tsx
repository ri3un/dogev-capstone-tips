import { useEffect, useRef, useState } from 'react'
import { BREEDS, matchBreeds } from '../../constants/breeds'
import { input } from './styles'

// 견종 입력: 입력하면 아래에 후보가 뜨고, 목록에 없으면 입력한 글자 그대로 저장
export default function BreedSearch({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const boxRef = useRef<HTMLDivElement>(null)
  const list = matchBreeds(value)

  // 밖을 누르면 닫힘
  useEffect(() => {
    const onDown = (e: MouseEvent) => !boxRef.current?.contains(e.target as Node) && setOpen(false)
    document.addEventListener('mousedown', onDown)
    return () => document.removeEventListener('mousedown', onDown)
  }, [])

  const pick = (b: string) => {
    onChange(b)
    setOpen(false)
    setActive(-1)
  }

  return (
    <div ref={boxRef} className="relative">
      <input
        value={value}
        onChange={(e) => { onChange(e.target.value); setOpen(true); setActive(-1) }}
        onFocus={() => setOpen(true)}
        onKeyDown={(e) => {
          if (!open || list.length === 0) return
          if (e.key === 'ArrowDown') { e.preventDefault(); setActive((i) => (i + 1) % list.length) }
          if (e.key === 'ArrowUp') { e.preventDefault(); setActive((i) => (i <= 0 ? list.length - 1 : i - 1)) }
          if (e.key === 'Enter' && active >= 0) { e.preventDefault(); pick(list[active]) }
          if (e.key === 'Escape') setOpen(false)
        }}
        role="combobox"
        aria-expanded={open}
        aria-controls="breed-list"
        autoComplete="off"
        placeholder="검색하거나 직접 입력 (예: 말티, ㅁㅌㅈ)"
        className={input}
      />
      {open && (
        <ul id="breed-list" role="listbox" className="absolute z-20 mt-1 max-h-64 w-full overflow-auto rounded-xl border border-beige bg-surface-container-lowest py-1 shadow-lg">
          {list.map((b, i) => (
            <li key={b} role="option" aria-selected={i === active}>
              <button type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => pick(b)} className={`w-full px-4 py-2.5 text-left hover:bg-surface-container-low ${i === active ? 'bg-surface-container-low font-semibold' : ''}`}>
                {b}
              </button>
            </li>
          ))}
          {value.trim() && !BREEDS.includes(value.trim()) && (
            <li className="px-4 py-2.5 text-label-md text-outline">목록에 없으면 "{value.trim()}"(으)로 저장돼요</li>
          )}
        </ul>
      )}
    </div>
  )
}
