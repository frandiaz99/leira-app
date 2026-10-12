import type { ButtonHTMLAttributes } from 'react'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & { active?: boolean }

export function Chip({ active, className = '', children, ...rest }: Props) {
  return (
    <button
      type="button"
      className={`shrink-0 rounded-full border px-3 py-1.5 text-sm font-medium transition ${
        active ? 'border-rose-600 bg-rose-600 text-white' : 'border-stone-200 bg-white text-stone-700 hover:bg-stone-50'
      } ${className}`}
      {...rest}
    >
      {children}
    </button>
  )
}
