import { Hourglass } from 'lucide-react'
import { STATUSES } from '@/lib/meta'
import type { Listing } from '@/lib/types'
import { cn } from '@/lib/utils'
import { useNow } from './hooks'

export function StatusPill({ listing }: { listing: Pick<Listing, 'status' | 'expiresAt'> }) {
  const now = useNow()
  const soon = listing.status === 'LISTED' && new Date(listing.expiresAt).getTime() - now < 3 * 3600_000
  const { label, className } = soon ? { label: 'Expiring soon', className: 'bg-amber-100 text-amber-800' } : STATUSES[listing.status]
  return (
    <span className={cn('inline-flex shrink-0 items-center gap-1 rounded-full px-3 py-1 text-xs font-semibold', className)}>
      {soon && <Hourglass className="size-3" aria-hidden />}{label}
    </span>
  )
}
