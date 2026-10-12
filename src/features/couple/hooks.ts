import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSession, useUserId } from '@/features/auth/AuthContext'
import * as api from './api'

export function useMyProfile() {
  const { session } = useSession()
  const userId = session?.user.id
  return useQuery({
    queryKey: ['profile', userId],
    queryFn: () => api.fetchMyProfile(userId!),
    enabled: !!userId,
  })
}

/** couple_id del usuario; solo usar en rutas que ya pasaron el guard de pareja. */
export function useCoupleId(): string {
  const { data } = useMyProfile()
  if (!data?.couple_id) throw new Error('useCoupleId sin pareja')
  return data.couple_id
}

export function useCouple() {
  const coupleId = useCoupleId()
  return useQuery({ queryKey: ['couple', coupleId], queryFn: () => api.fetchCouple(coupleId) })
}

export function useMembers() {
  const coupleId = useCoupleId()
  return useQuery({ queryKey: ['members', coupleId], queryFn: () => api.fetchMembers(coupleId) })
}

/** Mapa id → nombre, para mostrar quién reseñó/subió qué. */
export function useMemberNames(): Record<string, string> {
  const { data } = useMembers()
  return Object.fromEntries((data ?? []).map((m) => [m.id, m.display_name || 'Sin nombre']))
}

export function useCreateCouple() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: api.createCouple,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  })
}

export function useJoinCouple() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: api.joinCouple,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile'] }),
  })
}

export function useUpdateProfile() {
  const qc = useQueryClient()
  const userId = useUserId()
  return useMutation({
    mutationFn: (patch: { display_name: string }) => api.updateProfile(userId, patch),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['profile'] })
      qc.invalidateQueries({ queryKey: ['members'] })
    },
  })
}

export function useUpdateCoupleName() {
  const qc = useQueryClient()
  const coupleId = useCoupleId()
  return useMutation({
    mutationFn: (name: string) => api.updateCoupleName(coupleId, name),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['couple'] }),
  })
}
