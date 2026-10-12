import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { isSupabaseConfigured } from '@/lib/supabase'
import { router } from './routes'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1 },
  },
})

export default function App() {
  if (!isSupabaseConfigured) return <MissingConfig />
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  )
}

function MissingConfig() {
  return (
    <div className="mx-auto max-w-md px-6 py-16 text-stone-700">
      <h1 className="mb-3 text-2xl font-bold">Falta configurar Supabase</h1>
      <p>
        Copiá <code className="rounded bg-stone-100 px-1">.env.example</code> a{' '}
        <code className="rounded bg-stone-100 px-1">.env.local</code>, completá <code>VITE_SUPABASE_URL</code> y{' '}
        <code>VITE_SUPABASE_ANON_KEY</code> y reiniciá <code>npm run dev</code>.
      </p>
    </div>
  )
}
