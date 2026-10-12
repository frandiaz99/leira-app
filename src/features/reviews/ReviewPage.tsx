import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { Button } from '@/components/ui/Button'
import { Field, Textarea } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { StarRating } from '@/components/ui/StarRating'
import { ErrorMessage, FullScreenSpinner } from '@/components/ui/States'
import { useUserId } from '@/features/auth/AuthContext'
import type { Review } from '@/features/places/api'
import { usePlace } from '@/features/places/hooks'
import { useDeleteReview, useSaveReview } from './hooks'

export function ReviewPage() {
  const { id = '' } = useParams()
  const userId = useUserId()
  const { data: place, isPending, error } = usePlace(id)

  if (isPending) return <FullScreenSpinner />
  if (!place) return <ErrorMessage error={error} />

  const existing = place.reviews.find((r) => r.user_id === userId)
  // key: si llega la reseña existente después, el form se reinicia con sus valores
  return <ReviewForm key={existing?.id ?? 'new'} placeId={id} placeName={place.name} userId={userId} existing={existing} />
}

const RETURN_OPTIONS: { value: boolean | null; label: string }[] = [
  { value: true, label: '🔁 Sí' },
  { value: false, label: '🚫 No' },
  { value: null, label: 'No sé' },
]

function ReviewForm({ placeId, placeName, userId, existing }: { placeId: string; placeName: string; userId: string; existing?: Review }) {
  const navigate = useNavigate()
  const save = useSaveReview()
  const del = useDeleteReview()
  const [rating, setRating] = useState(existing?.rating ?? 0)
  const [comment, setComment] = useState(existing?.comment ?? '')
  const [wouldReturn, setWouldReturn] = useState<boolean | null>(existing?.would_return ?? null)

  function submit(e: FormEvent) {
    e.preventDefault()
    save.mutate(
      { place_id: placeId, user_id: userId, rating, comment: comment.trim() || null, would_return: wouldReturn },
      { onSuccess: () => navigate(-1) },
    )
  }

  return (
    <div className="pb-8">
      <PageHeader back title={existing ? 'Editar reseña' : 'Tu reseña'} />
      <form onSubmit={submit} className="space-y-6 px-4">
        <p className="text-stone-600">
          ¿Qué te pareció <b>{placeName}</b>?
        </p>

        <div className="flex flex-col items-center gap-2 rounded-2xl bg-white py-6 shadow-sm">
          <StarRating value={rating} onChange={setRating} size="lg" />
          <span className="text-sm text-stone-500">{['Tocá las estrellas', 'Malo', 'Meh', 'Bien', 'Muy bueno', '¡Increíble!'][rating]}</span>
        </div>

        <Field label="Comentario">
          <Textarea value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Lo mejor, lo peor, qué pedirían de nuevo…" rows={5} />
        </Field>

        <div className="space-y-1.5">
          <span className="text-sm font-medium text-stone-700">¿Volverías?</span>
          <div className="flex gap-2">
            {RETURN_OPTIONS.map((o) => (
              <button
                key={String(o.value)}
                type="button"
                onClick={() => setWouldReturn(o.value)}
                className={`flex-1 rounded-xl border py-2.5 text-sm font-medium ${
                  wouldReturn === o.value ? 'border-rose-500 bg-rose-50 text-rose-700' : 'border-stone-200 bg-white text-stone-700'
                }`}
              >
                {o.label}
              </button>
            ))}
          </div>
        </div>

        <ErrorMessage error={save.error ?? del.error} />
        <Button type="submit" block loading={save.isPending} disabled={rating === 0}>
          Guardar reseña
        </Button>
        {existing && (
          <Button
            type="button"
            variant="danger"
            block
            loading={del.isPending}
            onClick={() => confirm('¿Borrar tu reseña?') && del.mutate(existing.id, { onSuccess: () => navigate(-1) })}
          >
            Borrar reseña
          </Button>
        )}
      </form>
    </div>
  )
}
