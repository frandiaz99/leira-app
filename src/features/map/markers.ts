import L from 'leaflet'

const cache = new Map<string, L.DivIcon>()

/** Pin en forma de gota con el emoji de la categoría. Rosa = pendiente, verde = visitado. */
export function placeIcon(emoji: string, visited: boolean): L.DivIcon {
  const key = `${emoji}-${visited}`
  let icon = cache.get(key)
  if (!icon) {
    icon = L.divIcon({
      className: '',
      html: `<div class="leira-pin ${visited ? 'leira-pin--visited' : ''}"><span>${emoji}</span></div>`,
      iconSize: [36, 36],
      iconAnchor: [18, 36],
      popupAnchor: [0, -34],
    })
    cache.set(key, icon)
  }
  return icon
}

export const TILE_URL = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
export const TILE_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
