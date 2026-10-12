import type { InputHTMLAttributes, ReactNode, Ref, TextareaHTMLAttributes } from 'react'

const inputCls =
  'w-full rounded-xl border border-stone-200 bg-white px-3.5 py-2.5 text-base text-stone-900 placeholder:text-stone-400 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100'

export function Field({ label, error, hint, children }: { label: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block space-y-1.5">
      <span className="text-sm font-medium text-stone-700">{label}</span>
      {children}
      {error ? (
        <span className="block text-xs text-red-600">{error}</span>
      ) : (
        hint && <span className="block text-xs text-stone-500">{hint}</span>
      )}
    </label>
  )
}

// React 19: `ref` llega como prop común, sin forwardRef.
export function Input({ className = '', ref, ...rest }: InputHTMLAttributes<HTMLInputElement> & { ref?: Ref<HTMLInputElement> }) {
  return <input ref={ref} className={`${inputCls} ${className}`} {...rest} />
}

export function Textarea({
  className = '',
  ref,
  ...rest
}: TextareaHTMLAttributes<HTMLTextAreaElement> & { ref?: Ref<HTMLTextAreaElement> }) {
  return <textarea ref={ref} className={`${inputCls} min-h-24 ${className}`} {...rest} />
}
