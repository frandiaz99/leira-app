import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { ErrorMessage } from '@/components/ui/States'
import { supabase } from '@/lib/supabase'
import { useSession } from './AuthContext'

// Login sin contraseña: se manda un mail con link + código de 6 dígitos.
// El código importa en iPhone: el link abre Safari, no la app instalada,
// así que desde la PWA conviene tipear el código.
export function LoginPage() {
  const { session } = useSession()
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [step, setStep] = useState<'email' | 'code'>('email')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)

  if (session) return <Navigate to="/" replace />

  async function sendEmail(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    const { error } = await supabase.auth.signInWithOtp({
      email: email.trim(),
      options: { emailRedirectTo: window.location.origin },
    })
    setBusy(false)
    if (error) setError(error)
    else setStep('code')
  }

  async function verifyCode(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    const { error } = await supabase.auth.verifyOtp({ email: email.trim(), token: code.trim(), type: 'email' })
    setBusy(false)
    if (error) setError(error)
  }

  return (
    <div className="flex min-h-dvh flex-col justify-center px-6 py-12">
      <div className="mx-auto w-full max-w-sm">
        <div className="mb-10 text-center">
          <img src="/favicon.svg" alt="" className="mx-auto mb-4 size-20" />
          <h1 className="text-3xl font-bold text-stone-900">Plancito</h1>
          <p className="mt-2 text-stone-500">Los lugares a los que queremos ir, y lo que nos parecieron.</p>
        </div>

        {step === 'email' ? (
          <form onSubmit={sendEmail} className="space-y-4">
            <Field label="Tu email">
              <Input type="email" required autoComplete="email" inputMode="email" placeholder="vos@mail.com" value={email} onChange={(e) => setEmail(e.target.value)} />
            </Field>
            <ErrorMessage error={error} />
            <Button type="submit" block loading={busy}>
              Entrar
            </Button>
          </form>
        ) : (
          <form onSubmit={verifyCode} className="space-y-4">
            <p className="text-sm text-stone-600">
              Te mandamos un mail a <b>{email}</b>. Tocá el link o escribí el código acá.
            </p>
            <Field label="Código">
              <Input
                required
                inputMode="numeric"
                autoComplete="one-time-code"
                placeholder="123456"
                maxLength={10}
                className="text-center text-2xl tracking-[0.4em]"
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
              />
            </Field>
            <ErrorMessage error={error} />
            <Button type="submit" block loading={busy}>
              Verificar
            </Button>
            <Button type="button" variant="ghost" block onClick={() => setStep('email')}>
              Usar otro email
            </Button>
          </form>
        )}
      </div>
    </div>
  )
}
