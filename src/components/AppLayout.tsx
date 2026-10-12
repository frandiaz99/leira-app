import type { ReactNode } from 'react'
import { Navigate, NavLink, Outlet } from 'react-router'
import { FullScreenSpinner } from '@/components/ui/States'
import { useSession } from '@/features/auth/AuthContext'
import { useMyProfile } from '@/features/couple/hooks'

/** Exige sesión. */
export function RequireAuth() {
  const { session, loading } = useSession()
  if (loading) return <FullScreenSpinner />
  if (!session) return <Navigate to="/login" replace />
  return <Outlet />
}

/** Exige pareja; si no tiene, va al onboarding. Envuelve las pantallas con bottom nav. */
export function AppLayout() {
  const { data: profile, isPending, error } = useMyProfile()
  if (isPending) return <FullScreenSpinner />
  if (error) return <p className="p-6 text-red-700">{error.message}</p>
  if (!profile.couple_id) return <Navigate to="/onboarding" replace />

  return (
    <div className="mx-auto min-h-dvh max-w-2xl pb-[calc(4.5rem+env(safe-area-inset-bottom))]">
      <Outlet />
      <BottomNav />
    </div>
  )
}

function BottomNav() {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-[1500] border-t border-stone-200 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur">
      <div className="mx-auto flex h-[4.5rem] max-w-2xl items-center justify-around px-2">
        <NavItem to="/" label="Lista" icon={<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />} />
        <NavItem
          to="/map"
          label="Mapa"
          icon={
            <>
              <path d="M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z" />
              <path d="M9 4v14M15 6v14" />
            </>
          }
        />
        <NavLink
          to="/places/new"
          aria-label="Agregar lugar"
          className="-mt-6 flex size-14 items-center justify-center rounded-full bg-rose-600 text-3xl text-white shadow-lg shadow-rose-600/30 active:scale-95"
        >
          +
        </NavLink>
        <NavItem
          to="/profile"
          label="Perfil"
          icon={
            <>
              <circle cx="12" cy="8" r="4" />
              <path d="M4 21a8 8 0 0 1 16 0" />
            </>
          }
        />
      </div>
    </nav>
  )
}

function NavItem({ to, label, icon }: { to: string; label: string; icon: ReactNode }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) => `flex w-16 flex-col items-center gap-0.5 text-xs font-medium ${isActive ? 'text-rose-600' : 'text-stone-500'}`}
    >
      <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        {icon}
      </svg>
      {label}
    </NavLink>
  )
}
