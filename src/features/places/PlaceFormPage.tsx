import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router'
import { z } from 'zod'
import { Button } from '@/components/ui/Button'
import { Field, Input, Textarea } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { ErrorMessage, FullScreenSpinner } from '@/components/ui/States'
import { AddressSearch } from '@/features/map/AddressSearch'
import { LocationPicker } from '@/features/map/LocationPicker'
import { CATEGORIES, categoryInfo, type PlaceCategory } from '@/lib/constants'
import { reverseGeocode } from '@/lib/geocode'
import type { Place, PlaceInput } from './api'
import { useCreatePlace, usePlace, usePlaces, useUpdatePlace } from './hooks'
import { TagInput } from './TagInput'

const schema = z.object({
  name: z.string().trim().min(1, 'Ponele un nombre'),
  category: z.enum(CATEGORIES.map((c) => c.value) as [PlaceCategory, ...PlaceCategory[]]),
  address: z.string(),
  lat: z.number().nullable(),
  lng: z.number().nullable(),
  tags: z.array(z.string()),
  planned_date: z.string(),
  link: z.string().trim(),
  description: z.string(),
})

type FormValues = z.infer<typeof schema>

function toFormValues(place?: Place): FormValues {
  return {
    name: place?.name ?? '',
    category: place?.category ?? 'restaurante',
    address: place?.address ?? '',
    lat: place?.lat ?? null,
    lng: place?.lng ?? null,
    tags: place?.tags ?? [],
    planned_date: place?.planned_date ?? '',
    link: place?.link ?? '',
    description: place?.description ?? '',
  }
}

function toInput(v: FormValues): PlaceInput {
  const link = v.link && !/^https?:\/\//i.test(v.link) ? `https://${v.link}` : v.link
  return {
    name: v.name,
    category: v.category,
    address: v.address.trim() || null,
    lat: v.lat,
    lng: v.lng,
    tags: v.tags,
    planned_date: v.planned_date || null,
    link: link || null,
    description: v.description.trim() || null,
  }
}

export function NewPlacePage() {
  const navigate = useNavigate()
  const create = useCreatePlace()
  return (
    <PlaceForm
      title="Nuevo lugar"
      submitLabel="Agregar a la lista"
      error={create.error}
      saving={create.isPending}
      onSubmit={(input) => create.mutate(input, { onSuccess: (p) => navigate(`/places/${p.id}`, { replace: true }) })}
    />
  )
}

export function EditPlacePage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { data: place, isPending, error } = usePlace(id)
  const update = useUpdatePlace(id)

  if (isPending) return <FullScreenSpinner />
  if (!place) return <ErrorMessage error={error} />

  return (
    <PlaceForm
      key={place.id}
      title="Editar lugar"
      submitLabel="Guardar cambios"
      initial={place}
      error={update.error}
      saving={update.isPending}
      onSubmit={(input) => update.mutate(input, { onSuccess: () => navigate(-1) })}
    />
  )
}

type FormProps = {
  title: string
  submitLabel: string
  initial?: Place
  saving: boolean
  error: unknown
  onSubmit: (input: PlaceInput) => void
}

function PlaceForm({ title, submitLabel, initial, saving, error, onSubmit }: FormProps) {
  const { data: places = [] } = usePlaces()
  const tagSuggestions = [...new Set(places.flatMap((p) => p.tags))].sort()
  const {
    register,
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: toFormValues(initial) })

  const [category, lat, lng, address] = useWatch({ control, name: ['category', 'lat', 'lng', 'address'] })

  async function pickOnMap(newLat: number, newLng: number) {
    setValue('lat', newLat)
    setValue('lng', newLng)
    // Solo autocompletar la dirección si está vacía, para no pisar lo que escribieron
    if (!address.trim()) {
      const label = await reverseGeocode(newLat, newLng)
      if (label) setValue('address', label)
    }
  }

  return (
    <div className="pb-10">
      <PageHeader back title={title} />
      <form onSubmit={handleSubmit((v) => onSubmit(toInput(v)))} className="space-y-5 px-4">
        <Field label="Nombre" error={errors.name?.message}>
          <Input {...register('name')} placeholder="Ej: La Cabrera, Bariloche, Teatro Colón…" autoFocus={!initial} />
        </Field>

        <div className="space-y-1.5">
          <span className="text-sm font-medium text-stone-700">Categoría</span>
          <div className="grid grid-cols-4 gap-2">
            {CATEGORIES.map((c) => (
              <button
                key={c.value}
                type="button"
                onClick={() => setValue('category', c.value)}
                className={`flex flex-col items-center gap-0.5 rounded-xl border py-2 text-xs font-medium ${
                  category === c.value ? 'border-rose-500 bg-rose-50 text-rose-700' : 'border-stone-200 bg-white text-stone-600'
                }`}
              >
                <span className="text-xl">{c.emoji}</span>
                {c.label}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-2">
          <span className="text-sm font-medium text-stone-700">Ubicación</span>
          <AddressSearch
            onSelect={(r) => {
              setValue('lat', r.lat)
              setValue('lng', r.lng)
              setValue('address', r.label)
            }}
          />
          <LocationPicker lat={lat} lng={lng} emoji={categoryInfo(category).emoji} onPick={pickOnMap} />
          <p className="text-xs text-stone-500">Buscá arriba o tocá el mapa para marcar el punto.</p>
          <Input {...register('address')} placeholder="Dirección (opcional)" />
          {lat != null && (
            <button
              type="button"
              className="text-xs font-medium text-stone-500 underline"
              onClick={() => {
                setValue('lat', null)
                setValue('lng', null)
              }}
            >
              Quitar ubicación del mapa
            </button>
          )}
        </div>

        <Field label="¿Cuándo piensan ir?" hint="Opcional">
          <Input type="date" {...register('planned_date')} />
        </Field>

        <div className="space-y-1.5">
          <span className="text-sm font-medium text-stone-700">Etiquetas</span>
          <Controller
            control={control}
            name="tags"
            render={({ field }) => <TagInput value={field.value} onChange={field.onChange} suggestions={tagSuggestions} />}
          />
        </div>

        <Field label="Link" hint="Instagram, web, reserva…">
          <Input {...register('link')} inputMode="url" placeholder="instagram.com/…" />
        </Field>

        <Field label="Notas">
          <Textarea {...register('description')} placeholder="Por qué queremos ir, qué pedir, quién lo recomendó…" />
        </Field>

        <ErrorMessage error={error} />
        <Button type="submit" block loading={saving}>
          {submitLabel}
        </Button>
      </form>
    </div>
  )
}
