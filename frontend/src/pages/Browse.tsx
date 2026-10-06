import { ChevronLeft, ChevronRight, Search, SearchX } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useDebounced, usePageTitle } from '@/components/hooks'
import { ListingCard, ListingCardSkeleton } from '@/components/ListingCard'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/ui/button'
import { Input, Select } from '@/components/ui/field'
import { useBrowse } from '@/lib/listings'
import { CATEGORIES, STORAGES } from '@/lib/meta'
import { cn } from '@/lib/utils'

const chip = (active: boolean) => cn('shrink-0 rounded-full px-4 py-2 text-sm font-medium whitespace-nowrap transition', active ? 'bg-stone-900 text-white shadow-md' : 'bg-white text-stone-700 ring-1 ring-stone-200 hover:bg-stone-100')

export default function Browse() {
  usePageTitle('Browse food')
  const [params, setParams] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const debouncedQ = useDebounced(q)
  const page = Math.max(Number(params.get('page')) || 1, 1)
  const category = params.get('category') ?? ''

  const set = (key: string, value: string) => setParams((p) => {
    if (value) p.set(key, value)
    else p.delete(key)
    if (key !== 'page') p.delete('page')
    return p
  }, { replace: true })

  useEffect(() => {
    setParams((p) => {
      const next = debouncedQ.trim()
      if ((p.get('q') ?? '') === next) return p
      if (next) p.set('q', next)
      else p.delete('q')
      p.delete('page')
      return p
    }, { replace: true })
  }, [debouncedQ, setParams])

  const query = new URLSearchParams(params)
  query.set('page', String(page - 1)) // the API counts pages from 0
  query.set('size', '12')
  const { data, isLoading, isError, refetch } = useBrowse(query)
  const total = data?.page.totalElements ?? 0
  const filtered = ['q', 'category', 'storage'].some((k) => params.get(k))

  return (
    <>
      <h1 className="text-4xl font-extrabold sm:text-5xl">Available food</h1>
      <p className="mt-2 text-stone-500" aria-live="polite">{data ? `${total} listing${total === 1 ? '' : 's'} ready to collect` : 'Finding food near you…'}</p>

      <div className="surface mt-8 space-y-4 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr_12rem_12rem]">
          <div className="relative">
            <Search className="absolute top-1/2 left-4 size-5 -translate-y-1/2 text-stone-400" aria-hidden />
            <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search food or neighborhood…" aria-label="Search" className="pl-12" />
          </div>
          <Select aria-label="Storage" value={params.get('storage') ?? ''} onChange={(e) => set('storage', e.target.value)}>
            <option value="">Any storage</option>
            {Object.entries(STORAGES).map(([k, s]) => <option key={k} value={k}>{s.emoji} {s.label}</option>)}
          </Select>
          <Select aria-label="Sort" value={params.get('sort') ?? 'expiring'} onChange={(e) => set('sort', e.target.value === 'expiring' ? '' : e.target.value)}>
            <option value="expiring">Expiring soonest</option>
            <option value="newest">Newest first</option>
          </Select>
        </div>
        <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1" role="group" aria-label="Category">
          <button className={chip(!category)} aria-pressed={!category} onClick={() => set('category', '')}>All</button>
          {Object.entries(CATEGORIES).map(([k, c]) => <button key={k} className={chip(category === k)} aria-pressed={category === k} onClick={() => set('category', k)}>{c.emoji} {c.label}</button>)}
        </div>
      </div>

      <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {isLoading && Array.from({ length: 6 }, (_, i) => <ListingCardSkeleton key={i} />)}
        {isError && <EmptyState emoji="📡" title="Couldn't load listings"><Button variant="secondary" className="mt-4" onClick={() => refetch()}>Try again</Button></EmptyState>}
        {data?.content.map((l, i) => <ListingCard key={l.id} listing={l} index={i} />)}
        {data && !total && (
          <EmptyState emoji={filtered ? '🔍' : '🍃'} title={filtered ? 'Nothing matches those filters' : 'No food listed right now'}>
            {filtered ? <Button variant="secondary" className="mt-4" onClick={() => { setQ(''); setParams({}, { replace: true }) }}><SearchX className="size-4" aria-hidden />Clear filters</Button> : 'New surplus is posted all the time. Check back soon.'}
          </EmptyState>
        )}
      </div>

      {data && data.page.totalPages > 1 && (
        <nav className="mt-10 flex items-center justify-center gap-4" aria-label="Pagination">
          <Button variant="secondary" size="sm" disabled={page <= 1} onClick={() => set('page', String(page - 1))}><ChevronLeft className="size-4" aria-hidden />Previous</Button>
          <span className="text-sm font-medium text-stone-600">Page {page} of {data.page.totalPages}</span>
          <Button variant="secondary" size="sm" disabled={page >= data.page.totalPages} onClick={() => set('page', String(page + 1))}>Next<ChevronRight className="size-4" aria-hidden /></Button>
        </nav>
      )}
    </>
  )
}
