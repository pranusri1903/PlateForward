import { Check, X } from 'lucide-react'
import { motion } from 'motion/react'
import type { Listing } from '@/lib/types'
import { cn } from '@/lib/utils'

const STEPS = ['Listed', 'Claimed', 'Confirmed', 'Picked up']
const INDEX = { LISTED: 0, CLAIMED: 1, CONFIRMED: 2, PICKED_UP: 3 } as const

export function Stepper({ listing: l }: { listing: Pick<Listing, 'status' | 'claimer'> }) {
  const expired = l.status === 'EXPIRED'
  const at = l.status === 'EXPIRED' ? (l.claimer ? 2 : 0) : INDEX[l.status] // an expired listing stopped at the last step it reached
  return (
    <ol className="flex items-start" aria-label="Pickup progress">
      {STEPS.map((label, i) => {
        const failed = expired && i === at + 1
        const done = i < at || l.status === 'PICKED_UP'
        const active = i <= at && !failed
        return (
          <li key={label} className={cn('flex items-start', i < STEPS.length - 1 && 'flex-1')} aria-current={i === at && !expired ? 'step' : undefined}>
            <div className="flex w-16 flex-col items-center gap-2">
              <motion.span
                initial={false}
                animate={{ scale: i === at && !expired ? [1, 1.12, 1] : 1 }}
                transition={{ duration: 0.5 }}
                className={cn('grid size-10 place-items-center rounded-full text-sm font-bold', failed ? 'bg-red-500 text-white' : active ? 'bg-brand-600 text-white shadow-lg shadow-brand-600/30' : 'bg-stone-100 text-stone-400')}
              >
                {failed ? <X className="size-5" aria-hidden /> : done ? <Check className="size-5" aria-hidden /> : i + 1}
              </motion.span>
              <span className={cn('text-center text-xs font-medium', active || failed ? 'text-stone-900' : 'text-stone-400')}>{failed ? 'Expired' : label}</span>
            </div>
            {i < STEPS.length - 1 && <div className="mt-[18px] mr-1 ml-1 h-1 flex-1 overflow-hidden rounded-full bg-stone-100"><motion.div className="h-full bg-brand-500" initial={false} animate={{ width: i < at ? '100%' : '0%' }} transition={{ duration: 0.5 }} /></div>}
          </li>
        )
      })}
    </ol>
  )
}
