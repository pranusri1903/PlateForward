import { Activity, ArrowRight, BellRing, Hourglass, PackageCheck, ShieldAlert, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { ListingCard, ListingCardSkeleton } from '@/components/ListingCard'
import { buttonStyles } from '@/components/ui/button'
import { useAuth } from '@/lib/auth'
import { useMine } from '@/lib/listings'
import { ROLE_LABEL } from '@/lib/meta'
import { canGive } from '@/lib/utils'

const Stat = ({ icon: Icon, value, label }: { icon: LucideIcon; value: number; label: string }) => (
  <div className="surface p-6"><Icon className="size-5 text-brand-600" aria-hidden /><div className="mt-3 font-display text-4xl font-extrabold">{value}</div><div className="text-sm text-stone-500">{label}</div></div>
)

export default function Dashboard() {
  const { user } = useAuth()
  const { data, isLoading } = useMine()
  if (!user) return null
  const giving = canGive(user)
  const live = data?.filter((l) => ['LISTED', 'CLAIMED', 'CONFIRMED'].includes(l.status)) ?? []
  const past = data?.filter((l) => !live.includes(l)) ?? []
  const needsYou = live.filter((l) => l.actions.includes('confirm'))
  const waiting = live.filter((l) => l.status === 'CLAIMED').length

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-4xl font-extrabold sm:text-5xl">Hi, {user.name.split(' ')[0]} 👋</h1>
          <p className="mt-2 text-stone-500">{ROLE_LABEL[user.role]} · {user.area}</p>
        </div>
        <Link to={giving ? '/listings/new' : '/listings'} className={buttonStyles({ size: 'lg' })}>{giving ? 'Post surplus food' : 'Find food to collect'}<ArrowRight className="size-5" aria-hidden /></Link>
      </div>

      {user.role === 'ORG' && !user.verified && (
        <div className="mt-8 flex items-start gap-3 rounded-3xl bg-amber-50 p-5 text-amber-900"><ShieldAlert className="mt-0.5 size-5 shrink-0" aria-hidden /><p><b>Awaiting verification.</b> You can browse listings now. Claiming opens up as soon as our team verifies your organization.</p></div>
      )}

      <div className="mt-8 grid gap-4 sm:grid-cols-3">
        <Stat icon={Activity} value={live.length} label={giving ? 'Active listings' : 'Active claims'} />
        <Stat icon={Hourglass} value={giving ? needsYou.length : waiting} label={giving ? 'Waiting for your confirmation' : 'Waiting for donor to confirm'} />
        <Stat icon={PackageCheck} value={data?.filter((l) => l.status === 'PICKED_UP').length ?? 0} label="Completed pickups" />
      </div>

      {needsYou.length > 0 && (
        <div className="mt-8 flex items-center gap-3 rounded-3xl bg-amber-100 p-5 font-medium text-amber-900"><BellRing className="size-6 shrink-0" aria-hidden />{needsYou.length} claim{needsYou.length === 1 ? ' is' : 's are'} waiting for your confirmation. Open the highlighted listing{needsYou.length === 1 ? '' : 's'} below.</div>
      )}

      <h2 className="mt-12 text-2xl font-bold">{giving ? 'Your active listings' : 'Your upcoming pickups'}</h2>
      <div className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading && Array.from({ length: 3 }, (_, i) => <ListingCardSkeleton key={i} />)}
        {live.map((l, i) => <ListingCard key={l.id} listing={l} index={i} highlight={needsYou.includes(l)} />)}
        {data && !live.length && <EmptyState emoji={giving ? '📦' : '🧺'} title="Nothing active yet">{giving ? 'Post your first listing and organizations can claim it right away.' : 'Browse available food to make your first claim.'}</EmptyState>}
      </div>

      {past.length > 0 && (
        <>
          <h2 className="mt-12 text-2xl font-bold">Past</h2>
          <div className="mt-5 grid gap-5 opacity-80 sm:grid-cols-2 lg:grid-cols-3">{past.map((l, i) => <ListingCard key={l.id} listing={l} index={i} />)}</div>
        </>
      )}
    </>
  )
}
