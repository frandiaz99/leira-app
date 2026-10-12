import { Link } from 'react-router'
import { StarRating } from '@/components/ui/StarRating'
import { useUserId } from '@/features/auth/AuthContext'
import { useMembers } from '@/features/couple/hooks'
import type { Review } from '@/features/places/api'
import { averageRating } from './api'

export function ReviewsSection({ placeId, reviews }: { placeId: string; reviews: Review[] }) {
  const userId = useUserId()
  const { data: members = [] } = useMembers()
  const avg = averageRating(reviews)
  // Yo primero, después mi pareja
  const ordered = [...members].sort((a, b) => Number(b.id === userId) - Number(a.id === userId))

  return (
    <section>
      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-bold text-stone-900">¿Qué nos pareció?</h2>
        {avg != null && (
          <div className="flex items-center gap-1.5">
            <StarRating value={Math.round(avg * 2) / 2} />
            <span className="text-sm font-semibold text-stone-700">{avg.toFixed(1)}</span>
          </div>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        {ordered.map((m) => {
          const review = reviews.find((r) => r.user_id === m.id)
          const mine = m.id === userId
          return (
            <article key={m.id} className="rounded-2xl border border-stone-100 bg-white p-4 shadow-sm">
              <div className="mb-2 flex items-center justify-between">
                <span className="font-semibold text-stone-900">{mine ? 'Vos' : m.display_name}</span>
                {mine && (
                  <Link to={`/places/${placeId}/review`} className="text-sm font-semibold text-rose-600">
                    {review ? 'Editar' : 'Escribir'}
                  </Link>
                )}
              </div>
              {review ? (
                <>
                  <StarRating value={review.rating} />
                  {review.comment && <p className="mt-2 whitespace-pre-line text-sm text-stone-700">{review.comment}</p>}
                  {review.would_return != null && (
                    <p className="mt-2 text-xs font-medium text-stone-500">
                      {review.would_return ? '🔁 Volvería' : '🚫 No volvería'}
                    </p>
                  )}
                </>
              ) : (
                <p className="text-sm text-stone-400">{mine ? 'Todavía no dejaste tu reseña.' : 'Todavía no reseñó.'}</p>
              )}
            </article>
          )
        })}
        {members.length === 1 && (
          <p className="rounded-2xl border border-dashed border-stone-200 p-4 text-sm text-stone-500">
            Cuando tu pareja se una con el código (en Perfil), va a poder dejar su reseña acá.
          </p>
        )}
      </div>
    </section>
  )
}
