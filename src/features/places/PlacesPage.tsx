import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { Chip } from '@/components/ui/Chip'
import { Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { Tabs } from '@/components/ui/Tabs'
import { EmptyState, ErrorMessage, FullScreenSpinner } from '@/components/ui/States'
import { useCouple } from '@/features/couple/hooks'
import { useSignedUrls } from '@/features/photos/hooks'
import { CATEGORIES, type PlaceCategory, type PlaceStatus } from '@/lib/constants'
import type { PlaceListItem } from './api'
import { usePlaces } from './hooks'
import { PlaceCard } from './PlaceCard'

export function PlacesPage() {
  const { data: places, isPending, error } = usePlaces()
  const { data: couple } = useCouple()
  const [params, setParams] = useSearchParams()
  const tab = (params.get('tab') as PlaceStatus | null) ?? 'wishlist'
  const [category, setCategory] = useState<PlaceCategory | null>(null)
  const [tag, setTag] = useState<string | null>(null)
  const [search, setSearch] = useState('')

  const all = places ?? []
  const counts = {
    wishlist: all.filter((p) => p.status === 'wishlist').length,
    visited: all.filter((p) => p.status === 'visited').length,
  }
  const allTags = [...new Set(all.flatMap((p) => p.tags))].sort()
  const usedCategories = CATEGORIES.filter((c) => all.some((p) => p.category === c.value))

  const filtered = sortPlaces(
    all.filter(
      (p) =>
        p.status === tab &&
        (!category || p.category === category) &&
        (!tag || p.tags.includes(tag)) &&
        (!search || `${p.name} ${p.address ?? ''} ${p.description ?? ''}`.toLowerCase().includes(search.toLowerCase())),
    ),
    tab,
  )

  // Una sola llamada para firmar las portadas de toda la lista
  const covers = filtered.flatMap((p) => (p.photos[0] ? [p.photos[0].storage_path] : []))
  const { data: coverUrls = {} } = useSignedUrls(covers)

  return (
    <div className="pb-6">
      <PageHeader title={couple?.name ?? 'Leira'} />
      <div className="space-y-3 px-4">
        <Tabs
          value={tab}
          onChange={(v) => setParams(v === 'wishlist' ? {} : { tab: v }, { replace: true })}
          tabs={[
            { value: 'wishlist', label: 'Pendientes', count: counts.wishlist },
            { value: 'visited', label: 'Visitados', count: counts.visited },
          ]}
        />

        {all.length > 3 && <Input type="search" placeholder="Buscar…" value={search} onChange={(e) => setSearch(e.target.value)} />}

        {(usedCategories.length > 1 || allTags.length > 0) && (
          <div className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none]">
            {usedCategories.length > 1 &&
              usedCategories.map((c) => (
                <Chip key={c.value} active={category === c.value} onClick={() => setCategory(category === c.value ? null : c.value)}>
                  {c.emoji} {c.label}
                </Chip>
              ))}
            {allTags.map((t) => (
              <Chip key={t} active={tag === t} onClick={() => setTag(tag === t ? null : t)}>
                #{t}
              </Chip>
            ))}
          </div>
        )}

        <ErrorMessage error={error} />
        {isPending ? (
          <FullScreenSpinner />
        ) : filtered.length === 0 ? (
          <Empty tab={tab} hasAny={all.length > 0} filtering={!!(category || tag || search)} />
        ) : (
          <div className="space-y-3">
            {filtered.map((p) => (
              <PlaceCard key={p.id} place={p} coverUrl={p.photos[0] && coverUrls[p.photos[0].storage_path]} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

function sortPlaces(list: PlaceListItem[], tab: PlaceStatus) {
  return [...list].sort((a, b) => {
    if (tab === 'visited') return (b.visited_at ?? '').localeCompare(a.visited_at ?? '')
    // Pendientes: primero los que tienen fecha (la más cercana arriba), después el resto por más nuevo
    if (a.planned_date && b.planned_date) return a.planned_date.localeCompare(b.planned_date)
    if (a.planned_date) return -1
    if (b.planned_date) return 1
    return b.created_at.localeCompare(a.created_at)
  })
}

function Empty({ tab, hasAny, filtering }: { tab: PlaceStatus; hasAny: boolean; filtering: boolean }) {
  if (filtering) return <EmptyState emoji="🔍" title="Nada con ese filtro" />
  if (tab === 'visited')
    return (
      <EmptyState emoji="🗺️" title="Todavía no fueron a ningún lado">
        Cuando vayan a un lugar, entren y toquen “¡Fuimos!”.
      </EmptyState>
    )
  return (
    <EmptyState emoji="✨" title={hasAny ? '¡Fueron a todos!' : '¿A dónde quieren ir?'}>
      <Link to="/places/new" className="font-semibold text-rose-600">
        {hasAny ? 'Agregar otro lugar' : 'Agregar el primer lugar'}
      </Link>
    </EmptyState>
  )
}
