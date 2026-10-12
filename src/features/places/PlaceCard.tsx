import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { categoryInfo } from '@/lib/constants'
import { formatDate, relativeDays } from '@/lib/dates'
import { averageRating } from '@/features/reviews/api'
import type { PlaceListItem } from './api'

export function PlaceCard({ place, coverUrl }: { place: PlaceListItem; coverUrl?: string }) {
  const cat = categoryInfo(place.category)
  const avg = averageRating(place.reviews)
  const visited = place.status === 'visited'

  return (
    <Link
      to={`/places/${place.id}`}
      className="flex gap-3 rounded-2xl border border-stone-100 bg-white p-3 shadow-sm transition active:scale-[0.99]"
    >
      <div className="flex size-20 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-rose-50 text-3xl">
        {coverUrl ? <img src={coverUrl} alt="" loading="lazy" className="size-full object-cover" /> : cat.emoji}
      </div>
      <div className="min-w-0 flex-1 py-0.5">
        <div className="flex items-start justify-between gap-2">
          <h3 className="truncate font-semibold text-stone-900">{place.name}</h3>
          {visited && avg != null && (
            <span className="shrink-0 text-sm font-semibold text-stone-700">
              ★ {avg.toFixed(1)}
            </span>
          )}
        </div>
        <p className="truncate text-sm text-stone-500">
          {cat.label}
          {place.address && ` · ${place.address.split(',')[0]}`}
        </p>
        <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
          {visited ? (
            place.visited_at && <Badge tone="green">Fuimos el {formatDate(place.visited_at)}</Badge>
          ) : (
            place.planned_date && <Badge tone="rose">📅 {formatDate(place.planned_date)} · {relativeDays(place.planned_date)}</Badge>
          )}
          {visited && avg == null && <Badge tone="amber">Falta reseña</Badge>}
          {place.tags.slice(0, 2).map((t) => (
            <Badge key={t}>#{t}</Badge>
          ))}
        </div>
      </div>
    </Link>
  )
}

const tones = {
  stone: 'bg-stone-100 text-stone-600',
  rose: 'bg-rose-50 text-rose-700',
  green: 'bg-emerald-50 text-emerald-700',
  amber: 'bg-amber-50 text-amber-700',
}

function Badge({ tone = 'stone', children }: { tone?: keyof typeof tones; children: ReactNode }) {
  return <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${tones[tone]}`}>{children}</span>
}
