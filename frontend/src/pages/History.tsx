import { useMutation } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { usePageTitle } from '@/components/hooks'
import { StatusPill } from '@/components/StatusPill'
import { Button } from '@/components/ui/button'
import { download } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { useHistory } from '@/lib/listings'
import { CATEGORIES } from '@/lib/meta'
import { canGive, fmtDateTime } from '@/lib/utils'

export default function History() {
  usePageTitle('Pickup history')
  const { user } = useAuth()
  const { data, isLoading } = useHistory()
  const exportCsv = useMutation({ mutationFn: () => download('/listings/history.csv', 'plateforward-history.csv'), onError: (e) => toast.error(e.message) })
  const counterpart = canGive(user) ? 'Collected by' : 'Donor'

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-4xl font-extrabold sm:text-5xl">Pickup history</h1>
          <p className="mt-2 text-stone-500">{data ? `${data.length} completed or expired listing${data.length === 1 ? '' : 's'}` : 'Loading…'}</p>
        </div>
        <Button variant="secondary" onClick={() => exportCsv.mutate()} loading={exportCsv.isPending} disabled={!data?.length}><Download className="size-4" aria-hidden />Export CSV</Button>
      </div>

      <div className="surface mt-8 overflow-x-auto">
        {isLoading && <div className="h-40 animate-pulse" aria-busy />}
        {data && data.length > 0 && (
          <table className="w-full min-w-[40rem] text-left text-sm">
            <thead className="border-b border-stone-100 text-xs tracking-wide text-stone-500 uppercase">
              <tr>{['Food', 'Quantity', counterpart, 'Pickup', 'Status'].map((h) => <th key={h} className="px-5 py-4 font-semibold">{h}</th>)}</tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {data.map((l) => (
                <tr key={l.id} className="transition hover:bg-brand-50/50">
                  <td className="px-5 py-4 font-semibold text-stone-900"><Link to={`/listings/${l.id}`} className="hover:underline">{CATEGORIES[l.category].emoji} {l.title}</Link></td>
                  <td className="px-5 py-4">{l.quantity}</td>
                  <td className="px-5 py-4">{(canGive(user) ? l.claimer : l.donor)?.name ?? '—'}</td>
                  <td className="px-5 py-4 whitespace-nowrap">{fmtDateTime(l.pickupStart)}</td>
                  <td className="px-5 py-4"><StatusPill listing={l} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data?.length === 0 && <EmptyState emoji="📜" title="No completed pickups yet">Finished and expired listings will show up here.</EmptyState>}
      </div>
    </>
  )
}
