// Las columnas `date` vienen como 'YYYY-MM-DD'. new Date('2026-10-10') las toma como UTC
// y en Argentina se ve el día anterior, así que se parsean como fecha local.
export function parseLocalDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function todayIso(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

const fmt = new Intl.DateTimeFormat('es-AR', { day: 'numeric', month: 'short', year: 'numeric' })

export function formatDate(iso: string): string {
  return fmt.format(parseLocalDate(iso))
}

/** "hoy", "mañana", "en 5 días", "hace 3 días" */
export function relativeDays(iso: string): string {
  const today = parseLocalDate(todayIso()).getTime()
  const diff = Math.round((parseLocalDate(iso).getTime() - today) / 86_400_000)
  if (diff === 0) return 'hoy'
  if (diff === 1) return 'mañana'
  if (diff === -1) return 'ayer'
  return diff > 0 ? `en ${diff} días` : `hace ${-diff} días`
}
