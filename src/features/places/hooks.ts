import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { TablesUpdate } from '@/lib/database.types'
import * as api from './api'

export const placeKeys = {
  all: ['places'] as const,
  detail: (id: string) => ['places', id] as const,
}

export function usePlaces() {
  return useQuery({ queryKey: placeKeys.all, queryFn: api.fetchPlaces })
}

export function usePlace(id: string) {
  return useQuery({ queryKey: placeKeys.detail(id), queryFn: () => api.fetchPlace(id) })
}

/** Invalida lista y detalle; lo usan también reseñas y fotos. */
export function useInvalidatePlaces() {
  const qc = useQueryClient()
  return () => qc.invalidateQueries({ queryKey: placeKeys.all })
}

export function useCreatePlace() {
  const invalidate = useInvalidatePlaces()
  return useMutation({ mutationFn: api.createPlace, onSuccess: invalidate })
}

export function useUpdatePlace(id: string) {
  const invalidate = useInvalidatePlaces()
  return useMutation({
    mutationFn: (patch: TablesUpdate<'places'>) => api.updatePlace(id, patch),
    onSuccess: invalidate,
  })
}

export function useDeletePlace() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: api.deletePlace,
    onSuccess: (_data, id) => {
      qc.removeQueries({ queryKey: placeKeys.detail(id) })
      qc.invalidateQueries({ queryKey: placeKeys.all })
    },
  })
}
