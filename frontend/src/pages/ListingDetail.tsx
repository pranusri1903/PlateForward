import { ArrowLeft, BadgeCheck, HandHelping, MapPin, Package, PackageCheck, Phone, Thermometer, Timer, Trash2, Undo2, X, type LucideIcon } from 'lucide-react'
import { format } from 'date-fns'
import type { ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ConfirmButton } from '@/components/ConfirmButton'
import { Countdown } from '@/components/Countdown'
import { EmptyState } from '@/components/EmptyState'
import { StatusPill } from '@/components/StatusPill'
import { Stepper } from '@/components/Stepper'
import { Button, buttonStyles } from '@/components/ui/button'
import { ApiError } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { useListing, useListingAction } from '@/lib/listings'
import { CATEGORIES, STORAGES } from '@/lib/meta'
import type { Action, Listing, Status } from '@/lib/types'
import { cn, fmtDateTime, fmtWindow } from '@/lib/utils'

const ACTIONS: Record<Action, { label: (s: Status) => string; icon: LucideIcon; danger?: boolean }> = {
  claim: { label: () => 'Claim this food', icon: HandHelping },
  confirm: { label: () => 'Confirm claim', icon: BadgeCheck },
  pickup: { label: () => 'Mark as picked up', icon: PackageCheck },
  decline: { label: (s) => (s === 'CONFIRMED' ? 'Cancel this claim' : 'Decline claim'), icon: X, danger: true },
  release: { label: (s) => (s === 'CONFIRMED' ? "I can't make it" : 'Release claim'), icon: Undo2, danger: true },
  delete: { label: () => 'Remove listing', icon: Trash2, danger: true },
}

const tones = { amber: 'bg-amber-50 text-amber-900', sky: 'bg-sky-50 text-sky-900', brand: 'bg-brand-50 text-brand-900', stone: 'bg-stone-100 text-stone-700', red: 'bg-red-50 text-red-800' }
const Notice = ({ tone, children }: { tone: keyof typeof tones; children: ReactNode }) => <div className={cn('rounded-2xl p-4 text-sm', tones[tone])}>{children}</div>

export default function ListingDetail() {
  const { id } = useParams()
  const { data: l, isLoading, error } = useListing(id)
  if (isLoading) return <div className="surface h-96 animate-pulse" aria-busy />
  if (!l) return <EmptyState emoji="🫥" title={error instanceof ApiError && error.status === 404 ? 'This listing no longer exists' : "Couldn't load this listing"}><Link to="/listings" className={cn(buttonStyles({ variant: 'secondary' }), 'mt-4')}>Browse food</Link></EmptyState>
  return <Detail l={l} />
}

function Detail({ l }: { l: Listing }) {
  const navigate = useNavigate()
  const { user } = useAuth()
  const action = useListingAction(l.id, () => navigate('/dashboard'))
  const cat = CATEGORIES[l.category]
  const facts = [
    { icon: Package, label: 'Quantity', value: l.quantity },
    { icon: Thermometer, label: 'Storage', value: `${STORAGES[l.storage].emoji} ${STORAGES[l.storage].label}` },
    { icon: Timer, label: 'Pickup window', value: fmtWindow(l) },
    { icon: Timer, label: 'Food expires', value: fmtDateTime(l.expiresAt) },
  ]
  const isDonor = user?.id === l.donor.id

  return (
    <>
      <Link to="/listings" className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-stone-500 transition hover:text-stone-900"><ArrowLeft className="size-4" aria-hidden />All listings</Link>
      <div className="grid gap-8 lg:grid-cols-[1fr_22rem]">
        <div className="space-y-6">
          <section className="surface p-6 sm:p-8">
            <div className="flex items-start gap-5">
              <div className={cn('grid size-20 shrink-0 place-items-center rounded-3xl bg-gradient-to-br text-5xl', cat.tint)} aria-hidden>{cat.emoji}</div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <StatusPill listing={l} />
                  {l.audience === 'ANYONE' && <span className="rounded-full bg-violet-100 px-3 py-1 text-xs font-semibold text-violet-700">Open to everyone</span>}
                </div>
                <h1 className="mt-2 text-3xl font-extrabold sm:text-4xl">{l.title}</h1>
                <p className="mt-1 text-stone-500">Shared by <b className="font-semibold text-stone-700">{l.donor.name}</b> · {l.area}</p>
              </div>
            </div>
            {l.description && <p className="mt-6 whitespace-pre-line text-stone-600">{l.description}</p>}
            <dl className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
              {facts.map(({ icon: Icon, label, value }) => (
                <div key={label} className="rounded-2xl bg-stone-50 p-4"><Icon className="size-5 text-brand-600" aria-hidden /><dt className="mt-2 text-xs text-stone-500">{label}</dt><dd className="mt-0.5 text-sm font-semibold text-stone-900">{value}</dd></div>
              ))}
            </dl>
          </section>
          <section className="surface p-6 sm:p-8">
            <h2 className="mb-6 text-lg font-bold">Pickup progress</h2>
            <Stepper listing={l} />
            {l.status === 'EXPIRED' && <div className="mt-6"><Notice tone="red">This listing expired{l.claimer ? ' before it was collected.' : ' without a claim.'}</Notice></div>}
          </section>
        </div>

        <aside className="lg:sticky lg:top-24 lg:self-start">
          <div className="surface space-y-4 p-6 shadow-lift">
            {l.status === 'LISTED' && <div className="flex items-center justify-between text-sm"><span className="text-stone-500">Time remaining</span><Countdown to={l.expiresAt} /></div>}
            <Situation l={l} isDonor={isDonor} />
            {!user && l.status === 'LISTED' && <Link to="/login" state={{ from: `/listings/${l.id}` }} className={cn(buttonStyles(), 'w-full')}>Log in to claim</Link>}
            {l.actions.map((a) => {
              const { label, icon: Icon, danger } = ACTIONS[a]
              const Btn = danger ? ConfirmButton : Button
              return <Btn key={a} variant={danger ? 'danger' : 'primary'} size="lg" className="w-full" loading={action.isPending && action.variables === a} disabled={action.isPending} onClick={() => action.mutate(a)}><Icon className="size-5" aria-hidden />{label(l.status)}</Btn>
            })}
          </div>
        </aside>
      </div>
    </>
  )
}

/** Explains the current state from the viewer's point of view. */
function Situation({ l, isDonor }: { l: Listing; isDonor: boolean }) {
  const { user } = useAuth()
  const claimer = user?.id === l.claimer?.id
  const hold = l.claimExpiresAt && <b>{format(new Date(l.claimExpiresAt), 'h:mm a')}</b>
  const contact = (who: string, p: { name: string; phone: string | null }) => <>{who} <b>{p.name}</b>{p.phone && <a href={`tel:${p.phone}`} className="mt-1 flex items-center gap-1.5 font-semibold underline"><Phone className="size-3.5" aria-hidden />{p.phone}</a>}</>

  if (l.status === 'LISTED') {
    if (isDonor) return <Notice tone="stone">Waiting for a claim. You can remove this listing until someone claims it.</Notice>
    if (user?.role === 'ORG' && !user.verified) return <Notice tone="amber">Your organization is awaiting verification. You'll be able to claim food as soon as our team approves it.</Notice>
    if (user && !l.actions.length) return <Notice tone="stone">{user.role === 'ADMIN' || user.role === 'DONOR' || user.role === 'GIVER' ? 'Only organizations and individuals looking for food can claim listings.' : 'This listing is reserved for verified organizations.'}</Notice>
    return user ? <p className="text-center text-xs text-stone-500">The donor has 2 hours to confirm. If they don't, it goes back on the board.</p> : null
  }
  if (l.status === 'CLAIMED') return isDonor
    ? <Notice tone="amber">{contact('Claimed by', l.claimer!)}<p className="mt-2">Please confirm by {hold}, or it returns to the board.</p></Notice>
    : claimer ? <Notice tone="amber">Waiting for the donor to confirm (by {hold}). We'll email you as soon as they do.</Notice> : <Notice tone="stone">Someone has claimed this listing. If it isn't confirmed in time it will return to the board.</Notice>
  if (l.status === 'CONFIRMED') return isDonor
    ? <Notice tone="sky">{contact('Collecting:', l.claimer!)}</Notice>
    : claimer ? <Notice tone="brand"><b className="flex items-center gap-1.5"><MapPin className="size-4" aria-hidden />Pickup address</b><p className="mt-1 text-base font-semibold">{l.address}</p><p className="mt-1 text-xs">{fmtWindow(l)}</p><div className="mt-3 border-t border-brand-200 pt-3">{contact('Contact', l.donor)}</div></Notice>
      : <Notice tone="stone">This pickup is confirmed for another claimer.</Notice>
  if (l.status === 'PICKED_UP') return <Notice tone="stone">✅ Collected {l.pickedUpAt && fmtDateTime(l.pickedUpAt)}</Notice>
  return <Notice tone="red">This listing is no longer available.</Notice>
}
