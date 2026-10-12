import { createBrowserRouter, Navigate } from 'react-router'
import { AppLayout, RequireAuth } from '@/components/AppLayout'
import { LoginPage } from '@/features/auth/LoginPage'
import { OnboardingPage } from '@/features/couple/OnboardingPage'
import { PlacesPage } from '@/features/places/PlacesPage'
import { ProfilePage } from '@/features/profile/ProfilePage'
import { ReviewPage } from '@/features/reviews/ReviewPage'

// Las pantallas con mapa (Leaflet) se cargan al entrar, para que la lista abra rápido
export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      { path: '/onboarding', element: <OnboardingPage /> },
      {
        element: <AppLayout />,
        children: [
          { path: '/', element: <PlacesPage /> },
          { path: '/map', lazy: async () => ({ Component: (await import('@/features/map/MapPage')).MapPage }) },
          { path: '/places/new', lazy: async () => ({ Component: (await import('@/features/places/PlaceFormPage')).NewPlacePage }) },
          { path: '/places/:id', lazy: async () => ({ Component: (await import('@/features/places/PlaceDetailPage')).PlaceDetailPage }) },
          { path: '/places/:id/edit', lazy: async () => ({ Component: (await import('@/features/places/PlaceFormPage')).EditPlacePage }) },
          { path: '/places/:id/review', element: <ReviewPage /> },
          { path: '/profile', element: <ProfilePage /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
