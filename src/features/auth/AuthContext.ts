import type { Session } from '@supabase/supabase-js'
import { createContext, useContext } from 'react'

export type AuthState = {
  session: Session | null
  loading: boolean
}

export const AuthContext = createContext<AuthState>({ session: null, loading: true })

export function useSession() {
  return useContext(AuthContext)
}

/** Para usar dentro de rutas protegidas, donde siempre hay sesión. */
export function useUserId(): string {
  const { session } = useSession()
  if (!session) throw new Error('useUserId fuera de una ruta autenticada')
  return session.user.id
}
