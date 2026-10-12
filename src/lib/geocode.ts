// Geocoding gratis con Nominatim (OpenStreetMap).
// Política de uso: máx. ~1 request/seg, por eso la búsqueda va con debounce.
const BASE = 'https://nominatim.openstreetmap.org'

export type GeoResult = {
  label: string
  lat: number
  lng: number
}

type NominatimItem = { display_name: string; lat: string; lon: string }

export async function searchAddress(query: string, signal?: AbortSignal): Promise<GeoResult[]> {
  const params = new URLSearchParams({ q: query, format: 'jsonv2', limit: '5', 'accept-language': 'es' })
  const res = await fetch(`${BASE}/search?${params}`, { signal })
  if (!res.ok) throw new Error('No se pudo buscar la dirección')
  const items: NominatimItem[] = await res.json()
  return items.map((i) => ({ label: i.display_name, lat: Number(i.lat), lng: Number(i.lon) }))
}

export async function reverseGeocode(lat: number, lng: number): Promise<string | null> {
  const params = new URLSearchParams({ lat: String(lat), lon: String(lng), format: 'jsonv2', 'accept-language': 'es' })
  const res = await fetch(`${BASE}/reverse?${params}`)
  if (!res.ok) return null
  const item: Partial<NominatimItem> = await res.json()
  return item.display_name ?? null
}

export function googleMapsUrl(place: { lat: number | null; lng: number | null; address: string | null; name: string }) {
  const q = place.lat != null && place.lng != null ? `${place.lat},${place.lng}` : (place.address ?? place.name)
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`
}
