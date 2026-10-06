import { Clock, MapPin } from 'lucide-react'
import { motion } from 'motion/react'
import { Link } from 'react-router'
import { CATEGORIES, STORAGES } from '@/lib/meta'
import type { Listing } from '@/lib/types'
import { cn, fmtDateTime } from '@/lib/utils'
import { Countdown } from './Countdown'
import { StatusPill } from './StatusPill'

export function ListingCard({ listing: l, highlight, index = 0 }: { listing: Listing; highlight?: boolean; index?: number }) {
  const cat = CATEGORIES[l.category]
  return (
    <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: Math.min(index, 8) * 0.04, duration: 0.35 }} whileHover={{ y: -4 }}>
      <Link to={`/listings/${l.id}`} className={cn('surface group flex h-full flex-col p-5 transition-shadow hover:shadow-lift', highlight && 'ring-2 ring-amber-400')}>
        <div className="flex items-start gap-4">
          <div className={cn('grid size-14 shrink-0 place-items-center rounded-2xl bg-gradient-to-br text-3xl transition-transform group-hover:scale-110 group-hover:-rotate-6', cat.tint)} aria-hidden>{cat.emoji}</div>
          <div className="min-w-0 flex-1">
            <h3 className="truncate text-lg font-bold">{l.title}</h3>
            <p className="mt-0.5 flex items-center gap-1 truncate text-sm text-stone-500"><MapPin className="size-3.5 shrink-0" aria-hidden />{l.donor.name} · {l.area}</p>
          </div>
        </div>
        <div className="my-4 flex flex-wrap items-center gap-2 text-xs font-medium">
          <StatusPill listing={l} />
          <span className="rounded-full bg-stone-100 px-3 py-1 text-stone-700">{l.quantity}</span>
          <span className="rounded-full bg-stone-100 px-3 py-1 text-stone-700">{STORAGES[l.storage].emoji} {STORAGES[l.storage].label}</span>
          {l.audience === 'ANYONE' && <span className="rounded-full bg-violet-100 px-3 py-1 text-violet-700">Open to everyone</span>}
        </div>
        <div className="mt-auto flex items-center justify-between gap-3 border-t border-stone-100 pt-4 text-sm">
          <span className="flex min-w-0 items-center gap-1.5 text-stone-500"><Clock className="size-4 shrink-0" aria-hidden /><span className="truncate">{fmtDateTime(l.pickupStart)}</span></span>
          {l.status === 'LISTED' && <Countdown to={l.expiresAt} />}
        </div>
      </Link>
    </motion.div>
  )
}

export const ListingCardSkeleton = () => (
  <div className="surface animate-pulse p-5" aria-hidden>
    <div className="flex gap-4"><div className="size-14 rounded-2xl bg-stone-100" /><div className="flex-1 space-y-2.5 pt-1"><div className="h-4 w-3/4 rounded bg-stone-100" /><div className="h-3 w-1/2 rounded bg-stone-100" /></div></div>
    <div className="mt-5 flex gap-2"><div className="h-6 w-20 rounded-full bg-stone-100" /><div className="h-6 w-16 rounded-full bg-stone-100" /></div>
    <div className="mt-5 h-4 w-2/3 rounded bg-stone-100" />
  </div>
)
