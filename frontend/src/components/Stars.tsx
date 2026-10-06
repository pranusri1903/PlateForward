import { Star } from 'lucide-react'
import { cn } from '@/lib/utils'

export const Stars = ({ value, className }: { value: number; className?: string }) => (
  <span className={cn('inline-flex', className)} role="img" aria-label={`${value} out of 5 stars`}>
    {[1, 2, 3, 4, 5].map((n) => <Star key={n} className={cn('size-4', n <= value ? 'fill-amber-400 text-amber-400' : 'text-stone-300')} aria-hidden />)}
  </span>
)

export function StarInput({ value, onChange }: { value: number; onChange: (n: number) => void }) {
  return (
    <div role="radiogroup" aria-label="Rating" className="flex gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button key={n} type="button" role="radio" aria-checked={value === n} aria-label={`${n} star${n === 1 ? '' : 's'}`} onClick={() => onChange(n)} className="rounded-md p-0.5 transition hover:scale-125">
          <Star className={cn('size-8', n <= value ? 'fill-amber-400 text-amber-400' : 'text-stone-300')} aria-hidden />
        </button>
      ))}
    </div>
  )
}
