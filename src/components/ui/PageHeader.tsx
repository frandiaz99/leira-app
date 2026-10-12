import type { ReactNode } from 'react'
import { useNavigate } from 'react-router'

export function PageHeader({ title, back, actions }: { title?: ReactNode; back?: boolean; actions?: ReactNode }) {
  const navigate = useNavigate()
  return (
    <header className="sticky top-0 z-[1000] flex items-center gap-2 bg-cream/90 px-4 pb-3 pt-[calc(env(safe-area-inset-top)+0.75rem)] backdrop-blur">
      {back && (
        <button
          type="button"
          onClick={() => (window.history.state?.idx > 0 ? navigate(-1) : navigate('/'))}
          className="-ml-2 rounded-full p-2 text-stone-700 hover:bg-stone-100"
          aria-label="Volver"
        >
          <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="m15 18-6-6 6-6" />
          </svg>
        </button>
      )}
      <h1 className="flex-1 truncate text-xl font-bold text-stone-900">{title}</h1>
      {actions}
    </header>
  )
}
