import { useState } from 'react'

type Props = {
  value: string[]
  onChange: (tags: string[]) => void
  suggestions?: string[]
}

function normalize(tag: string) {
  return tag.trim().toLowerCase().replace(/^#/, '').replace(/\s+/g, '-')
}

export function TagInput({ value, onChange, suggestions = [] }: Props) {
  const [draft, setDraft] = useState('')
  const add = (raw: string) => {
    const t = normalize(raw)
    if (t && !value.includes(t)) onChange([...value, t])
    setDraft('')
  }
  const remaining = suggestions.filter((s) => !value.includes(s)).slice(0, 8)

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-1.5 rounded-xl border border-stone-200 bg-white px-2.5 py-2 focus-within:border-rose-400 focus-within:ring-2 focus-within:ring-rose-100">
        {value.map((t) => (
          <span key={t} className="flex items-center gap-1 rounded-full bg-rose-50 py-0.5 pl-2.5 pr-1 text-sm text-rose-700">
            #{t}
            <button type="button" onClick={() => onChange(value.filter((x) => x !== t))} className="rounded-full px-1 hover:bg-rose-100" aria-label={`Quitar ${t}`}>
              ×
            </button>
          </span>
        ))}
        <input
          value={draft}
          onChange={(e) => {
            // En el celular la coma es la forma más cómoda de cortar
            if (e.target.value.endsWith(',')) add(e.target.value.slice(0, -1))
            else setDraft(e.target.value)
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              add(draft)
            } else if (e.key === 'Backspace' && !draft && value.length) {
              onChange(value.slice(0, -1))
            }
          }}
          onBlur={() => draft && add(draft)}
          placeholder={value.length ? '' : 'aniversario, barato, con-amigos…'}
          className="min-w-24 flex-1 bg-transparent py-0.5 text-base outline-none placeholder:text-stone-400"
        />
      </div>
      {remaining.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {remaining.map((s) => (
            <button key={s} type="button" onClick={() => add(s)} className="rounded-full border border-dashed border-stone-300 px-2.5 py-0.5 text-xs text-stone-500">
              + #{s}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
