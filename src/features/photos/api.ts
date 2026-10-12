import imageCompression from 'browser-image-compression'
import { PHOTOS_BUCKET, supabase, unwrap } from '@/lib/supabase'

const SIGNED_URL_TTL = 60 * 60 // 1 hora

export async function uploadPhoto(coupleId: string, placeId: string, file: File) {
  // Fotos del celular pesan 3-8MB; las bajamos a ~1600px / <500KB
  const compressed = await imageCompression(file, {
    maxSizeMB: 0.5,
    maxWidthOrHeight: 1600,
    useWebWorker: true,
    fileType: 'image/jpeg',
  })

  // El primer segmento del path tiene que ser el couple_id: lo exige la policy de storage
  const path = `${coupleId}/${placeId}/${crypto.randomUUID()}.jpg`
  const { error } = await supabase.storage.from(PHOTOS_BUCKET).upload(path, compressed, { contentType: 'image/jpeg' })
  if (error) throw new Error(error.message)

  const { error: insertError } = await supabase.from('photos').insert({ place_id: placeId, storage_path: path })
  if (insertError) {
    // Que no quede un archivo huérfano
    await supabase.storage.from(PHOTOS_BUCKET).remove([path])
    throw new Error(insertError.message)
  }
}

export async function deletePhoto(photo: { id: string; storage_path: string }) {
  unwrap(await supabase.from('photos').delete().eq('id', photo.id))
  await supabase.storage.from(PHOTOS_BUCKET).remove([photo.storage_path])
}

export async function signUrls(paths: string[]): Promise<Record<string, string>> {
  if (!paths.length) return {}
  const { data, error } = await supabase.storage.from(PHOTOS_BUCKET).createSignedUrls(paths, SIGNED_URL_TTL)
  if (error) throw new Error(error.message)
  const entries = data.flatMap((d) => (d.path && d.signedUrl ? [[d.path, d.signedUrl] as const] : []))
  return Object.fromEntries(entries)
}

export const SIGNED_URL_STALE_MS = (SIGNED_URL_TTL - 10 * 60) * 1000
