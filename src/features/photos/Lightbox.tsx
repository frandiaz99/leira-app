import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'

type Props = {
  urls: string[]
  index: number
  onIndexChange: (i: number) => void
  onClose: () => void
  caption?: string
  onDelete?: () => void
}

export function Lightbox({ urls, index, onIndexChange, onClose, caption, onDelete }: Props) {
  const touchX = useRef<number | null>(null)
  const prev = () => onIndexChange((index - 1 + urls.length) % urls.length)
  const next = () => onIndexChange((index + 1) % urls.length)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
      if (e.key === 'ArrowLeft') prev()
      if (e.key === 'ArrowRight') next()
    }
    window.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  })

  return createPortal(
    <div
      className="fixed inset-0 z-[2000] flex flex-col bg-black/95 text-white"
      onTouchStart={(e) => (touchX.current = e.touches[0].clientX)}
      onTouchEnd={(e) => {
        if (touchX.current == null) return
        const dx = e.changedTouches[0].clientX - touchX.current
        if (Math.abs(dx) > 50) (dx > 0 ? prev : next)()
        touchX.current = null
      }}
    >
      <div className="flex items-center justify-between px-4 pb-2 pt-[calc(env(safe-area-inset-top)+0.75rem)]">
        <span className="text-sm text-white/70">
          {index + 1} / {urls.length}
        </span>
        <div className="flex gap-2">
          {onDelete && (
            <button type="button" onClick={onDelete} className="rounded-full px-3 py-1.5 text-sm text-red-300 hover:bg-white/10">
              Borrar
            </button>
          )}
          <button type="button" onClick={onClose} className="rounded-full px-3 py-1.5 text-sm hover:bg-white/10" aria-label="Cerrar">
            ✕
          </button>
        </div>
      </div>
      <div className="relative flex flex-1 items-center justify-center overflow-hidden">
        <img src={urls[index]} alt="" className="max-h-full max-w-full object-contain" />
        {urls.length > 1 && (
          <>
            <button type="button" onClick={prev} className="absolute left-2 hidden rounded-full bg-white/10 p-3 sm:block" aria-label="Anterior">
              ‹
            </button>
            <button type="button" onClick={next} className="absolute right-2 hidden rounded-full bg-white/10 p-3 sm:block" aria-label="Siguiente">
              ›
            </button>
          </>
        )}
      </div>
      {caption && <p className="px-4 pb-[calc(env(safe-area-inset-bottom)+1rem)] pt-2 text-center text-sm text-white/70">{caption}</p>}
    </div>,
    document.body,
  )
}
