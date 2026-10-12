import L from 'leaflet'
import { useEffect, useMemo, useState } from 'react'
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet'
import { Link } from 'react-router'
import { Tabs } from '@/components/ui/Tabs'
import { ErrorMessage } from '@/components/ui/States'
import { categoryInfo, DEFAULT_CENTER } from '@/lib/constants'
import { formatDate } from '@/lib/dates'
import type { PlaceListItem } from '@/features/places/api'
import { usePlaces } from '@/features/places/hooks'
import { placeIcon, TILE_ATTRIBUTION, TILE_URL } from './markers'

type Filter = 'all' | 'wishlist' | 'visited'
type Located = PlaceListItem & { lat: number; lng: number }

export function MapPage() {
  const { data: places = [], error } = usePlaces()
  const [filter, setFilter] = useState<Filter>('all')

  const located = useMemo(
    () =>
      places.filter(
        (p): p is Located => p.lat != null && p.lng != null && (filter === 'all' || p.status === filter),
      ),
    [places, filter],
  )

  return (
    <div className="relative h-[calc(100dvh-4.5rem-env(safe-area-inset-bottom))]">
      <div className="absolute inset-x-4 top-[calc(env(safe-area-inset-top)+0.75rem)] z-[1000] shadow-md rounded-xl">
        <Tabs
          value={filter}
          onChange={setFilter}
          tabs={[
            { value: 'all', label: 'Todos' },
            { value: 'wishlist', label: 'Pendientes' },
            { value: 'visited', label: 'Visitados' },
          ]}
        />
        {error && <div className="mt-2"><ErrorMessage error={error} /></div>}
      </div>

      <MapContainer center={DEFAULT_CENTER} zoom={12} className="size-full" zoomControl={false}>
        <TileLayer url={TILE_URL} attribution={TILE_ATTRIBUTION} />
        <FitBounds places={located} />
        {located.map((p) => (
          <Marker key={p.id} position={[p.lat, p.lng]} icon={placeIcon(categoryInfo(p.category).emoji, p.status === 'visited')}>
            <Popup>
              <Link to={`/places/${p.id}`} className="block min-w-36 !text-stone-900 no-underline">
                <div className="font-semibold">{p.name}</div>
                <div className="text-xs text-stone-500">
                  {p.status === 'visited'
                    ? `Fuimos${p.visited_at ? ` el ${formatDate(p.visited_at)}` : ''}`
                    : p.planned_date
                      ? `Planeado: ${formatDate(p.planned_date)}`
                      : 'Pendiente'}
                </div>
                <div className="mt-1 text-xs font-semibold text-rose-600">Ver detalle →</div>
              </Link>
            </Popup>
          </Marker>
        ))}
      </MapContainer>

      {places.length > 0 && located.length === 0 && (
        <p className="absolute inset-x-8 bottom-6 z-[1000] rounded-xl bg-white/95 p-3 text-center text-sm text-stone-600 shadow">
          Ningún lugar con ubicación en este filtro.
        </p>
      )}
    </div>
  )
}

/** Encuadra el mapa para que entren todos los pines cuando cambia el conjunto. */
function FitBounds({ places }: { places: Located[] }) {
  const map = useMap()
  const key = places.map((p) => p.id).join(',')
  useEffect(() => {
    if (!places.length) return
    if (places.length === 1) {
      map.setView([places[0].lat, places[0].lng], 15)
    } else {
      map.fitBounds(L.latLngBounds(places.map((p) => [p.lat, p.lng])), { padding: [60, 60], maxZoom: 15 })
    }
    // Solo cuando cambia el conjunto de ids, no en cada refetch
  }, [key, map]) // oxlint-disable-line react/exhaustive-deps
  return null
}
