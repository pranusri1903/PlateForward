import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BadgeCheck, CircleCheck, Gavel } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { usePageTitle } from '@/components/hooks'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/field'
import { api } from '@/lib/api'
import { ROLE_LABEL } from '@/lib/meta'
import type { Dispute, User } from '@/lib/types'
import { fmtDateTime } from '@/lib/utils'

const KIND = { NO_SHOW: 'No-show', ISSUE: 'Issue' }

function DisputeCard({ d, onResolve, busy }: { d: Dispute; onResolve: (resolution: string) => void; busy: boolean }) {
  const [note, setNote] = useState('')
  return (
    <form className="surface p-6 ring-red-200" onSubmit={(e) => { e.preventDefault(); onResolve(note.trim()) }}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="rounded-full bg-red-100 px-3 py-1 text-xs font-semibold text-red-700">{KIND[d.kind]}</span>
        <Link to={`/listings/${d.listingId}`} className="text-sm font-semibold text-brand-700 hover:underline">{d.listingTitle} →</Link>
      </div>
      <p className="mt-3 text-sm"><b className="text-stone-900">{d.reporter.name}</b> <span className="text-stone-500">({ROLE_LABEL[d.reporter.role]})</span> reported <b className="text-stone-900">{d.against?.name}</b> · <span className="text-stone-500">{fmtDateTime(d.createdAt)}</span></p>
      <p className="mt-2 rounded-2xl bg-stone-50 p-4 text-sm text-stone-700">“{d.text}”</p>
      <div className="mt-4 flex flex-col gap-3 sm:flex-row">
        <Input aria-label="Resolution note" value={note} onChange={(e) => setNote(e.target.value)} maxLength={2000} placeholder="Resolution note (emailed to both parties)" />
        <Button loading={busy} disabled={!note.trim()} className="shrink-0"><Gavel className="size-4" aria-hidden />Resolve</Button>
      </div>
    </form>
  )
}

export default function Admin() {
  usePageTitle('Admin')
  const queryClient = useQueryClient()
  const orgs = useQuery({ queryKey: ['admin-orgs'], queryFn: () => api<User[]>('/admin/organizations'), refetchInterval: 15_000 })
  const disputes = useQuery({ queryKey: ['admin-disputes'], queryFn: () => api<{ open: Dispute[]; resolved: Dispute[] }>('/admin/disputes'), refetchInterval: 15_000 })
  const toggle = useMutation({
    mutationFn: (id: number) => api<User>(`/admin/organizations/${id}/verify`, { method: 'POST' }),
    onSuccess: (org) => { queryClient.invalidateQueries({ queryKey: ['admin-orgs'] }); toast.success(org.verified ? `${org.name} verified` : `${org.name} verification revoked`) },
    onError: (e) => toast.error(e.message),
  })
  const resolve = useMutation({
    mutationFn: ({ id, resolution }: { id: number; resolution: string }) => api(`/admin/disputes/${id}/resolve`, { method: 'POST', body: { resolution } }),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['admin-disputes'] }); toast.success('Resolved. Both parties were notified.') },
    onError: (e) => toast.error(e.message),
  })
  const open = disputes.data?.open ?? []
  const pending = orgs.data?.filter((o) => !o.verified).length ?? 0

  return (
    <>
      <h1 className="text-4xl font-extrabold sm:text-5xl">Admin console</h1>

      <h2 className="mt-10 flex items-center gap-3 text-2xl font-bold">Disputes {open.length > 0 && <span className="rounded-full bg-red-100 px-3 py-0.5 text-sm font-semibold text-red-700">{open.length} open</span>}</h2>
      <div className="mt-5 space-y-4">
        {open.map((d) => <DisputeCard key={d.id} d={d} busy={resolve.isPending && resolve.variables?.id === d.id} onResolve={(resolution) => resolve.mutate({ id: d.id, resolution })} />)}
        {disputes.data && !open.length && <div className="surface"><EmptyState emoji="🎉" title="No open disputes">Reports from donors and organizations will appear here.</EmptyState></div>}
      </div>
      {!!disputes.data?.resolved.length && (
        <details className="surface mt-4 p-5">
          <summary className="cursor-pointer font-semibold text-stone-700">Resolved ({disputes.data.resolved.length})</summary>
          <ul className="mt-3 divide-y divide-stone-100">
            {disputes.data.resolved.map((d) => (
              <li key={d.id} className="flex items-start gap-3 py-3 text-sm"><CircleCheck className="mt-0.5 size-4 shrink-0 text-brand-600" aria-hidden /><span><b className="text-stone-900">{d.listingTitle}</b> · {KIND[d.kind]} by {d.reporter.name}<br /><span className="text-stone-500">{d.resolution}</span></span></li>
            ))}
          </ul>
        </details>
      )}

      <h2 className="mt-12 flex items-center gap-3 text-2xl font-bold">Organization verification {pending > 0 && <span className="rounded-full bg-amber-100 px-3 py-0.5 text-sm font-semibold text-amber-800">{pending} pending</span>}</h2>
      <div className="surface mt-5 divide-y divide-stone-100">
        {orgs.isLoading && <div className="h-24 animate-pulse" aria-busy />}
        {orgs.data?.map((o) => (
          <div key={o.id} className="flex flex-wrap items-center justify-between gap-4 p-5">
            <div><div className="font-semibold text-stone-900">{o.name}</div><div className="text-sm text-stone-500">{o.email} · {o.area}{o.phone && ` · ${o.phone}`}</div></div>
            <Button variant={o.verified ? 'secondary' : 'primary'} size="sm" loading={toggle.isPending && toggle.variables === o.id} onClick={() => toggle.mutate(o.id)}>
              {o.verified ? <><BadgeCheck className="size-4 text-brand-600" aria-hidden />Verified · revoke</> : 'Verify'}
            </Button>
          </div>
        ))}
        {orgs.data?.length === 0 && <EmptyState emoji="🏛️" title="No organizations yet" />}
      </div>
    </>
  )
}
