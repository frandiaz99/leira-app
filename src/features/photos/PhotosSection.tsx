import { useRef, useState } from 'react'
import { Spinner } from '@/components/ui/Button'
import { ErrorMessage } from '@/components/ui/States'
import { useUserId } from '@/features/auth/AuthContext'
import { useMemberNames } from '@/features/couple/hooks'
import type { Photo } from '@/features/places/api'
import { useDeletePhoto, useSignedUrls, useUploadPhotos } from './hooks'
import { Lightbox } from './Lightbox'

export function PhotosSection({ placeId, photos }: { placeId: string; photos: Photo[] }) {
  const userId = useUserId()
  const names = useMemberNames()
  const inputRef = useRef<HTMLInputElement>(null)
  const [open, setOpen] = useState<number | null>(null)
  const upload = useUploadPhotos(placeId)
  const del = useDeletePhoto()
  const { data: urls = {} } = useSignedUrls(photos.map((p) => p.storage_path))

  const visible = photos.filter((p) => urls[p.storage_path])
  const current = open != null ? visible[open] : null

  return (
    <section>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-bold text-stone-900">Fotos</h2>
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={upload.isPending}
          className="flex items-center gap-1.5 rounded-full bg-rose-50 px-3 py-1.5 text-sm font-semibold text-rose-700 disabled:opacity-60"
        >
          {upload.isPending ? <Spinner className="size-4" /> : '＋'} {upload.isPending ? 'Subiendo…' : 'Agregar'}
        </button>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          multiple
          hidden
          onChange={(e) => {
            const files = Array.from(e.target.files ?? [])
            if (files.length) upload.mutate(files)
            e.target.value = ''
          }}
        />
      </div>

      <ErrorMessage error={upload.error ?? del.error} />

      {photos.length === 0 ? (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className="flex w-full flex-col items-center rounded-2xl border-2 border-dashed border-stone-200 py-8 text-stone-500"
        >
          <span className="text-3xl">📸</span>
          <span className="mt-1 text-sm">Subí las fotos de ese día</span>
        </button>
      ) : (
        <div className="grid grid-cols-3 gap-1.5">
          {photos.map((p) => {
            const url = urls[p.storage_path]
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => url && setOpen(visible.indexOf(p))}
                className="aspect-square overflow-hidden rounded-lg bg-stone-200"
              >
                {url && <img src={url} alt="" loading="lazy" className="size-full object-cover" />}
              </button>
            )
          })}
        </div>
      )}

      {current && open != null && (
        <Lightbox
          urls={visible.map((p) => urls[p.storage_path])}
          index={open}
          onIndexChange={setOpen}
          onClose={() => setOpen(null)}
          caption={`Subida por ${names[current.uploaded_by] ?? '…'}`}
          onDelete={
            current.uploaded_by === userId
              ? () => {
                  if (!confirm('¿Borrar esta foto?')) return
                  del.mutate(current)
                  setOpen(null)
                }
              : undefined
          }
        />
      )}
    </section>
  )
}
