import { useEffect } from 'react'
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import { DEFAULT_CENTER } from '@/lib/constants'
import { placeIcon, TILE_ATTRIBUTION, TILE_URL } from './markers'

type Props = {
  lat: number | null
  lng: number | null
  emoji: string
  /** Si no se pasa, el mapa es de solo lectura. */
  onPick?: (lat: number, lng: number) => void
  className?: string
}

export function LocationPicker({ lat, lng, emoji, onPick, className = 'h-56' }: Props) {
  const hasPoint = lat != null && lng != null
  return (
    <div className={`overflow-hidden rounded-2xl border border-stone-200 ${className}`}>
      <MapContainer
        center={hasPoint ? [lat, lng] : DEFAULT_CENTER}
        zoom={hasPoint ? 15 : 11}
        className="size-full"
        scrollWheelZoom={!!onPick}
        dragging={!!onPick}
        zoomControl={!!onPick}
        attributionControl={false}
      >
        <TileLayer url={TILE_URL} attribution={TILE_ATTRIBUTION} />
        {hasPoint && <Marker position={[lat, lng]} icon={placeIcon(emoji, false)} />}
        {hasPoint && <Recenter lat={lat} lng={lng} />}
        {onPick && <ClickHandler onPick={onPick} />}
      </MapContainer>
    </div>
  )
}

function Recenter({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap()
  useEffect(() => {
    map.setView([lat, lng], Math.max(map.getZoom(), 15))
  }, [lat, lng, map])
  return null
}

function ClickHandler({ onPick }: { onPick: (lat: number, lng: number) => void }) {
  useMapEvents({ click: (e) => onPick(e.latlng.lat, e.latlng.lng) })
  return null
}
