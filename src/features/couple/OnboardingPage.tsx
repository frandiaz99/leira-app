import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { ErrorMessage, FullScreenSpinner } from '@/components/ui/States'
import { supabase } from '@/lib/supabase'
import { useCreateCouple, useJoinCouple, useMyProfile, useUpdateProfile } from './hooks'

export function OnboardingPage() {
  const profile = useMyProfile()
  const updateProfile = useUpdateProfile()
  const createCouple = useCreateCouple()
  const joinCouple = useJoinCouple()
  const [name, setName] = useState<string | null>(null)
  const [mode, setMode] = useState<'create' | 'join'>('create')
  const [coupleName, setCoupleName] = useState('')
  const [code, setCode] = useState('')

  if (profile.isPending) return <FullScreenSpinner />
  if (profile.data?.couple_id) return <Navigate to="/" replace />

  const displayName = name ?? profile.data?.display_name ?? ''
  const busy = updateProfile.isPending || createCouple.isPending || joinCouple.isPending

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (displayName.trim() && displayName !== profile.data?.display_name) {
      await updateProfile.mutateAsync({ display_name: displayName.trim() })
    }
    if (mode === 'create') createCouple.mutate(coupleName)
    else joinCouple.mutate(code)
  }

  return (
    <div className="flex min-h-dvh flex-col justify-center px-6 py-12">
      <form onSubmit={submit} className="mx-auto w-full max-w-sm space-y-6">
        <div className="text-center">
          <div className="mb-2 text-5xl">💞</div>
          <h1 className="text-2xl font-bold text-stone-900">¡Bienvenido/a!</h1>
          <p className="mt-1 text-stone-500">Primero, conectemos sus cuentas.</p>
        </div>

        <Field label="¿Cómo te llamás?">
          <Input required value={displayName} onChange={(e) => setName(e.target.value)} placeholder="Tu nombre" />
        </Field>

        <div className="space-y-3">
          <OptionCard
            selected={mode === 'create'}
            onSelect={() => setMode('create')}
            title="Soy el/la primero/a"
            desc="Creo nuestro espacio y le paso el código a mi pareja."
          />
          <OptionCard
            selected={mode === 'join'}
            onSelect={() => setMode('join')}
            title="Tengo un código"
            desc="Mi pareja ya creó el espacio y me pasó el código."
          />
        </div>

        {mode === 'create' ? (
          <Field label="Nombre del espacio (opcional)">
            <Input value={coupleName} onChange={(e) => setCoupleName(e.target.value)} placeholder="Ej: Fran & Sofi" />
          </Field>
        ) : (
          <Field label="Código de invitación">
            <Input
              required
              value={code}
              onChange={(e) => setCode(e.target.value.toUpperCase())}
              placeholder="ABC123"
              maxLength={6}
              className="text-center text-xl tracking-[0.3em] uppercase"
            />
          </Field>
        )}

        <ErrorMessage error={updateProfile.error ?? createCouple.error ?? joinCouple.error} />
        <Button type="submit" block loading={busy}>
          {mode === 'create' ? 'Crear nuestro espacio' : 'Unirme'}
        </Button>
        <button type="button" onClick={() => supabase.auth.signOut()} className="block w-full text-center text-sm text-stone-500">
          Cerrar sesión
        </button>
      </form>
    </div>
  )
}

function OptionCard({ selected, onSelect, title, desc }: { selected: boolean; onSelect: () => void; title: string; desc: string }) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`w-full rounded-2xl border-2 p-4 text-left transition ${selected ? 'border-rose-500 bg-rose-50' : 'border-stone-200 bg-white'}`}
    >
      <div className="font-semibold text-stone-900">{title}</div>
      <div className="text-sm text-stone-500">{desc}</div>
    </button>
  )
}
