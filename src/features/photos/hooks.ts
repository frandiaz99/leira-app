import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { useCoupleId } from '@/features/couple/hooks'
import { useInvalidatePlaces } from '@/features/places/hooks'
import * as api from './api'

/** Firma un lote de paths del bucket privado. Devuelve path → URL temporal. */
export function useSignedUrls(paths: string[]) {
  const sorted = [...paths].sort()
  return useQuery({
    queryKey: ['signed-urls', sorted],
    queryFn: () => api.signUrls(sorted),
    enabled: sorted.length > 0,
    staleTime: api.SIGNED_URL_STALE_MS,
    placeholderData: keepPreviousData,
  })
}

export function useUploadPhotos(placeId: string) {
  const coupleId = useCoupleId()
  const invalidate = useInvalidatePlaces()
  return useMutation({
    mutationFn: async (files: File[]) => {
      // De a una para no saturar la conexión del celular
      for (const file of files) await api.uploadPhoto(coupleId, placeId, file)
    },
    onSettled: invalidate,
  })
}

export function useDeletePhoto() {
  const invalidate = useInvalidatePlaces()
  return useMutation({ mutationFn: api.deletePhoto, onSuccess: invalidate })
}
