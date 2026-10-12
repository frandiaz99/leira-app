import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState, ErrorMessage, FullScreenSpinner } from '@/components/ui/States'
import { useMemberNames } from '@/features/couple/hooks'
import { LocationPicker } from '@/features/map/LocationPicker'
import { PhotosSection } from '@/features/photos/PhotosSection'
import { ReviewsSection } from '@/features/reviews/ReviewsSection'
import { categoryInfo } from '@/lib/constants'
import { formatDate, relativeDays, todayIso } from '@/lib/dates'
import { googleMapsUrl } from '@/lib/geocode'
import type { PlaceDetail } from './api'
import { useDeletePlace, usePlace, useUpdatePlace } from './hooks'

export function PlaceDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { data: place, isPending, error } = usePlace(id)
  const del = useDeletePlace()
  const names = useMemberNames()

  if (isPending) return <FullScreenSpinner />
  if (!place)
    return (
      <>
        <PageHeader back />
        {error ? <div className="px-4"><ErrorMessage error={error} /></div> : <EmptyState emoji="🤷" title="Este lugar no existe" />}
      </>
    )

  const cat = categoryInfo(place.category)
  const visited = place.status === 'visited'

  return (
    <div className="pb-10">
      <PageHeader
        back
        actions={
          <>
            <Link to={`/places/${id}/edit`} className="rounded-full px-3 py-1.5 text-sm font-semibold text-stone-700 hover:bg-stone-100">
              Editar
            </Link>
            <button
              type="button"
              disabled={del.isPending}
              onClick={() => {
                if (confirm(`¿Borrar "${place.name}"? Se borran también reseñas y fotos.`)) {
                  del.mutate(id, { onSuccess: () => navigate('/', { replace: true }) })
                }
              }}
              className="rounded-full px-3 py-1.5 text-sm font-semibold text-red-600 hover:bg-red-50"
            >
              Borrar
            </button>
          </>
        }
      />

      <div className="space-y-6 px-4">
        <ErrorMessage error={del.error} />
        <div>
          <p className="text-sm font-medium text-rose-600">
            {cat.emoji} {cat.label}
          </p>
          <h1 className="mt-1 text-3xl font-bold leading-tight text-stone-900">{place.name}</h1>
          {place.tags.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1.5">
              {place.tags.map((t) => (
                <span key={t} className="rounded-full bg-stone-100 px-2.5 py-0.5 text-xs font-medium text-stone-600">
                  #{t}
                </span>
              ))}
            </div>
          )}
        </div>

        <VisitedControl place={place} />

        <div className="space-y-3 rounded-2xl bg-white p-4 shadow-sm">
          {place.address && (
            <InfoRow icon="📍">
              <a href={googleMapsUrl(place)} target="_blank" rel="noreferrer" className="text-stone-800 underline decoration-stone-300 underline-offset-2">
                {place.address}
              </a>
            </InfoRow>
          )}
          {!visited && place.planned_date && (
            <InfoRow icon="📅">
              {formatDate(place.planned_date)} <span className="text-stone-500">({relativeDays(place.planned_date)})</span>
            </InfoRow>
          )}
          {place.link && (
            <InfoRow icon="🔗">
              <a href={place.link} target="_blank" rel="noreferrer" className="break-all text-rose-600 underline underline-offset-2">
                {place.link.replace(/^https?:\/\/(www\.)?/, '')}
              </a>
            </InfoRow>
          )}
          {place.description && (
            <InfoRow icon="📝">
              <span className="whitespace-pre-line">{place.description}</span>
            </InfoRow>
          )}
          <InfoRow icon="➕">
            <span className="text-stone-500">
              Agregado por {names[place.created_by] ?? '…'} el {formatDate(place.created_at.slice(0, 10))}
            </span>
          </InfoRow>
        </div>

        {place.lat != null && place.lng != null && (
          <a href={googleMapsUrl(place)} target="_blank" rel="noreferrer" className="block">
            <LocationPicker lat={place.lat} lng={place.lng} emoji={cat.emoji} className="pointer-events-none h-44" />
          </a>
        )}

        {visited ? (
          <>
            <ReviewsSection placeId={id} reviews={place.reviews} />
            <PhotosSection placeId={id} photos={place.photos} />
          </>
        ) : (
          <p className="rounded-2xl border border-dashed border-stone-200 p-4 text-center text-sm text-stone-500">
            Cuando vayan, toquen <b>¡Fuimos!</b> para dejar sus reseñas y subir fotos.
          </p>
        )}
      </div>
    </div>
  )
}

function InfoRow({ icon, children }: { icon: string; children: ReactNode }) {
  return (
    <div className="flex gap-3 text-sm text-stone-800">
      <span className="w-5 shrink-0 text-center">{icon}</span>
      <div className="min-w-0 flex-1">{children}</div>
    </div>
  )
}

function VisitedControl({ place }: { place: PlaceDetail }) {
  const update = useUpdatePlace(place.id)
  const [confirming, setConfirming] = useState(false)
  const [date, setDate] = useState(todayIso())

  if (place.status === 'visited') {
    return (
      <div className="flex items-center justify-between rounded-2xl bg-emerald-50 px-4 py-3">
        <span className="font-semibold text-emerald-800">
          ✅ Fuimos{place.visited_at ? ` el ${formatDate(place.visited_at)}` : ''}
        </span>
        <button
          type="button"
          disabled={update.isPending}
          onClick={() => confirm('¿Volver a pendientes? Las reseñas y fotos se mantienen.') && update.mutate({ status: 'wishlist', visited_at: null })}
          className="text-xs font-medium text-emerald-700 underline"
        >
          Desmarcar
        </button>
      </div>
    )
  }

  if (!confirming) {
    return (
      <Button block onClick={() => setConfirming(true)} className="py-3.5 text-base">
        🎉 ¡Fuimos!
      </Button>
    )
  }

  return (
    <div className="space-y-3 rounded-2xl bg-rose-50 p-4">
      <p className="font-semibold text-stone-900">¿Cuándo fueron?</p>
      <Input type="date" value={date} max={todayIso()} onChange={(e) => setDate(e.target.value)} />
      <ErrorMessage error={update.error} />
      <div className="flex gap-2">
        <Button variant="secondary" className="flex-1" onClick={() => setConfirming(false)}>
          Cancelar
        </Button>
        <Button
          className="flex-1"
          loading={update.isPending}
          onClick={() => update.mutate({ status: 'visited', visited_at: date || todayIso() }, { onSuccess: () => setConfirming(false) })}
        >
          Confirmar
        </Button>
      </div>
    </div>
  )
}
