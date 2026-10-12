import type { Tables, TablesUpdate } from '@/lib/database.types'
import { PHOTOS_BUCKET, supabase, unwrap } from '@/lib/supabase'

export type Place = Tables<'places'>
export type Review = Tables<'reviews'>
export type Photo = Tables<'photos'>

export type PlaceListItem = Place & {
  reviews: Pick<Review, 'user_id' | 'rating'>[]
  photos: Pick<Photo, 'storage_path' | 'created_at'>[]
}

export type PlaceDetail = Place & {
  reviews: Review[]
  photos: Photo[]
}

export type PlaceInput = Pick<
  Place,
  'name' | 'description' | 'address' | 'lat' | 'lng' | 'category' | 'tags' | 'planned_date' | 'link'
>

export async function fetchPlaces(): Promise<PlaceListItem[]> {
  // RLS ya filtra por pareja: no hace falta .eq('couple_id', ...)
  return unwrap(
    await supabase
      .from('places')
      .select('*, reviews(user_id, rating), photos(storage_path, created_at)')
      .order('created_at', { ascending: false })
      .order('created_at', { referencedTable: 'photos', ascending: true }),
  )
}

export async function fetchPlace(id: string): Promise<PlaceDetail> {
  return unwrap(
    await supabase
      .from('places')
      .select('*, reviews(*), photos(*)')
      .eq('id', id)
      .order('created_at', { referencedTable: 'photos', ascending: true })
      .single(),
  )
}

export async function createPlace(input: PlaceInput): Promise<Place> {
  // couple_id y created_by los completa la base con sus defaults
  return unwrap(await supabase.from('places').insert(input).select().single())
}

export async function updatePlace(id: string, patch: TablesUpdate<'places'>): Promise<Place> {
  return unwrap(await supabase.from('places').update(patch).eq('id', id).select().single())
}

export async function deletePlace(id: string) {
  // Las filas de photos se borran en cascada, pero los archivos del storage no
  const photos = unwrap(await supabase.from('photos').select('storage_path').eq('place_id', id))
  if (photos.length) {
    const { error } = await supabase.storage.from(PHOTOS_BUCKET).remove(photos.map((p) => p.storage_path))
    if (error) throw new Error(error.message)
  }
  unwrap(await supabase.from('places').delete().eq('id', id))
}
