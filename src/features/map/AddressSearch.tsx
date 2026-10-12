import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Spinner } from '@/components/ui/Button'
import { Input } from '@/components/ui/Field'
import { searchAddress, type GeoResult } from '@/lib/geocode'
import { useDebounce } from '@/lib/useDebounce'

export function AddressSearch({ onSelect }: { onSelect: (r: GeoResult) => void }) {
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const debounced = useDebounce(query.trim(), 600)

  const { data = [], isFetching, error } = useQuery({
    queryKey: ['geocode', debounced],
    queryFn: ({ signal }) => searchAddress(debounced, signal),
    enabled: debounced.length >= 3,
    staleTime: Infinity,
  })

  return (
    <div className="relative">
      <Input
        type="search"
        value={query}
        placeholder="Buscar dirección o nombre del lugar…"
        onChange={(e) => {
          setQuery(e.target.value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
      />
      {isFetching && <Spinner className="absolute right-3 top-3 size-5 text-stone-400" />}
      {open && debounced.length >= 3 && !isFetching && (
        <ul className="absolute inset-x-0 top-full z-[1100] mt-1 max-h-64 overflow-auto rounded-xl border border-stone-200 bg-white shadow-lg">
          {error && <li className="px-3 py-2 text-sm text-red-600">No se pudo buscar.</li>}
          {!error && data.length === 0 && <li className="px-3 py-2 text-sm text-stone-500">Sin resultados. Probá tocando en el mapa.</li>}
          {data.map((r) => (
            <li key={`${r.lat},${r.lng}`}>
              <button
                type="button"
                className="w-full px-3 py-2.5 text-left text-sm text-stone-700 hover:bg-rose-50"
                onClick={() => {
                  onSelect(r)
                  setQuery('')
                  setOpen(false)
                }}
              >
                {r.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
