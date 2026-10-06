import { formatDistanceToNowStrict } from 'date-fns'
import { Timer } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useNow } from './hooks'

export function Countdown({ to, className, icon }: { to: string; className?: string; icon?: boolean }) {
  const ms = new Date(to).getTime() - useNow()
  return (
    <span className={cn('inline-flex items-center gap-1.5 font-semibold tabular-nums', ms < 3 * 3600_000 ? 'text-amber-600' : 'text-stone-700', className)}>
      {icon && <Timer className="size-4" aria-hidden />}
      {ms <= 0 ? 'Expired' : `${formatDistanceToNowStrict(new Date(to))} left`}
    </span>
  )
}
