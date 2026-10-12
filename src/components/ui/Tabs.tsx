type Tab<T extends string> = { value: T; label: string; count?: number }

export function Tabs<T extends string>({ tabs, value, onChange }: { tabs: Tab<T>[]; value: T; onChange: (v: T) => void }) {
  return (
    <div className="flex rounded-xl bg-stone-100 p-1">
      {tabs.map((t) => (
        <button
          key={t.value}
          type="button"
          onClick={() => onChange(t.value)}
          className={`flex-1 rounded-lg py-2 text-sm font-semibold transition ${
            value === t.value ? 'bg-white text-stone-900 shadow-sm' : 'text-stone-500'
          }`}
        >
          {t.label}
          {t.count != null && <span className="ml-1.5 text-xs font-normal text-stone-400">{t.count}</span>}
        </button>
      ))}
    </div>
  )
}
