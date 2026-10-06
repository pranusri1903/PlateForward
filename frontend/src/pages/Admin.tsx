import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BadgeCheck } from 'lucide-react'
import { toast } from 'sonner'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/ui/button'
import { api } from '@/lib/api'
import type { User } from '@/lib/types'
import { cn } from '@/lib/utils'

export default function Admin() {
  const queryClient = useQueryClient()
  const { data: orgs, isLoading } = useQuery({ queryKey: ['admin-orgs'], queryFn: () => api<User[]>('/admin/organizations') })
  const toggle = useMutation({
    mutationFn: (id: number) => api<User>(`/admin/organizations/${id}/verify`, { method: 'POST' }),
    onSuccess: (org) => { queryClient.invalidateQueries({ queryKey: ['admin-orgs'] }); toast.success(org.verified ? `${org.name} verified` : `${org.name} verification revoked`) },
    onError: (e) => toast.error(e.message),
  })

  return (
    <>
      <h1 className="text-4xl font-extrabold sm:text-5xl">Admin console</h1>
      <h2 className="mt-10 text-2xl font-bold">Organization verification</h2>
      <div className="surface mt-5 divide-y divide-stone-100">
        {isLoading && <div className="h-24 animate-pulse" aria-busy />}
        {orgs?.map((o) => (
          <div key={o.id} className="flex flex-wrap items-center justify-between gap-4 p-5">
            <div><div className="font-semibold text-stone-900">{o.name}</div><div className="text-sm text-stone-500">{o.email} · {o.area}{o.phone && ` · ${o.phone}`}</div></div>
            <Button variant={o.verified ? 'secondary' : 'primary'} size="sm" loading={toggle.isPending && toggle.variables === o.id} onClick={() => toggle.mutate(o.id)} className={cn(o.verified && 'text-stone-700')}>
              {o.verified ? <><BadgeCheck className="size-4 text-brand-600" aria-hidden />Verified · revoke</> : 'Verify'}
            </Button>
          </div>
        ))}
        {orgs?.length === 0 && <EmptyState emoji="🏛️" title="No organizations yet" />}
      </div>
    </>
  )
}
