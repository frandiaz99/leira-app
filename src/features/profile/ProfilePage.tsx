import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { ErrorMessage } from '@/components/ui/States'
import { useSession } from '@/features/auth/AuthContext'
import { useCouple, useMembers, useMyProfile, useUpdateCoupleName, useUpdateProfile } from '@/features/couple/hooks'
import { usePlaces } from '@/features/places/hooks'
import { supabase } from '@/lib/supabase'

export function ProfilePage() {
  const { session } = useSession()
  const { data: profile } = useMyProfile()
  const { data: couple } = useCouple()
  const { data: members = [] } = useMembers()
  const { data: places = [] } = usePlaces()
  const updateProfile = useUpdateProfile()
  const updateCouple = useUpdateCoupleName()
  const [name, setName] = useState<string | null>(null)
  const [coupleName, setCoupleName] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const partner = members.find((m) => m.id !== session?.user.id)
  const visited = places.filter((p) => p.status === 'visited').length

  async function shareCode() {
    if (!couple) return
    const text = `Sumate a nuestra lista de lugares en Leira 💞\nEntrá a ${window.location.origin} y usá el código: ${couple.invite_code}`
    if (navigator.share) {
      await navigator.share({ text }).catch(() => {})
    } else {
      await navigator.clipboard.writeText(couple.invite_code)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  return (
    <div className="pb-10">
      <PageHeader title="Perfil" />
      <div className="space-y-6 px-4">
        <div className="grid grid-cols-2 gap-3">
          <Stat value={places.length - visited} label="pendientes" />
          <Stat value={visited} label="visitados" />
        </div>

        <section className="space-y-4 rounded-2xl bg-white p-4 shadow-sm">
          <h2 className="font-bold text-stone-900">Nosotros</h2>
          {partner ? (
            <p className="text-sm text-stone-600">
              Vos y <b>{partner.display_name}</b> 💞
            </p>
          ) : (
            couple && (
              <div className="space-y-2 rounded-xl bg-rose-50 p-3">
                <p className="text-sm text-stone-700">Pasale este código a tu pareja para que se una:</p>
                <div className="flex items-center gap-2">
                  <span className="flex-1 text-center font-mono text-2xl font-bold tracking-[0.3em] text-rose-700">{couple.invite_code}</span>
                  <Button variant="secondary" onClick={shareCode}>
                    {copied ? '¡Copiado!' : 'Compartir'}
                  </Button>
                </div>
              </div>
            )
          )}
          <SaveField
            label="Nombre del espacio"
            value={coupleName ?? couple?.name ?? ''}
            onChange={setCoupleName}
            dirty={coupleName != null && coupleName !== couple?.name}
            saving={updateCouple.isPending}
            onSave={() => coupleName?.trim() && updateCouple.mutate(coupleName.trim(), { onSuccess: () => setCoupleName(null) })}
          />
          <ErrorMessage error={updateCouple.error} />
        </section>

        <section className="space-y-4 rounded-2xl bg-white p-4 shadow-sm">
          <h2 className="font-bold text-stone-900">Vos</h2>
          <SaveField
            label="Tu nombre"
            value={name ?? profile?.display_name ?? ''}
            onChange={setName}
            dirty={name != null && name !== profile?.display_name}
            saving={updateProfile.isPending}
            onSave={() => name?.trim() && updateProfile.mutate({ display_name: name.trim() }, { onSuccess: () => setName(null) })}
          />
          <ErrorMessage error={updateProfile.error} />
          <p className="text-sm text-stone-500">{session?.user.email}</p>
        </section>

        <Button variant="danger" block onClick={() => supabase.auth.signOut()}>
          Cerrar sesión
        </Button>
      </div>
    </div>
  )
}

function Stat({ value, label }: { value: number; label: string }) {
  return (
    <div className="rounded-2xl bg-white p-4 text-center shadow-sm">
      <div className="text-3xl font-bold text-rose-600">{value}</div>
      <div className="text-sm text-stone-500">{label}</div>
    </div>
  )
}

type SaveFieldProps = {
  label: string
  value: string
  onChange: (v: string) => void
  dirty: boolean
  saving: boolean
  onSave: () => void
}

function SaveField({ label, value, onChange, dirty, saving, onSave }: SaveFieldProps) {
  return (
    <Field label={label}>
      <div className="flex gap-2">
        <Input value={value} onChange={(e) => onChange(e.target.value)} />
        {dirty && (
          <Button type="button" onClick={onSave} loading={saving}>
            Guardar
          </Button>
        )}
      </div>
    </Field>
  )
}
