import { supabase, unwrap } from '@/lib/supabase'

export type ReviewInput = {
  place_id: string
  user_id: string
  rating: number
  comment: string | null
  would_return: boolean | null
}

export async function saveReview(input: ReviewInput) {
  // unique(place_id, user_id) → crear o editar la reseña propia con la misma llamada
  unwrap(await supabase.from('reviews').upsert(input, { onConflict: 'place_id,user_id' }))
}

export async function deleteReview(id: string) {
  unwrap(await supabase.from('reviews').delete().eq('id', id))
}

export function averageRating(reviews: { rating: number }[]): number | null {
  if (!reviews.length) return null
  return reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length
}
