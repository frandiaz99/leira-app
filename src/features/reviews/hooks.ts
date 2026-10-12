import { useMutation } from '@tanstack/react-query'
import { useInvalidatePlaces } from '@/features/places/hooks'
import * as api from './api'

export function useSaveReview() {
  const invalidate = useInvalidatePlaces()
  return useMutation({ mutationFn: api.saveReview, onSuccess: invalidate })
}

export function useDeleteReview() {
  const invalidate = useInvalidatePlaces()
  return useMutation({ mutationFn: api.deleteReview, onSuccess: invalidate })
}
