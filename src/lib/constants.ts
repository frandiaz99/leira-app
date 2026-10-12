import type { Enums } from './database.types'

export type PlaceCategory = Enums<'place_category'>
export type PlaceStatus = Enums<'place_status'>

export const CATEGORIES: { value: PlaceCategory; label: string; emoji: string }[] = [
  { value: 'restaurante', label: 'Restaurante', emoji: '🍝' },
  { value: 'bar', label: 'Bar', emoji: '🍸' },
  { value: 'cafe', label: 'Café', emoji: '☕' },
  { value: 'viaje', label: 'Viaje', emoji: '✈️' },
  { value: 'paseo', label: 'Paseo', emoji: '🌳' },
  { value: 'experiencia', label: 'Experiencia', emoji: '🎟️' },
  { value: 'otro', label: 'Otro', emoji: '📍' },
]

export function categoryInfo(value: PlaceCategory) {
  return CATEGORIES.find((c) => c.value === value) ?? CATEGORIES[CATEGORIES.length - 1]
}

// Centro por defecto del mapa cuando todavía no hay lugares (Buenos Aires)
export const DEFAULT_CENTER: [number, number] = [-34.6037, -58.3816]
