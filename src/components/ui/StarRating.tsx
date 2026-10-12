type Props = {
  value: number
  onChange?: (v: number) => void
  size?: 'sm' | 'lg'
}

export function StarRating({ value, onChange, size = 'sm' }: Props) {
  const textCls = size === 'lg' ? 'text-4xl' : 'text-base'
  return (
    <div
      className={`flex ${size === 'lg' ? 'gap-2' : 'gap-0.5'}`}
      role={onChange ? 'radiogroup' : 'img'}
      aria-label={`${value} de 5 estrellas`}
    >
      {[1, 2, 3, 4, 5].map((n) => {
        // Media estrella para promedios (ej. 3.5)
        const fill = value >= n ? 'full' : value >= n - 0.5 ? 'half' : 'empty'
        const star = (
          <span className={`${textCls} relative inline-block leading-none`}>
            <span className="text-stone-300">★</span>
            {fill !== 'empty' && (
              <span className={`absolute inset-0 overflow-hidden text-amber-400 ${fill === 'half' ? 'w-1/2' : ''}`}>★</span>
            )}
          </span>
        )
        return onChange ? (
          <button
            key={n}
            type="button"
            role="radio"
            aria-checked={value === n}
            aria-label={`${n} estrellas`}
            onClick={() => onChange(n)}
            className="transition active:scale-90"
          >
            {star}
          </button>
        ) : (
          <span key={n}>{star}</span>
        )
      })}
    </div>
  )
}
