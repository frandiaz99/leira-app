import type { ReactNode } from 'react'
import { Spinner } from './Button'

export function FullScreenSpinner() {
  return (
    <div className="flex min-h-[60vh] items-center justify-center text-rose-500">
      <Spinner className="size-8" />
    </div>
  )
}

export function EmptyState({ emoji, title, children }: { emoji: string; title: string; children?: ReactNode }) {
  return (
    <div className="flex flex-col items-center px-6 py-16 text-center">
      <div className="mb-3 text-5xl">{emoji}</div>
      <p className="font-semibold text-stone-800">{title}</p>
      {children && <div className="mt-1 text-sm text-stone-500">{children}</div>}
    </div>
  )
}

export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null
  const msg = error instanceof Error ? error.message : String(error)
  return <p className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700">{msg}</p>
}
